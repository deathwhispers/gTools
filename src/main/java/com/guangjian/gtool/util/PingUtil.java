package com.guangjian.gtool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingUtil {

    /**
     * ping-检测设备是否在线
     *
     * @param ipAddress IP或者域名
     * @return 是否在线
     */
    public static boolean ping(String ipAddress) {
        return ping(ipAddress, 4, 2);
    }

    /**
     * ping-检测设备是否在线
     *
     * @param ipAddress IP或者域名
     * @param count     ping的次数
     * @param timeout   超时时间(秒)
     * @return 是否在线
     */
    public static boolean ping(String ipAddress, int count, int timeout) {
        BufferedReader in = null;
        String pingCommand;
        Runtime r = Runtime.getRuntime();
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            pingCommand = "ping" + " -n " + count + " -w " + timeout * 1000 + " " + ipAddress;
        } else {
            pingCommand = "ping" + " -c " + count + " -W " + timeout + " -i 0.2 " + ipAddress;
        }
        try {
            Process p = r.exec(pingCommand);
            if (p == null) {
                return false;
            }
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int connectedCount = 0;
            String line;
            while ((line = in.readLine()) != null) {
                connectedCount += getCheckResult(line,osName);
            }
            return connectedCount >= 2;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static int getCheckResult(String line,String osName) {
        if(osName.contains("Windows")){
            if(line.contains("TTL=")){
                return 1;
            }
        }else{
            if(line.contains("ttl=")){
                return 1;
            }
        }
        return 0;
    }

}
