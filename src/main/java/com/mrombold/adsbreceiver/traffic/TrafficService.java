package com.mrombold.adsbreceiver.traffic;

import com.mrombold.adsbreceiver.model.AircraftSnapshot;
import com.mrombold.adsbreceiver.model.AircraftTrack;
import com.mrombold.adsbreceiver.model.AircraftUpdate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
////////////////////////////////////////////////////////
public class TrafficService {

    private final BlockingQueue<AircraftUpdate> updates = new ArrayBlockingQueue<>(1000);
    /*
     * Only the TrafficService virtual thread
     * modifies this map.
     */
    private final Map<String, AircraftTrack> aircraft = new HashMap<>();
    /*
     * Everyone else reads this immutable snapshot.
     */
    private final AtomicReference<List<AircraftSnapshot>> publishedSnapshot = new AtomicReference<>(List.of());
    public void submit(AircraftUpdate update) throws InterruptedException {
        updates.put(update);
    }

    public List<AircraftSnapshot> snapshot() {
        return publishedSnapshot.get();
    }

    public void run() {
        System.out.println("TrafficService started");
        long nextPublish = System.nanoTime();
        long nextCleanup = System.nanoTime();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                AircraftUpdate update = updates.poll(200, TimeUnit.MILLISECONDS);
                if (update != null) {
                    boolean newAircraft = !aircraft.containsKey(update.icao());
                    AircraftTrack track = aircraft.computeIfAbsent(update.icao(), AircraftTrack::new);
                    track.apply(update);
                    if (newAircraft) {
                        System.out.println("New aircraft: " + track);
                    }
                }

                long now = System.nanoTime();

                /*
                 * Remove aircraft that haven't
                 * transmitted anything for 15 sec.
                 */
                if (now >= nextCleanup) {
                    aircraft.values().removeIf(track -> track.isStale(Duration.ofSeconds(15)));
                    nextCleanup = now + 1_000_000_000L;
                }

                /*
                 * Publish a new immutable view
                 * five times per second.
                 */
                if (now >= nextPublish) {
                    List<AircraftSnapshot> snapshot = aircraft.values().stream().map(AircraftTrack::snapshot).toList();
                    publishedSnapshot.set(snapshot);
                    nextPublish = now + 200_000_000L;
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}