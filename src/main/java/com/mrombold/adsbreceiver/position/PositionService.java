package com.mrombold.adsbreceiver.position;

import com.mrombold.adsbreceiver.model.OwnshipPosition;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
/////////////////////////////////////////////////////////////////////////////////
public class PositionService {
    private final AtomicReference<OwnshipPosition> current = new AtomicReference<>();
    private volatile int satellitesVisible;
    private volatile int satellitesUsed;

    public void updateSatellites(int visible, int used) {
        satellitesVisible = visible;
        satellitesUsed = used;
    }

    public void updateFix(double latitude, double longitude, Double altitudeMslMeters, Double altitudeHaeMeters, Double trackDegrees, Double groundSpeedKnots, Double verticalSpeedFpm, int fixMode) {
        current.set(new OwnshipPosition(latitude, longitude, altitudeMslMeters, altitudeHaeMeters, trackDegrees, groundSpeedKnots, verticalSpeedFpm, fixMode, satellitesVisible, satellitesUsed, Instant.now()));
    }

    public Optional<OwnshipPosition> getFreshPosition() {
        OwnshipPosition position = current.get();
        if (position == null) {
            return Optional.empty();
        }

        Duration age = Duration.between(position.receivedAt(), Instant.now());

        if (age.compareTo(Duration.ofSeconds(10)) > 0) {
            return Optional.empty();
        }

        return Optional.of(position);
    }

    public boolean hasFreshPosition() {
        return getFreshPosition().isPresent();
    }
}