package com.mrombold.adsbreceiver.weather;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
////////////////////////////////////////////////////////////////////////////
public class WeatherService {
    private final BlockingQueue<byte[]> frames = new ArrayBlockingQueue<>(1000);
    public void submit(byte[] frame) throws InterruptedException {
        /*
         * Defensive copy so nobody can
         * modify the frame later.
         */
        frames.put(frame.clone());
    }

    public byte[] take() throws InterruptedException {
        return frames.take();
    }

    public int queueDepth() {
        return frames.size();
    }
}