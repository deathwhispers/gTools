package com.dw.tool.snmp;

import lombok.Data;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2024/6/13 11:38
 */
@Data
public class SwitchMonitor {
    private String upTime;
    private String cpuUsage;
    private String version;
    private Extra extra;

}

@Data
class Extra {
    private String disk;
    private String diskStatus;
    private String memory;
    private String memoryStatus;
}
