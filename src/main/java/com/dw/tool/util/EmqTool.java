package com.dw.tool.util;

import cn.hutool.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EmqTool {
    private static String broker = "tcp://100.65.105.15:1883";
    private static String clientId = "emq-tool";
    private static String username = "emq-tool";
    private static String password = "emq-tool";
    private static MqttClient client;

    public EmqTool(String broker, String clientId, String username, String password) throws MqttException {
        this.broker = broker;
        this.clientId = clientId != null ? clientId : "EmqToolClient";
        this.username = username;
        this.password = password;

        // Initialize MQTT client
        client = new MqttClient(broker, this.clientId, new MemoryPersistence());
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        if (username != null && password != null) {
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
        }

        // Connect to the broker
        client.connect(connOpts);
        System.out.println("Connected to broker: " + broker);
    }

    public List<String> getConnectedClientIds() throws MqttException {
        if (!client.isConnected()) {
            throw new IllegalStateException("MQTT client is not connected.");
        }

        List<String> clientIds = new ArrayList<>();
        try {
            String apiUrl = "http://100.65.105.15:8081/api/v4/clients";
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(("admin" + ":" + "public").getBytes()));

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response to extract client IDs (Assuming JSON format)
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                org.json.JSONArray clients = jsonResponse.getJSONArray("data");
                for (int i = 0; i < clients.length(); i++) {
                    org.json.JSONObject client = clients.getJSONObject(i);
                    String clientid = client.getString("clientid");
                    String ip_address = client.getString("ip_address");
                    if (ip_address.contains(".134.")) continue;
                    if (clientid.contains("lan")) {
                        String sn = clientid.replace("_lan", "");
                        clientIds.add(sn);
                    }
                }
            } else {
                System.err.println("Failed to fetch client IDs. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Fetched connected client IDs: " + clientIds);
        return clientIds;
    }

    public void sendCommand(String topic, String command) throws MqttException {
        if (!client.isConnected()) {
            throw new IllegalStateException("MQTT client is not connected.");
        }

        MqttMessage message = new MqttMessage(command.getBytes());
        message.setQos(1);
        client.publish(topic, message);
        System.out.println("Command sent to topic " + topic + ": " + command);
    }

    public void sendCommandsToAllClients(String baseTopic, JSONObject command) throws MqttException {
        List<String> clientIds = getConnectedClientIds();
        for (String clientId : clientIds) {
            String topic = baseTopic.replace("{sn}", clientId);
            command.set("sn", clientId);
            System.out.println("Command sent to topic " + topic + ": " + command);
            sendCommand(topic, command.toString());
        }
    }

    public void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            System.out.println("Disconnected from broker.");
        }
    }

    public static void genSql() throws Exception {

        EmqTool emqTool = new EmqTool(broker, clientId, username, password);

        String apiUrl = "http://100.65.105.15:8081/api/v4/clients";
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(("admin" + ":" + "public").getBytes()));

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse the response to extract client IDs (Assuming JSON format)
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
            org.json.JSONArray clients = jsonResponse.getJSONArray("data");
            for (int i = 0; i < clients.length(); i++) {
                org.json.JSONObject client = clients.getJSONObject(i);
                String clientid = client.getString("clientid");
                String ip = client.getString("ip_address");
                if (clientid.contains("lan")) {
                    String sn = clientid.replace("_lan", "");
                    String sql_tmpl = "update aibox.t_aibox_devinfo set real_sn = '{sn}' where bord_ip = '{ip}';";
                    System.out.println(sql_tmpl.replace("{sn}", sn).replace("{ip}", ip));
                }
            }
        } else {
            System.err.println("Failed to fetch client IDs. Response code: " + responseCode);
        }
        emqTool.disconnect();
    }

    public static void main(String[] args) throws Exception {
        genSql();
    }

    private static void rebootAll() throws MqttException {

        EmqTool emqTool = new EmqTool(broker, clientId, username, password);

        // Example usage
        String baseTopic = "znzd/dahua/box/{sn}/control/reboot";
        JSONObject command = new JSONObject();
        command
                .set("enable", new JSONObject()
                        .set("ipcRebootAble", "00")
                        .set("boxRebootAble", "01")
                        .set("switcherRebootAble", "00")
                )
                .set("switcher", "00")
                .set("ipc", "00")
                .set("box", "01")
                .set("type", "00");
        emqTool.sendCommandsToAllClients(baseTopic, command);
        emqTool.disconnect();
    }
}
