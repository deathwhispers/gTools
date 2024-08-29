package com.dw.tool.util;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * TCP 连接池
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2023/11/3 21:11
 */
@Slf4j
public class ConnectionPool {

    /**
     * 连接列表
     */
    private final Map<String, PooledSocket> connections = new ConcurrentHashMap<>();

    /**
     * 最大连接数
     */
    private final int maxConnections;

    /**
     * 连接空闲超时时间（单位：毫秒）
     */
    private final long timeout;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 构造函数
     *
     * @param maxConnections 最大连接数
     * @param timeout        连接空闲超时时间（单位：毫秒）
     */
    public ConnectionPool(int maxConnections, long timeout) {
        this.maxConnections = maxConnections;
        this.timeout = timeout;

        // 定时清理空闲连接
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(this::cleanIdleConnections, timeout, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取TCP客户端连接
     *
     * @param serverIP   服务端IP地址
     * @param serverPort 服务端端口号
     * @return 返回Socket对象
     * @throws IOException 连接异常
     */
    public Socket getConnection(String serverIP, int serverPort) throws IOException {
        String key = getKey(serverIP, serverPort);
        lock.writeLock().lock();
        try {
            PooledSocket pooledSocket = connections.get(key);
            // 连接不可用时，新建连接
            if (pooledSocket == null) {
                pooledSocket = createConnection(serverIP, serverPort, key);
                connections.put(key, pooledSocket);
            } else {
                if (pooledSocket.isInvalid() || pooledSocket.isIdle()) {
                    pooledSocket.close();
                    pooledSocket = createConnection(serverIP, serverPort, key);
                    connections.put(key, pooledSocket);
                }
            }
            int retryCount = 0;
            int retryInterval = 200;
            int maxRetryCount = (int) timeout / retryInterval;
            synchronized (pooledSocket) {
                while (pooledSocket.isInUse() && retryCount < maxRetryCount) {
                    try {
                        // 等待连接可用
                        pooledSocket.wait(retryInterval);
                        retryCount++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        // 处理线程中断异常
                    }
                }
            }
            if (pooledSocket.isInUse()) {
                throw new IOException("无法获取连接：超过最大重试次数");
            }
            log.debug("获取连接 >> ip {} 端口 {}", serverIP, serverPort);
            pooledSocket.use();
            return pooledSocket.socket;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void release(String serverIP, int serverPort) {
        String key = getKey(serverIP, serverPort);
        lock.writeLock().lock();
        try {
            PooledSocket pooledSocket = connections.get(key);
            if (pooledSocket != null) {
                pooledSocket.release();
                // 连接不可用则清除
                if (pooledSocket.isInvalid()) {
                    pooledSocket.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void close(String serverIP, int serverPort) {
        String key = getKey(serverIP, serverPort);
        lock.writeLock().lock();
        try {
            PooledSocket pooledSocket = connections.get(key);
            if (pooledSocket != null) {
                // 连接不可用则清除
                pooledSocket.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 创建TCP客户端连接
     *
     * @param serverIP   服务端IP地址
     * @param serverPort 服务端端口号
     * @return 返回Socket对象
     * @throws IOException 连接异常
     */
    private PooledSocket createConnection(String serverIP, int serverPort, String key) throws IOException {
        log.debug("创建新连接 >> ip {} 端口 {}", serverIP, serverPort);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(serverIP, serverPort));
            socket.setKeepAlive(true);
            socket.setSoTimeout((int) timeout);
            return new PooledSocket(socket);
        } catch (SocketTimeoutException e1) {
            socket.connect(new InetSocketAddress(serverIP, serverPort));
            socket.setKeepAlive(true);
            socket.setSoTimeout((int) timeout);
            return new PooledSocket(socket);
        } catch (IOException e) {
            log.error("创建连接异常", e);
            if (socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    log.error("关闭连接异常", ex);
                }
            }
            throw e;
        }
    }

    /**
     * 清理不可用连接
     */
    private void cleanIdleConnections() {
        lock.writeLock().lock();
        try {
            connections.entrySet().removeIf(entry -> {
                PooledSocket pooledSocket = entry.getValue();
                if (pooledSocket.isInvalid() || pooledSocket.isIdle()) {
                    log.info("清理不可用连接 >> ip {} 端口 {}", pooledSocket.getIp(), pooledSocket.getPort());
                    pooledSocket.close();
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 生成连接列表中的key
     */
    private String getKey(String serverIP, int serverPort) {
        return serverIP + ":" + serverPort;
    }

    /**
     * 连接信息类
     */
    private class PooledSocket {
        private final Socket socket; // 连接
        private LocalDateTime lastUseTime; // 最后一次使用时间
        private Boolean inUse; // 是否在用

        public PooledSocket(Socket socket) {
            this.socket = socket;
            this.inUse = false;
        }

        public String getIp() {
            return this.socket.getInetAddress().getHostAddress();
        }

        public int getPort() {
            return this.socket.getPort();
        }

        public boolean isInUse() {
            return inUse;
        }

        /**
         * 更新最后一次使用时间
         */
        public void use() {
            this.lastUseTime = LocalDateTime.now();
            this.inUse = true;
        }

        public void release() {
            this.lastUseTime = LocalDateTime.now();
            this.inUse = false;
        }

        /**
         * 判断连接是否空闲
         *
         * @param now 当前时间
         * @return 如果连接空闲超过超时时间，则返回true；否则返回false
         */
        public synchronized boolean isIdle(LocalDateTime now) {
            return lastUseTime.plus(timeout * 10, ChronoUnit.MILLIS).isBefore(now);
        }

        /**
         * 判断连接是否空闲
         *
         * @return 如果连接空闲超过超时时间，则返回true；否则返回false
         */
        public synchronized boolean isIdle() {
            return isIdle(LocalDateTime.now());
        }

        /**
         * 连接无效
         *
         * @return true 连接无效， false 连接有效
         */
        public boolean isInvalid() {
            return socket.isClosed()
                    || !socket.isConnected()
                    || socket.isInputShutdown()
                    || socket.isOutputShutdown();
        }

        public void close() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("关闭连接异常", e);
                }
            }
        }
    }
}
