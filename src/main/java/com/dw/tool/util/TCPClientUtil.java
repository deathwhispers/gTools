package com.dw.tool.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2023/11/3 14:41
 */
@Slf4j
public class TCPClientUtil {

    private static final ConnectionPool connectionPool = new ConnectionPool(6, 3000);
    private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();


    private static final int maxRetry = 3;
    private static final int readTimeOut = 3000;
    // 重试间隔 单位 ms
    private static final long retryInterval = 1000;

    /**
     * 发送指令到TCP服务端
     *
     * @param serverIP   服务端IP地址
     * @param serverPort 服务端端口号
     * @param command    要发送的指令
     * @throws IOException 发送异常
     */
    public static synchronized String SendAndReceive645Data(String serverIP, int serverPort, String command) throws IOException {
        return trySendAndReceive(serverIP, serverPort, command, 0, (msg) -> {
            String trimMsg = StringUtils.replaceIgnoreCase(msg, "FEFEFEFE", "");
            String len_s = trimMsg.substring(9 * 2, 9 * 2 + 2);
            int dataLen = Integer.parseInt(len_s, 16);
            return (dataLen + 12) * 2;
        });
    }


    public static String modbusSendAndReceiveData(String serverIP, int serverPort, String command) throws IOException {
        return trySendAndReceive(serverIP, serverPort, command, 0, (msg) -> {
            int functionCode = Integer.parseInt(msg.substring(2, 4));
            switch (functionCode) {
                case FunctionCode.READ_HOLDING_REGISTERS:
                    int dataLen = Integer.parseInt(msg.substring(4, 6), 16);
                    return (dataLen + 5) * 2;
                case FunctionCode.WRITE_REGISTER:
                    return command.length();
                default:
                    return msg.length();
            }
        });
    }

    private static String trySendAndReceive(String serverIP, int serverPort, String command, int retryCount, Function<String, Integer> calculateMsgLength) throws IOException {
        try {
            return sendCommandAndReceive(serverIP, serverPort, command, calculateMsgLength);
        } catch (IOException e) {
            log.error("发送指令失败: ", e);
            if (++retryCount <= maxRetry) {
                log.debug("{}ms后，重试第{}次", retryInterval, retryCount);
                LockSupport.parkNanos(retryInterval * 2 * 1000_000);
                return trySendAndReceive(serverIP, serverPort, command, retryCount, calculateMsgLength);
            } else {
                throw new IOException("Exceeded maximum retry count", e);
            }
        }
    }

    private static String sendCommandAndReceive(String serverIP, int serverPort, String command, Function<String, Integer> calculateMsgLength) throws IOException {
        Socket socket;
        String key = serverIP + ":" + serverPort;
        Object lock;
        synchronized (lockMap) {
            lock = lockMap.computeIfAbsent(key, k -> new Object());
            socket = connectionPool.getConnection(serverIP, serverPort);
        }
        try {
            synchronized (lock) {
                try {
                    sendCommand(socket, command);
                } catch (SocketException se) {
                    connectionPool.close(serverIP, serverPort);
                    socket = connectionPool.getConnection(serverIP, serverPort);
                    sendCommand(socket, command);
                }
                return receiveResponse(socket, calculateMsgLength);
            }
        } finally {
            connectionPool.release(serverIP, serverPort);
        }
    }


    /**
     * 发送数据到TCP服务端
     *
     * @param socket  要发送数据的Socket对象
     * @param command 要发送的指令
     * @throws IOException 发送异常
     */
    private static void sendCommand(Socket socket, String command) throws IOException {
        OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
        if (socket.isOutputShutdown()) {
            throw new SocketException("output stream is shut down,can't send command");
        }
        outputStream.write(HexUtils.hexStringToBytes(command));
        outputStream.flush();
        log.debug("已发送指令到 {} >> command:{}", socket, command);
    }

    /**
     * 接收TCP服务端返回的数据
     *
     * @param socket TCP客户端Socket对象
     * @return 返回服务端返回的数据的十六进制字符串
     * @throws IOException 接收异常
     */
    private static String receiveResponse(Socket socket, Function<String, Integer> calculateMsgLength) throws IOException {
        InputStream inputStream = new BufferedInputStream(socket.getInputStream());
        StringBuilder responseBuilder = new StringBuilder();

        byte[] buffer = new byte[1024];
        int len;
        int totalLen = 0;
        String receiveMsg = "";

        // 设置超时时间以避免永久阻塞
        long timeoutMillis = System.currentTimeMillis() + readTimeOut;

        while (totalLen < 4 && System.currentTimeMillis() < timeoutMillis) {
            len = inputStream.read(buffer);
            if (len != -1) {
                responseBuilder.append(HexUtils.bytesToHexString(buffer, 0, len));
                totalLen += len;
            }
        }

        String msg = responseBuilder.toString();
        log.debug("读取到数据 >> receive: {}", msg);
        // 预处理，去掉前导字节
        msg = StringUtils.replaceIgnoreCase(msg, "FEFEFEFE", "");
        log.debug("预处理后数据 >> receive: {}", msg);

        int msgLen = calculateMsgLength.apply(msg);

        boolean isCompleteMessageReceived = false;
        if (msg.length() >= msgLen) {
            receiveMsg = msg.substring(0, msgLen);
            isCompleteMessageReceived = true;
        }

        if (!isCompleteMessageReceived) {
            while (System.currentTimeMillis() < timeoutMillis) {
                len = inputStream.read(buffer);
                if (len != -1) {
                    responseBuilder.append(HexUtils.bytesToHexString(buffer, 0, len));
                    msg = responseBuilder.toString();
                    if (msg.length() >= msgLen) {
                        receiveMsg = msg.substring(0, msgLen);
                        break;
                    }
                }
            }
        }
        log.debug("收到服务端返回的数据：{}", receiveMsg);
        return receiveMsg;
    }

    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    String command = "01060028";
                    String s2 = modbusSendAndReceiveData("127.0.0.1", 6000, command);
                    if (!command.equalsIgnoreCase(s2)) {
                        System.out.println("thread1 - 心跳响应不正确 ========== 发送：" + command + "，接收：" + s2);
                    }
                    TimeUnit.SECONDS.sleep(3);
                }
            } catch (Exception e) {
                log.debug("=========== main exception ==========");
            }
        }, "thread1").start();
    }
}
