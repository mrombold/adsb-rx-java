package com.mrombold.adsbreceiver.gps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrombold.adsbreceiver.position.PositionService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
////////////////////////////////////////
public class GpsdClient {
    private static final double METERS_PER_SECOND_TO_KNOTS = 1.943844;
    private static final double METERS_PER_SECOND_TO_FPM = 196.850394;
    private final String host;
    private final int port;
    private final PositionService positionService;
    private final ObjectMapper mapper = new ObjectMapper();

    public GpsdClient(String host, int port, PositionService positionService) {
        this.host = host;
        this.port = port;
        this.positionService = positionService;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                readGpsd();
            } catch (IOException e) {
                System.err.println("gpsd connection error: " + e.getMessage());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void readGpsd() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port),5000);
            System.out.printf("Connected to gpsd at %s:%d%n", host, port);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write("?WATCH={\"enable\":true,\"json\":true}\n");
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line);
            }
        }
    }

    private void parseLine(String line) {
        try {
            JsonNode root = mapper.readTree(line);
            String messageClass = root.path("class").asText("");
            switch (messageClass) {
                case "SKY" -> parseSky(root);
                case "TPV" -> parseTpv(root);
                default -> {
                    // Ignore VERSION, DEVICES, etc.
                }
            }

        } catch (IOException e) {
            System.err.println("gpsd JSON parse error: " + e.getMessage()
            );
        }
    }

    private void parseSky(JsonNode root) {
        int visible = root.path("nSat").asInt(0);
        int used = root.path("uSat").asInt(0);
        positionService.updateSatellites(visible, used);
    }

    private void parseTpv(JsonNode root) {
        int mode = root.path("mode").asInt(0);
        if (mode < 2) {
            return;
        }

        JsonNode latNode = root.get("lat");
        JsonNode lonNode = root.get("lon");

        if (latNode == null || lonNode == null || !latNode.isNumber() || !lonNode.isNumber()) {
            return;
        }

        double latitude = latNode.asDouble();
        double longitude = lonNode.asDouble();
        Double altitudeMsl = number(root, "altMSL");

        if (altitudeMsl == null) {
            altitudeMsl = number(root, "alt");
        }

        Double altitudeHae = number(root, "altHAE");
        Double track = number(root, "track");
        Double speed = number(root, "speed");

        if (speed != null) {
            speed *= METERS_PER_SECOND_TO_KNOTS;
        }

        Double climb = number(root, "climb");

        if (climb != null) {
            climb *= METERS_PER_SECOND_TO_FPM;
        }

        positionService.updateFix(latitude, longitude, altitudeMsl, altitudeHae, track, speed, climb, mode);
    }

    private Double number(JsonNode root, String name) {
        JsonNode node = root.get(name);

        if (node == null || !node.isNumber()) {
            return null;
        }

        return node.asDouble();
    }
}