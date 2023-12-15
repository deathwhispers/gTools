package com.guangjian.gtool.util;

import cn.hutool.json.JSONUtil;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.*;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class ModbusUtil {

    private ModbusUtil() {
    }

    /**
     * @param ip   目的ip
     * @param port 目的port
     * @return /
     */
    private ModbusMaster getMaster(String ip, int port) {
        return getMaster(ip, port, true);
    }

    /**
     * @param ip      目的ip
     * @param port    目的port
     * @param rtuMode 报文格式  true是rtu报文格式 （默认） false是tcp报文格式
     * @return /
     */
    private ModbusMaster getMaster(String ip, int port, Boolean rtuMode) {
        if (rtuMode == null) {
            rtuMode = true;
        }
        IpParameters params = new IpParameters();
        params.setHost(ip);
        params.setPort(port);
        params.setEncapsulated(false);
        ModbusFactory modbusFactory = new ModbusFactory();
        ModbusMaster master = modbusFactory.createTcpMaster(params, false);
        try {
            master.init();
        } catch (ModbusInitException e) {
            log.error("init modbusMaster failed!", e);
            throw new RuntimeException("nit modbusMaster failed");
        }
        return master;
    }

    public static short[] readHoldingRegisters(String ip, int port, int slaveId, int start, int len, Boolean rtuMode) throws ModbusTransportException {
        ModbusMaster master = new ModbusUtil().getMaster(ip, port, rtuMode);
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, start, len);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
        master.destroy();
        if (response.isException()) {
            log.error("modbus读取失败！:req is->{},rsp err->{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        return response.getShortData();
    }

    public static byte[] readHoldingRegistersAll(String ip, int port, int slaveId, int funCode, int start, int len, Boolean rtuMode) throws ModbusTransportException {
        ModbusMaster master = new ModbusUtil().getMaster(ip, port, rtuMode);

        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, start, len);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
        master.destroy();
        if (response.isException()) {
            log.error("modbus读取失败！:req is->{},rsp err->{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        return response.getData();
    }

    public static boolean writeRegister(String ip, int port, int slaveId, int offset, short[] sdata, Boolean rtuMode) throws ModbusTransportException {
        ModbusMaster master = new ModbusUtil().getMaster(ip, port, rtuMode);
        WriteRegistersRequest request = new WriteRegistersRequest(slaveId, offset, sdata);
        WriteRegistersResponse response = (WriteRegistersResponse) master.send(request);
        master.destroy();
        if (response.isException()) {
            log.error("modbus控制失败！:req is->{},rsp err->{}", request, response.getExceptionMessage());
            return false;
        }
        return true;
    }

    //多读
    public static byte[] read(String ip, int port, int slaveId, int funCode, int start, int len, Boolean rtuMode) throws Exception {

        ModbusMaster master = null;
        try {
            master = new ModbusUtil().getMaster(ip, port, rtuMode);
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            head.write(slaveId);
            head.write(funCode);
            head.flush();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(head.toByteArray());
            body.write(ByteArrHelper.getInstance().int2twobytes(start));
            body.write(ByteArrHelper.getInstance().int2twobytes(len));
            body.flush();
            ModbusRequest request = ModbusRequest.createModbusRequest(new ByteQueue(body.toByteArray()));
            ReadResponse response = (ReadResponse) master.send(request);

            if (response.isException()) {
                throw new RuntimeException(String.format("modbus读取失败！:req is->%s,rsp err->%s", JSONUtil.toJsonStr(request), response.getExceptionMessage()));
            }
            head.write(response.getData().length);
            head.write(response.getData());
            return head.toByteArray();
        } finally {
            if (master != null) {
                master.destroy();
            }
        }
    }

    //单独写
    public static void write(String ip, int port, int slaveId, int funCode, int offset, Integer val, Boolean rtuMode) throws Exception {
        ModbusMaster master = null;
        try {
            master = new ModbusUtil().getMaster(ip, port, rtuMode);
            doWrite(master, slaveId, funCode, offset, val);
        } finally {
            if (master != null) {
                master.destroy();
            }
        }
    }

    /**
     * 多次单位置写入
     */
    public static void batchWrite(String ip, int port, int slaveId, int funCode, List<BatchWriteInfo> batchWriteInfoList, Boolean rtuMode) throws Exception {
        ModbusMaster master = null;
        try {
            master = new ModbusUtil().getMaster(ip, port, rtuMode);
            for (BatchWriteInfo batchWriteInfo : batchWriteInfoList) {
                int offset = batchWriteInfo.offset;
                Integer val = batchWriteInfo.getVal();
                doWrite(master, slaveId, funCode, offset, val);
            }
        } finally {
            if (master != null) {
                master.destroy();
            }
        }
    }

    private static void doWrite(ModbusMaster master, int slaveId, int funCode, int offset, Integer val) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(slaveId);
        outputStream.write(funCode);
        outputStream.write(ByteArrHelper.getInstance().int2twobytes(offset));
        if (funCode != FunctionCode.WRITE_COIL) {
            outputStream.write(ByteArrHelper.getInstance().int2twobytes(val));
        } else {
            //写线圈
            outputStream.write(val);
        }
        outputStream.flush();
        ModbusRequest request = ModbusRequest.createModbusRequest(new ByteQueue(outputStream.toByteArray()));
        ModbusResponse response = master.send(request);
        if (response.isException()) {
            throw new RuntimeException(String.format("modbus控制失败！:req is->%s,rsp err->%s", JSONUtil.toJsonStr(request), response.getExceptionMessage()));
        }
    }

    @Data
    public static class BatchWriteInfo {
        private int offset;
        private Integer val;
    }

    public static void main(String[] args) throws Exception {
        String str = "6801000000000068110433333635";
        String i = calculateChecksum(str);
        System.out.println(i);
    }

    public static String calculateChecksum(byte[] data) {
        int checksum = 0;
        for (int i = 0; i < data.length; i++) {
            checksum += data[i] & 0xFF;
            checksum %= 256;
        }
        return Integer.toHexString(checksum);
    }

    public static String calculateChecksum(String hexString) {
        if (!isHexString(hexString)) {
            throw new IllegalArgumentException("Input is not a hexadecimal string");
        }
        // 去除字符串中的空格
        hexString = hexString.replaceAll("\\s", "");

        // 将字符串转换为字节数组
        byte[] bytes = hexStringToBytes(hexString);

/*        if (bytes.length < 3) {
            throw new IllegalArgumentException("Input is too short to contain a valid frame");
        }*/
        return calculateChecksum(bytes);
    }

    private static final String HEX_PATTERN = "^[0-9a-fA-F]+$";

    private static boolean isHexString(String str) {
        return Pattern.matches(HEX_PATTERN, str);
    }

    private static byte[] hexStringToBytes(String hexString) {
        int length = hexString.length();
        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

}
