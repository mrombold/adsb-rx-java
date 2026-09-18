package com.mrombold.adsbreceiver.gdl90;

import com.mrombold.adsbreceiver.ahrs.AhrsData;
import com.mrombold.adsbreceiver.ahrs.AhrsService;
import com.mrombold.adsbreceiver.model.AircraftSnapshot;
import com.mrombold.adsbreceiver.model.OwnshipPosition;
import com.mrombold.adsbreceiver.position.PositionService;
import com.mrombold.adsbreceiver.traffic.TrafficService;
import com.mrombold.adsbreceiver.weather.WeatherService;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
////////////////////////////////////////////////////////////////////////
public class Gdl90Service {
    private final Gdl90Encoder encoder;
    private final Gdl90Transmitter transmitter;
    private final TrafficService trafficService;
    private final PositionService positionService;
    private final WeatherService weatherService;
    private final AhrsService ahrsService;

    public Gdl90Service(
            Gdl90Encoder encoder,
            Gdl90Transmitter transmitter,
            TrafficService trafficService,
            PositionService positionService,
            WeatherService weatherService,
            AhrsService ahrsService) {

        this.encoder = encoder;
        this.transmitter = transmitter;
        this.trafficService = trafficService;
        this.positionService = positionService;
        this.weatherService = weatherService;
        this.ahrsService = ahrsService;
    }

    public void runStandardLoop() {
        System.out.println("GDL90 standard loop started");

        while (!Thread.currentThread().isInterrupted()) {
            long cycleStart = System.nanoTime();
            try {
                Optional<OwnshipPosition> ownship = positionService.getFreshPosition();
                boolean gpsValid = ownship.isPresent();
                boolean ahrsValid = ahrsService.getFreshData().isPresent();
                int satellitesVisible = 0;
                int satellitesUsed = 0;
                if (ownship.isPresent()) {
                    satellitesVisible = ownship.get().satellitesVisible();
                    satellitesUsed = ownship.get().satellitesUsed();
                }

                /*
                 * Submit the complete heartbeat
                 * group as ONE queue entry.
                 *
                 * The transmitter therefore sends
                 * all three consecutively.
                 */
                transmitter.submitBundle("heartbeat",
                        encoder.heartbeat(gpsValid),
                        encoder.stratuxHeartbeat(gpsValid,ahrsValid),
                        encoder.stratuxStatus(gpsValid, ahrsValid, satellitesVisible, satellitesUsed));

                /*
                 * Ownship for now remains part
                 * of this one-second loop.
                 *
                 * We'll separate it to 5 Hz later.
                 */
                if (ownship.isPresent()) {
                    transmitter.submitBundle("ownship",
                            encoder.ownship(ownship.get()),
                            encoder.ownshipGeometricAltitude(ownship.get())
                    );
                }

                /*
                 * Only targets with a recently
                 * received position are sent.
                 */
                List<AircraftSnapshot> targets = trafficService.snapshot()
                                .stream()
                                .filter(target -> target.hasFreshPosition(Duration.ofSeconds(10)))
                                .toList();

                /*
                 * Distribute approximately one
                 * complete traffic update across
                 * 800 ms.
                 */
                long spacingNanos = 0;

                if (!targets.isEmpty()) {
                    spacingNanos = 800_000_000L / targets.size();
                }

                for (int i = 0; i < targets.size(); i++) {
                    AircraftSnapshot target = targets.get(i);
                    transmitter.submit("traffic " + target.icao(), encoder.traffic(target));
                    if (spacingNanos > 0 && i < targets.size() - 1) {
                        Thread.sleep(Duration.ofNanos(spacingNanos));
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            /*
             * Complete one-second cycle.
             */
            long elapsed = System.nanoTime() - cycleStart;
            long remaining = 1_000_000_000L - elapsed;
            if (remaining > 0) {
                try {
                    Thread.sleep(Duration.ofNanos(remaining));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void runWeatherLoop() {
        System.out.println("GDL90 weather loop started");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                /*
                 * Blocks here until a NEW
                 * UAT frame arrives.
                 */
                byte[] frame = weatherService.take();
                transmitter.submit("weather", encoder.uplink(frame));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void runAhrsLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Optional<AhrsData> data = ahrsService.getFreshData();
                if (data.isPresent()) {
                    transmitter.submit("ahrs", encoder.ahrs(data.get()));
                }
                Thread.sleep(Duration.ofMillis(200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}