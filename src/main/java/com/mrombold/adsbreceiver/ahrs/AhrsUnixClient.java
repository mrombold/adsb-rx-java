package com.mrombold.adsbreceiver.ahrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Instant;
///////////////////////////////////////////////////////////////////////////////////////////
public class AhrsUnixClient {
    private final Path socketPath;
    private final AhrsService ahrsService;
    private final ObjectMapper mapper = new ObjectMapper();
    public AhrsUnixClient(Path socketPath, AhrsService ahrsService) {
        this.socketPath = socketPath;
        this.ahrsService = ahrsService;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                readSocket();
            } catch (IOException e) {
                // AHRS is optional.
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void readSocket() throws IOException {
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);

        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(address);
            System.out.println("Connected to AHRS at " + socketPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(channel)));
            String line;
            while ((line = reader.readLine()) != null) {
                parse(line);
            }
        }
    }

    private void parse(String line) {
        try {
            JsonNode root = mapper.readTree(line);
            AhrsData data = new AhrsData(root.path("roll").asDouble(),
                            root.path("pitch").asDouble(),
                            root.path("yaw").asDouble(),
                            root.path("roll_rate").asDouble(),
                            root.path("pitch_rate").asDouble(),
                            root.path("yaw_rate").asDouble(),
                            Instant.now()
                    );
            ahrsService.update(data);
        } catch (IOException e) {
            System.err.println("AHRS JSON parse error: " + e.getMessage()
            );
        }
    }
}