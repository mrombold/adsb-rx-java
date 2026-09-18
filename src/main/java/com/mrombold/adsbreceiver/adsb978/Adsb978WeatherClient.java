package com.mrombold.adsbreceiver.adsb978;

import com.mrombold.adsbreceiver.weather.WeatherService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HexFormat;
///////////////////////////////////////////////////////
public class Adsb978WeatherClient {
    private final String host;
    private final int port;
    private final WeatherService weatherService;
    public Adsb978WeatherClient(String host, int port, WeatherService weatherService) {
        this.host = host;
        this.port = port;
        this.weatherService = weatherService;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                readSocket();
            } catch (IOException e) {
                System.err.println("dump978 weather error: " + e.getMessage());
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
            socket.connect(new InetSocketAddress(host, port),5000);
            System.out.printf("Connected to dump978 weather at %s:%d%n", host, port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                byte[] frame = parseFrame(line);
                if (frame != null) {
                    weatherService.submit(frame);
                }
            }
        }
    }

    private byte[] parseFrame(String line) {
        /*
         * dump978 raw uplink:
         *
         * +HEXDATA;rssi=...;t=...;
         */
        if (line.isEmpty() || line.charAt(0) != '+') {
            return null;
        }

        int semicolon = line.indexOf(';');

        if (semicolon < 0) {
            return null;
        }

        String hex = line.substring(1, semicolon);

        try {
            byte[] frame = HexFormat.of().parseHex(hex);
            /*
             * UAT uplink payload is
             * exactly 432 bytes.
             */
            if (frame.length != 432) {
                System.err.printf("Unexpected UAT uplink length: %d%n", frame.length);
                return null;
            }
            return frame;

        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}