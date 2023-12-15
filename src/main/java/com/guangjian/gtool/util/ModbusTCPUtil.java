package com.guangjian.gtool.util;

import cn.hutool.json.JSONUtil;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.*;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2023/8/17 9:23
 */
@Slf4j
public class ModbusTCPUtil {

    private static final ModbusFactory factory = new ModbusFactory();

    public static ModbusMaster createMaster(String ip, int port) {
        IpParameters params = new IpParameters();
        params.setHost(ip);
        params.setPort(port);

        ModbusMaster master = factory.createTcpMaster(params, false);

        try {
            master.init();
            log.debug("初始化连接 >> ip:{}, port:{}", ip, port);
            return master;
        } catch (ModbusInitException e) {
            log.error("init modbusMaster failed!", e);
            throw new RuntimeException("nit modbusMaster failed");
        }
    }

    /**
     * 读取线圈（功能码01）
     */
    public static boolean[] readCoils(ModbusMaster master, int slaveId, int startOffset, int numberOfBits) throws Exception {
        ReadCoilsRequest request = new ReadCoilsRequest(slaveId, startOffset, numberOfBits);
        ReadCoilsResponse response = (ReadCoilsResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus读取失败! >> req:{}, err:{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        return response.getBooleanData();
    }

    /**
     * 写入线圈（功能码05）
     */
    public static void writeCoil(ModbusMaster master, int slaveId, int offset, boolean value) throws Exception {
        WriteCoilRequest request = new WriteCoilRequest(slaveId, offset, value);
        WriteCoilResponse response = (WriteCoilResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus控制失败！>> eq:{}, err:{}", request, response.getExceptionMessage());
        }
    }

    /**
     * 写入多个线圈（功能码0F）
     */
    public static void writeCoils(ModbusMaster master, int slaveId, int startOffset, boolean[] data) throws Exception {
        WriteCoilsRequest request = new WriteCoilsRequest(slaveId, startOffset, data);
        WriteCoilsResponse response = (WriteCoilsResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus控制失败！>> eq:{}, err:{}", request, response.getExceptionMessage());
        }
    }

    /**
     * 读取离散输入（功能码02）
     */
    public static boolean[] readDiscreteInputs(ModbusMaster master, int slaveId, int startOffset, int numberOfBits) throws Exception {
        ReadDiscreteInputsRequest request = new ReadDiscreteInputsRequest(slaveId, startOffset, numberOfBits);
        ReadDiscreteInputsResponse response = (ReadDiscreteInputsResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus读取失败! >> req:{}, err:{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        return response.getBooleanData();
    }

    /**
     * 读取保持寄存器（功能码03）
     */
    public static Integer readHoldingRegister(ModbusMaster master, int slaveId, int startOffset) throws Exception {
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startOffset, 1);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus读取失败! >> req:{}, err:{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        short[] shortData = response.getShortData();
        if (shortData.length == 1) {
            return (int) shortData[0];
        }
        return 0;
    }

    /**
     * 读取保持寄存器（功能码03）
     */
    public static short[] readHoldingRegisters(ModbusMaster master, int slaveId, int startOffset, int numberOfRegisters) throws Exception {
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startOffset, numberOfRegisters);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus读取失败! >> req:{}, err:{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        return response.getShortData();
    }

    /**
     * 写入单个保持寄存器（功能码06）
     */
    public static boolean writeHoldingRegister(ModbusMaster master, int slaveId, int startOffset, int data) throws Exception {
        WriteRegisterRequest request = new WriteRegisterRequest(slaveId, startOffset, data);
        WriteRegisterResponse response = (WriteRegisterResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus控制失败！>> eq:{}, err:{}", request, response.getExceptionMessage());
            return false;
        } else {
            return true;
        }
    }

    /**
     * 写入多个保持寄存器（功能码16）
     */
    public static boolean writeHoldingRegisters(ModbusMaster master, int slaveId, int startOffset, short[] data) throws Exception {
        WriteRegistersRequest request = new WriteRegistersRequest(slaveId, startOffset, data);
        WriteRegistersResponse response = (WriteRegistersResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus控制失败！>> eq:{}, err:{}", request, response.getExceptionMessage());
            return false;
        } else {
            return true;
        }
    }

    /**
     * 读取输入寄存器（功能码04）
     */
    public static short[] readInputRegisters(ModbusMaster master, int slaveId, int startOffset, int len) throws Exception {
        ReadInputRegistersRequest request = new ReadInputRegistersRequest(slaveId, startOffset, len);
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) master.send(request);
        if (response.isException()) {
            log.error("modbus读取失败! >> req:{}, err:{}", request, response.getExceptionMessage());
            throw new BadRequestException("获取数据失败");
        }
        return response.getShortData();
    }

    public static void doWrite(ModbusMaster master, int slaveId, int funCode, int offset, Integer val) throws Exception {
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

    public static void main(String[] args) throws Exception {

        ModbusMaster master = ModbusTCPUtil.createMaster("192.168.16.253", 51001);
        // 读寄存器
        short[] shorts = readHoldingRegisters(master, 1, 52, 6);

        System.out.println(Arrays.toString(shorts));

        // 写单个寄存器
//        boolean b = writeHoldingRegister(master, 1, 57, 650);
        // 写多个保持寄存器
        short[] data = new short[6];
        Arrays.fill(data, (short) 680);
        boolean b = writeHoldingRegisters(master, 1, 52, data);

        System.out.println(Arrays.toString(readHoldingRegisters(master, 1, 52, 6)));

        System.out.println(Arrays.toString(readCoils(master, 1, 0, 8)));
        writeCoil(master, 1, 0, true);
        System.out.println(Arrays.toString(readCoils(master, 1, 0, 8)));

        System.out.println(Arrays.toString(readHoldingRegisters(master, 1, 52, 6)));
        master.destroy();
    }

}

