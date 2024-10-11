package com.dw.tool.snmp;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DdmInfoDTO {

    private String portId;

    private String vendor;

    private String pn;

    private String ddmiSN;

    private String ddmiVersion;

    private String ddmiMode;

    private String manutrueDate;

    private String currTemp;

    private String currVol;

    private String currTxPwr;

    private String currRxPwr;

    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }
}
