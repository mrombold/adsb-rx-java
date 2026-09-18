package com.mrombold.adsbreceiver.model;

import java.time.Instant;
//////////////////////////////////////////////////////
public record AircraftUpdate(
        String icao,
        String callsign,
        Integer altitudeFeet,
        Double latitude,
        Double longitude,
        Double groundSpeedKnots,
        Double trackDegrees,
        Integer verticalRateFpm,
        Boolean onGround,
        Instant receivedAt
) {
}