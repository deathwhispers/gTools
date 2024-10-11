package com.dw.tool.snmp;

import lombok.Data;

/**
 * 流量数据
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2024/6/11 16:06
 */
@Data
public class TrafficData {
    private String linkStatus;
    private Integer portNum;
    // 速率双工
    private String speedDuplex;
    private String mode;
    private String maxFrame;
    private String linkSpeed;
    private String rxAverage;
    private String txAverage;
    private String rxEtherStatsOctets;
    private String txEtherStatsOctets;
    private String excessive;
    private String flowConfig;
    private String mediaType;
    private DdmInfoDTO ddmInfo;

    private Long timestamp;
}
