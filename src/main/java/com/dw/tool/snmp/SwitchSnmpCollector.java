package com.dw.tool.snmp;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2024/6/11 16:05
 */
@Slf4j
@Service
public class SwitchSnmpCollector {

    private final SNMPUtils snmpUtils = SNMPUtils.getInstance();

    private static final String DEFAULT_COMMUNITY = "public";
    // ifInOctets
    private static final String IF_IN_OCTETS_OID = "1.3.6.1.2.1.2.2.1.10";
    // ifOutOctets
    private static final String IF_OUT_OCTETS_OID = "1.3.6.1.2.1.2.2.1.16";
    // ifOperStatus
    private static final String IF_OPER_STATUS_OID = "1.3.6.1.2.1.2.2.1.8";
    // 速率
    private static final String IF_SPEED_OID = "1.3.6.1.2.1.2.2.1.5";
    // DDM 的 OID

    // 描述
    private static final String IF_DESC_OID = "1.3.6.1.2.1.2.2.1.2";
    // cpu 使用率
    private static final String CPU_USAGE_OID = "1.3.6.1.4.1.9.2.1.58";
    private static final String SWITCH_VERSION_OID = "1.3.6.1.2.1.1.1.0";
    private static final String SWITCH_UPTIME_OID = "1.3.6.1.2.1.1.3.0";
    // 双工状态
    private static final String DUPLEX_OID = "1.3.6.1.2.1.10.7.2.1.19";
    // 介质类型
    private static final String MEDIA_TYPE_OID = "1.3.6.1.2.1.2.2.1.3";
    // mtu / max_frame
    private static final String MTU_OID = "1.3.6.1.2.1.2.2.1.4";
    // mode
    private static final String MODE_OID = "1.3.6.1.2.1.10.7.2.1.19";
    /********************************    光模块   **************************************/
    private static final String DDM_PREFIX = "1.3.6.1.4.1.53911.1.2.2.1.1.2";
    private static final String DDM_TX_POWER = ".3";
    private static final String DDM_RX_POWER = ".4";
    private static final String DDM_TEMPERATURE = ".1";

    // 存储每个设备每个端口的历史流量数据 key: ip_portNum
    private final Map<String, TrafficData> trafficHistory = new ConcurrentHashMap<>();

    public static void main(String[] args) {
    }

    public SwitchMonitor getSwitchData(String ip) {
        return getSwitchData(ip, DEFAULT_COMMUNITY);
    }

    public SwitchMonitor getSwitchData(String ip, String community) {
        SwitchMonitor switchMonitor = new SwitchMonitor();

        // 获取交换机的 CPU 使用率
        Long cpuUsage = snmpUtils.getSingleValueLong(ip, community, CPU_USAGE_OID);
        if (cpuUsage != null) {
            switchMonitor.setCpuUsage(String.valueOf(cpuUsage));
        }

        // 获取交换机的版本
        String switchVersion = snmpUtils.getSingleValue(ip, community, SwitchSnmpCollector.SWITCH_VERSION_OID);
        switchMonitor.setVersion(switchVersion);

        // 获取交换机的在线时间并转换为秒
        Long upTime = snmpUtils.getSingleValueLong(ip, community, SWITCH_UPTIME_OID);
        if (upTime == null) {
            upTime = 0L;
        }
        switchMonitor.setUpTime(String.valueOf(upTime * 10));
        return switchMonitor;
    }

    public List<TrafficData> getSwitchTrafficData(String ip) {
        List<TrafficData> list = getSwitchTrafficData(ip, DEFAULT_COMMUNITY);

        return list;
    }

    private DdmInfoDTO getOpticalData(String ip, Integer portNum, String community) {
        String[] oids = new String[3];
        int index = 0;
        oids[index++] = DDM_PREFIX + "." + portNum + DDM_TEMPERATURE;
        oids[index++] = DDM_PREFIX + "." + portNum + DDM_TX_POWER;
        oids[index++] = DDM_PREFIX + "." + portNum + DDM_RX_POWER;

        // 发送 GETBULK 请求
        ResponseEvent responseEvent = snmpUtils.sendGetRequest(ip, community, oids);
        DdmInfoDTO ddmInfo = new DdmInfoDTO();
        if (responseEvent != null && responseEvent.getResponse() != null) {
            Vector<? extends VariableBinding> variableBindings = responseEvent.getResponse().getVariableBindings();
            for (VariableBinding vb : variableBindings) {
                OID oid = vb.getOid();
                String oidString = oid.toString();
                if (oidString.endsWith(DDM_TEMPERATURE)) {
                    ddmInfo.setCurrTemp(vb.getVariable().toString());
                } else if (oidString.endsWith(DDM_TX_POWER)) {
                    ddmInfo.setCurrTxPwr(vb.getVariable().toString());
                } else if (oidString.endsWith(DDM_RX_POWER)) {
                    ddmInfo.setCurrRxPwr(vb.getVariable().toString());
                }
            }
        }
        return ddmInfo;
    }

    public List<TrafficData> getSwitchTrafficData(String ip, String community) {
        List<TrafficData> trafficDataList = new ArrayList<>();
        try {
            // 第一步：获取所有端口的描述
            List<TreeEvent> ifDescEvents = snmpUtils.getSubtree(ip, community, IF_DESC_OID);
            List<Integer> gigabitPortNums = new ArrayList<>();

            // 筛选出含有 GigabitEthernet 的端口
            for (TreeEvent event : ifDescEvents) {
                if (event != null && event.getVariableBindings() != null) {
                    for (VariableBinding vb : event.getVariableBindings()) {
                        String descr = vb.getVariable().toString();
                        if (descr.contains("GigabitEthernet")) {
                            gigabitPortNums.add(vb.getOid().last());
                        }
                    }
                }
            }

            // 第二步：获取这些端口的入方向流量、出方向流量和状态
            Map<Integer, TrafficData> trafficDataMap = new HashMap<>();
            String[] oids = new String[gigabitPortNums.size() * 8];
            int index = 0;

            for (Integer portNum : gigabitPortNums) {
                oids[index++] = IF_IN_OCTETS_OID + "." + portNum;
                oids[index++] = IF_OUT_OCTETS_OID + "." + portNum;
                oids[index++] = IF_OPER_STATUS_OID + "." + portNum;
                oids[index++] = IF_SPEED_OID + "." + portNum;
                oids[index++] = DUPLEX_OID + "." + portNum;
                oids[index++] = MEDIA_TYPE_OID + "." + portNum;
                oids[index++] = MTU_OID + "." + portNum;
                oids[index++] = MODE_OID + "." + portNum;
            }
            // 发送 GETBULK 请求
            ResponseEvent responseEvent = snmpUtils.sendGetRequest(ip, community, oids);

            if (responseEvent != null && responseEvent.getResponse() != null) {
                Vector<? extends VariableBinding> variableBindings = responseEvent.getResponse().getVariableBindings();
                long timestamp = System.currentTimeMillis();
                for (VariableBinding vb : variableBindings) {
                    OID oid = vb.getOid();
                    String oidString = oid.toString();
                    int portNum = oid.last();
                    if (!gigabitPortNums.contains(portNum)) {
                        continue;
                    }

                    // 从 trafficDataMap 获取或创建 TrafficData 对象
                    TrafficData trafficData = trafficDataMap.computeIfAbsent(portNum, k -> new TrafficData());

                    trafficData.setPortNum(portNum);
                    trafficData.setTimestamp(timestamp);
                    if (oidString.contains(IF_IN_OCTETS_OID)) {
                        trafficData.setRxEtherStatsOctets(vb.getVariable().toString());
                    } else if (oidString.contains(IF_OUT_OCTETS_OID)) {
                        trafficData.setTxEtherStatsOctets(vb.getVariable().toString());
                    } else if (oidString.contains(IF_OPER_STATUS_OID)) {
                        trafficData.setLinkStatus(parseLinkStatus(vb.getVariable().toString()));
                    } else if (oidString.contains(IF_SPEED_OID)) {
                        long speed = vb.getVariable().toLong();
                        String linkSpeed = "";
                        if (speed > 0) {
                            linkSpeed = NumberUtil.div(speed, 1000_000, 0) + " Mbps";
                        }
                        trafficData.setLinkSpeed(linkSpeed);
                    } else if (oidString.contains(DUPLEX_OID)) {
                        trafficData.setSpeedDuplex(vb.getVariable().toString());
                    } else if (oidString.contains(MEDIA_TYPE_OID)) {
                        trafficData.setMediaType(vb.getVariable().toString());
                    } else if (oidString.contains(MTU_OID)) {
                        trafficData.setMaxFrame(vb.getVariable().toString());
                    } else if (oidString.contains(MODE_OID)) {
                        trafficData.setMode(vb.getVariable().toString());
                    }
                    if (checkIsOpticalPort(portNum)) {
                        DdmInfoDTO opticalData = getOpticalData(ip, portNum, community);
                        trafficData.setDdmInfo(opticalData);
                    }
                }

                // 计算并设置最近5分钟的平均流量
                for (TrafficData trafficData : trafficDataMap.values()) {
                    if ("Up".equals(trafficData.getLinkStatus())) {
                        trafficData.setRxAverage(calculateRxAverageTraffic(ip, trafficData));
                        trafficData.setTxAverage(calculateTxAverageTraffic(ip, trafficData));
                    } else {
                        trafficData.setRxAverage("0");
                        trafficData.setTxAverage("0");
                    }
                    trafficDataList.add(trafficData);
                    updateTrafficHistory(ip, trafficData);
                }
            } else {
                log.warn("No response from SNMP agent for address: {}", ip);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve traffic data for switch: {}", ip, e);
        }
        return trafficDataList;
    }

    private String parseLinkStatus(String status) {
        int statusInt = Integer.parseInt(status);
        return (statusInt == 1) ? "Up" : "Down";
    }

    private void updateTrafficHistory(String ip, TrafficData cur) {
        Integer portNum = cur.getPortNum();
        trafficHistory.put(ip + "_" + portNum, cur);
    }

    // byte/s
    private String calculateRxAverageTraffic(String ip, TrafficData cur) {
        Integer portNum = cur.getPortNum();
        TrafficData old = trafficHistory.get(ip + "_" + portNum);
        if (old == null) {
            return "";
        }
        // 时间差：ms
        BigDecimal time_diff = NumberUtil.sub(cur.getTimestamp(), old.getTimestamp());
        BigDecimal diff = NumberUtil.sub(cur.getRxEtherStatsOctets(), old.getRxEtherStatsOctets());
        return diff.multiply(BigDecimal.valueOf(1000)).divide(time_diff, 2, RoundingMode.HALF_UP).toString();
    }

    private String calculateTxAverageTraffic(String ip, TrafficData cur) {
        Integer portNum = cur.getPortNum();
        TrafficData old = trafficHistory.get(ip + "_" + portNum);
        if (old == null) {
            return "";
        }
        // 时间差：ms
        BigDecimal time_diff = NumberUtil.sub(cur.getTimestamp(), old.getTimestamp());
        BigDecimal bit = NumberUtil.sub(cur.getTxEtherStatsOctets(), old.getTxEtherStatsOctets());
        return bit.multiply(BigDecimal.valueOf(1000)).divide(time_diff, 2, RoundingMode.HALF_UP).toString();
    }

    /**
     * 根据配置判断是否为光口
     *
     * @param portNum 端口号
     * @return <ul>
     * <li>返回 {@code true}，则是光口</li>
     * <li>返回{@code false}则不是光口</li>
     * </ul>
     */
    private Boolean checkIsOpticalPort(Integer portNum) {
        return true;
    }

}
