package com.mrombold.adsbreceiver.model;

import java.time.Duration;
import java.time.Instant;

public record AircraftSnapshot(
        String icao,
        String callsign,
        Integer altitudeFeet,
        Double latitude,
        Double longitude,
        Double groundSpeedKnots,
        Double trackDegrees,
        Integer verticalRateFpm,
        Boolean onGround,
        Instant lastSeen,
        Instant lastPositionUpdate,
        Instant lastVelocityUpdate
) {

    public boolean hasFreshPosition(Duration maxAge) {
        if (latitude == null || longitude == null || lastPositionUpdate == null) {
            return false;
        }
        return Duration.between(lastPositionUpdate, Instant.now()).compareTo(maxAge) <= 0;
    }
}