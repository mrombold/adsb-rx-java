package com.mrombold.adsbreceiver.gdl90;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
//////////////////////////////////////////////////////////////////////
public class Gdl90Transmitter {
    private final BlockingQueue<Gdl90Transmission> queue = new ArrayBlockingQueue<>(4096);
    private final Gdl90UdpPublisher publisher;
    private final AtomicLong packetsSent = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();

    public Gdl90Transmitter(Gdl90UdpPublisher publisher) {
        this.publisher = publisher;
    }

    public void submit(Gdl90Transmission transmission)
            throws InterruptedException {
        queue.put(transmission);
    }

    public void submit(String description, byte[] packet)
            throws InterruptedException {
        submit(Gdl90Transmission.single(description, packet));
    }

    public void submitBundle(String description, byte[]... packets)
            throws InterruptedException {
        submit(Gdl90Transmission.bundle(description, packets));
    }

    public int queueDepth() {
        return queue.size();
    }

    public long packetsSent() {
        return packetsSent.get();
    }

    public long bytesSent() {
        return bytesSent.get();
    }

    public void run() {
        System.out.println("GDL90 transmitter started");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Gdl90Transmission transmission = queue.take();
                /*
                 * Send every packet in this
                 * transmission consecutively.
                 *
                 * Nothing from another producer
                 * can get inserted in the middle
                 * of a bundle.
                 */
                for (byte[] packet : transmission.packets()) {
                    publisher.send(packet);
                    packetsSent.incrementAndGet();
                    bytesSent.addAndGet(packet.length);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                System.err.println(
                        "GDL90 transmit error: " +
                                e.getMessage()
                );
            }
        }
    }
}