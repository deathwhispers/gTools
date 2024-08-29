package com.dw.tool.util;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.locator.BaseLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2023/8/17 9:23
 */
@Slf4j
public class ModbusRTUUtil {


    // 创建一个ModbusFactory对象
    private static ModbusFactory factory = new ModbusFactory();

    // 创建一个ModbusMaster对象
    private static ModbusMaster master;

    // 连接ModbusMaster对象
    public static void connect(String port, int baudRate, int dataBits, int stopBits, int parity) {
/*        // 创建一个SerialParameters对象
        SerialParameters params = new SerialParameters();
        // 设置端口号
        params.setCommPortId(port);
        // 设置波特率
        params.setBaudRate(baudRate);
        // 设置数据位
        params.setDataBits(dataBits);
        // 设置停止位
        params.setStopBits(stopBits);
        // 设置奇偶校验
        params.setParity(parity);

        // 调用ModbusFactory的createRtuMaster方法
        master = factory.createRtuMaster(params);

        try {
            // 调用ModbusMaster的init方法
            master.init();
            System.out.println("连接成功");
        } catch (ModbusInitException e) {
            e.printStackTrace();
            System.out.println("连接失败");
        }*/
    }

    // 断开ModbusMaster对象
    public static void disconnect() {
        try {
            master.destroy();
            System.out.println("断开成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("断开失败");
        }
    }

    // 读取线圈（功能码01）
    public static boolean readCoil(int slaveId, int offset) {
        boolean value = false;
        try {
            // 创建一个BaseLocator对象，用于指定线圈的范围，类型和偏移量
            BaseLocator<Boolean> locator = BaseLocator.coilStatus(slaveId, offset);
            // 调用ModbusMaster的getValue方法，传入BaseLocator对象，得到一个Boolean对象
            value = master.getValue(locator);
            System.out.println("读取成功");
            System.out.println("线圈" + offset + "的值为：" + value);
        } catch (ModbusTransportException | ErrorResponseException e) {
            e.printStackTrace();
            System.out.println("读取失败");
        }
        return value;
    }

    // 写入线圈（功能码05）
    public static void writeCoil(int slaveId, int offset, boolean value) {
        try {
            // 创建一个BaseLocator对象，用于指定线圈的范围，类型和偏移量
            BaseLocator<Boolean> locator = BaseLocator.coilStatus(slaveId, offset);
            // 调用ModbusMaster的setValue方法，传入BaseLocator对象和Boolean对象，写入数据到线圈
            master.setValue(locator, value);
            System.out.println("写入成功");
        } catch (ModbusTransportException | ErrorResponseException e) {
            e.printStackTrace();
            System.out.println("写入失败");
        }
    }

    // 读取离散输入（功能码02）
    public static boolean readDiscreteInput(int slaveId, int offset) {
        boolean value = false;
        try {
            // 创建一个BaseLocator对象，用于指定离散输入的范围，类型和偏移量
            BaseLocator<Boolean> locator = BaseLocator.inputStatus(slaveId, offset);
            // 调用ModbusMaster的getValue方法，传入BaseLocator对象，得到一个Boolean对象
            value = master.getValue(locator);
            System.out.println("读取成功");
            System.out.println("离散输入" + offset + "的值为：" + value);
        } catch (ModbusTransportException | ErrorResponseException e) {
            e.printStackTrace();
            System.out.println("读取失败");
        }
        return value;
    }

    // 读取保持寄存器（功能码03）
    public static Number readHoldingRegister(int slaveId, int offset, int dataType) {
        Number value = 0;
        try {
            // 创建一个BaseLocator对象，用于指定保持寄存器的范围，类型和偏移量
            BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset, dataType);
            // 调用ModbusMaster的getValue方法，传入BaseLocator对象，得到一个Number对象
            value = master.getValue(locator);
            System.out.println("读取成功");
            System.out.println("保持寄存器" + offset + "的值为：" + value);
        } catch (ModbusTransportException | ErrorResponseException e) {
            e.printStackTrace();
            System.out.println("读取失败");
        }
        return value;
    }

    // 写入保持寄存器（功能码06）
    public static void writeHoldingRegister(int slaveId, int offset, Number value, int dataType) {
        try {
            // 创建一个BaseLocator对象，用于指定保持寄存器的范围，类型和偏移量
            BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset, dataType);
            // 调用ModbusMaster的setValue方法，传入BaseLocator对象和Number对象，写入数据到保持寄存器
            master.setValue(locator, value);
            System.out.println("写入成功");
        } catch (ModbusTransportException | ErrorResponseException e) {
            e.printStackTrace();
            System.out.println("写入失败");
        }
    }

    // 读取输入寄存器（功能码04）
    public static Number readInputRegister(int slaveId, int offset, int dataType) {
        Number value = 0;
        try {
            // 创建一个BaseLocator对象，用于指定输入寄存器的范围，类型和偏移量
            BaseLocator<Number> locator = BaseLocator.inputRegister(slaveId, offset, dataType);
            // 调用ModbusMaster的getValue方法，传入BaseLocator对象，得到一个Number对象
            value = master.getValue(locator);
            System.out.println("读取成功");
            System.out.println("输入寄存器" + offset + "的值为：" + value);
        } catch (ModbusTransportException | ErrorResponseException e) {
            e.printStackTrace();
            System.out.println("读取失败");
        }
        return value;
    }

}
