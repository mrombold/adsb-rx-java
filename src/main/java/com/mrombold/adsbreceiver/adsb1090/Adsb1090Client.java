package com.mrombold.adsbreceiver.adsb1090;

import com.mrombold.adsbreceiver.model.AircraftUpdate;
import com.mrombold.adsbreceiver.traffic.TrafficService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
//////////////////////////////////////////////////////////////////////////////////////
public class Adsb1090Client {
    private final String host;
    private final int port;
    private final Adsb1090Parser parser;
    private final TrafficService trafficService;

    public Adsb1090Client(String host, int port, Adsb1090Parser parser, TrafficService trafficService) {
        this.host = host;
        this.port = port;
        this.parser = parser;
        this.trafficService = trafficService;
    }

    public void run() {
        System.out.println("ADSB1090 client started");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                readSocket();
            } catch (IOException e) {
                System.err.println("dump1090 connection error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            /*
             * Wait two seconds before trying to reconnect.
             */
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
            System.out.printf("Connecting to dump1090 at %s:%d%n", host, port);
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setKeepAlive(true);
            System.out.println("Connected to dump1090");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    AircraftUpdate update = parser.parse(line);
                    if (update != null) {
                        trafficService.submit(update);
                    }
                }
            }
        }
    }
}