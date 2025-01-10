package com.dw.tool.snmp;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2024/9/3 9:03
 */
@Slf4j
public class SNMPUtils {


    private static Snmp snmp;
    private static final int DEFAULT_PORT = 161;
    private static final String DEFAULT_PROTOCOL = "udp";
    private static final String DEFAULT_COMMUNITY = "public";
    // 协议版本
    private static final int DEFAULT_VERSION = SnmpConstants.version2c;
    // 超时时间
    private static final int DEFAULT_TIMEOUT = 1500;
    // 重试次数
    private static final int DEFAULT_RETRIES = 2;

    private static final class InstanceHolder {
        static final SNMPUtils instance = new SNMPUtils();
    }

    public static synchronized SNMPUtils getInstance() {
        return InstanceHolder.instance;
    }

    private SNMPUtils() {
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException e) {
            log.error("Failed to start SNMP transport mapping", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (snmp != null) {
            try {
                snmp.close();
            } catch (IOException e) {
                log.error("Failed to close SNMP transport mapping", e);
            }
        }
    }

    public CommunityTarget createTarget(String ip) {
        return createTarget(ip, DEFAULT_COMMUNITY);
    }

    public CommunityTarget createTarget(String ip, String community) {
        String address = DEFAULT_PROTOCOL + ":" + ip + "/" + DEFAULT_PORT;
        Address targetAddress = GenericAddress.parse(address);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(targetAddress);
        target.setRetries(DEFAULT_RETRIES);
        target.setTimeout(DEFAULT_TIMEOUT);
        target.setVersion(DEFAULT_VERSION);
        return target;
    }

    public PDU createPDU(int pduType, String... oids) {
        PDU pdu = new PDU();
        for (String oid : oids) {
            pdu.add(new VariableBinding(new OID(oid)));
        }
        pdu.setType(pduType);
        if (pduType == PDU.GETBULK) {
            pdu.setMaxRepetitions(10);
        }
        return pdu;
    }

    public ResponseEvent sendPDUWithRetry(CommunityTarget target, PDU pdu) {
        try {
            ResponseEvent responseEvent = snmp.send(pdu, target);
            if (responseEvent == null || responseEvent.getResponse() == null) {
                log.warn("No response from SNMP agent. Retrying...");
                // 补救措施：重试一次
                responseEvent = snmp.send(pdu, target);
            }
            return responseEvent;
        } catch (IOException e) {
            log.error("IOException during SNMP request", e);
            return null;
        }
    }

    public ResponseEvent sendGetRequest(String ip, String community, String... oids) {
        CommunityTarget target = createTarget(ip, community);
        PDU pdu = createPDU(PDU.GET, oids);
        return sendPDUWithRetry(target, pdu);
    }

    public ResponseEvent sendGetBulkRequest(String ip, String community, String... oids) {
        CommunityTarget target = createTarget(ip, community);
        PDU pdu = createPDU(PDU.GETBULK, oids);
        return sendPDUWithRetry(target, pdu);
    }

    public ResponseEvent sendSetRequest(String ip, String community, VariableBinding... varBindings) {
        CommunityTarget target = createTarget(ip, community);
        PDU pdu = new PDU();
        for (VariableBinding vb : varBindings) {
            pdu.add(vb);
        }
        pdu.setType(PDU.SET);
        return sendPDUWithRetry(target, pdu);
    }

    public void sendTrap(String ip, String community, VariableBinding... varBindings) {
        CommunityTarget target = createTarget(ip, community);
        PDU pdu = new PDU();
        for (VariableBinding vb : varBindings) {
            pdu.add(vb);
        }
        pdu.setType(PDU.TRAP);
        try {
            snmp.send(pdu, target);
        } catch (IOException e) {
            log.error("IOException during SNMP trap sending", e);
        }
    }

    public List<TreeEvent> getSubtree(String ip, String community, String oid) {
        List<TreeEvent> events = new ArrayList<>();
        CommunityTarget target = createTarget(ip, community);
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> treeEvents = treeUtils.getSubtree(target, new OID(oid));

        for (TreeEvent event : treeEvents) {
            if (event != null && event.isError()) {
                log.warn("Error retrieving subtree: {}", event.getErrorMessage());
                continue;
            }
            events.add(event);
        }

        return events;
    }

    public String getSingleValue(String ip, String community, String oid) {
        ResponseEvent responseEvent = sendGetRequest(ip, community, oid);
        if (responseEvent != null && responseEvent.getResponse() != null) {
            VariableBinding vb = responseEvent.getResponse().get(0);
            return vb.getVariable().toString();
        } else {
            log.warn("No response from SNMP agent for address: {}", ip);
            return "";
        }
    }

    public Long getSingleValueLong(String ip, String community, String oid) {
        ResponseEvent responseEvent = sendGetRequest(ip, community, oid);
        if (responseEvent != null && responseEvent.getResponse() != null) {
            VariableBinding vb = responseEvent.getResponse().get(0);
            return vb.getVariable().toLong();
        } else {
            log.warn("No response from SNMP agent for address: {}", ip);
            return null;
        }
    }


    public void setSingleValue(String ip, String community, String oid) {
        VariableBinding vb = new VariableBinding(new OID(oid), new Integer32(1));

        ResponseEvent responseEvent = sendSetRequest(ip, community, vb);
        // 检查响应
        if (responseEvent != null && responseEvent.getResponse() != null) {
            System.out.println("SNMP SET Response: " + responseEvent.getResponse().toString());
        } else {
            System.out.println("SNMP SET Request Failed.");
        }
    }



    public static void main(String[] args) {
        SNMPUtils snmpUtils = SNMPUtils.getInstance();
//        snmpUtils.setSingleValue("192.168.1.234", "admin", "1.3.6.1.4.1.53911.1.1.1.0");
        System.out.println(snmpUtils.getSingleValue("192.168.1.234", "public", "1.3.6.1.4.1.53911.1.2.2.1.1.2.10.3"));
        System.out.println(snmpUtils.getSingleValue("192.168.1.234", "public", "1.3.6.1.4.1.53911.1.2.2.1.1.2.10.4"));

    }

}
