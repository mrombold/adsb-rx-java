package com.mrombold.adsbreceiver.adsb978;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrombold.adsbreceiver.model.AircraftUpdate;
import com.mrombold.adsbreceiver.traffic.TrafficService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
//////////////////////////////////////////////////////////////////////
public class Adsb978TrafficClient {
    private final String host;
    private final int port;
    private final TrafficService trafficService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Adsb978TrafficClient(String host, int port, TrafficService trafficService) {
        this.host = host;
        this.port = port;
        this.trafficService = trafficService;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                readSocket();
            } catch (IOException e) {
                System.err.println("dump978 traffic error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void readSocket() throws IOException, InterruptedException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host,port), 5000);
            System.out.printf("Connected to dump978 traffic at %s:%d%n", host, port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                AircraftUpdate update = parse(line);
                if (update != null) {
                    trafficService.submit(update);
                }
            }
        }
    }

    private AircraftUpdate parse(String line) {
        try {
            JsonNode root = mapper.readTree(line);
            String icao = text(root, "address");

            if (icao == null) {
                return null;
            }

            icao = icao.trim().toUpperCase();
            String callsign = text(root, "callsign");

            if (callsign != null) {
                callsign = callsign.trim();
            }

            Integer altitude = integer(root, "pressure_altitude");

            if (altitude == null) {
                altitude = integer(root, "geometric_altitude");
            }

            Double latitude = null;
            Double longitude = null;
            JsonNode position = root.get("position");

            if (position != null) {
                latitude = number(position, "lat");
                longitude = number(position, "lon");
            }

            Double speed = number(root,"ground_speed");
            Double track = number(root, "true_track");
            Integer verticalRate = integer(root, "vertical_velocity_barometric");

            if (verticalRate == null) {
                verticalRate = integer(root, "vertical_velocity_geometric");
            }

            Boolean onGround = null;
            String airground = text(root, "airground_state");

            if (airground != null) {
                onGround = !airground.equalsIgnoreCase("airborne");
            }

            return new AircraftUpdate(icao, callsign, altitude, latitude, longitude, speed, track, verticalRate, onGround, Instant.now());
        } catch (IOException e) {
            System.err.println("dump978 JSON parse error: " + e.getMessage());
            return null;
        }
    }

    private String text(JsonNode node, String name) {
        JsonNode child = node.get(name);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asText();
    }

    private Double number(JsonNode node, String name) {
        JsonNode child = node.get(name);
        if (child == null || !child.isNumber()) {
            return null;
        }
        return child.asDouble();
    }

    private Integer integer(JsonNode node, String name) {
        JsonNode child = node.get(name);
        if (child == null || !child.isNumber()) {
            return null;
        }
        return child.asInt();
    }
}