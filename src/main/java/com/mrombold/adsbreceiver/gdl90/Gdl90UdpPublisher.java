package com.mrombold.adsbreceiver.gdl90;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
///////////////////////////////////////////////////////////////////
public class Gdl90UdpPublisher implements AutoCloseable {
    private final DatagramSocket socket;
    private final InetAddress destination;
    private final int port;

    public Gdl90UdpPublisher(String destinationAddress, int port)
            throws IOException {
        socket = new DatagramSocket(4000);
        socket.setBroadcast(true);
        socket.setSendBufferSize(1024 * 1024);
        destination =InetAddress.getByName(destinationAddress);
        this.port = port;
        System.out.printf("GDL90 destination %s:%d%n", destinationAddress, port);
    }

    public void send(byte[] message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, destination, port);
        socket.send(packet);
    }

    @Override
    public void close() {
        socket.close();
    }
}