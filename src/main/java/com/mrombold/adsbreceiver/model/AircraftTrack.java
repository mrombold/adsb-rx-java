package com.mrombold.adsbreceiver.model;

import java.time.Duration;
import java.time.Instant;
///////////////////////////////////////////////////////////////////
public class AircraftTrack {
    private final String icao;
    private String callsign;
    private Integer altitudeFeet;
    private Double latitude;
    private Double longitude;
    private Double groundSpeedKnots;
    private Double trackDegrees;
    private Integer verticalRateFpm;
    private Boolean onGround;
    private Instant lastSeen;
    private Instant lastPositionUpdate;
    private Instant lastVelocityUpdate;
    public AircraftTrack(String icao) {
        this.icao = icao;
    }
    public void apply(AircraftUpdate update) {
        lastSeen = update.receivedAt();

        if (update.callsign() != null) {
            callsign = update.callsign();
        }

        if (update.altitudeFeet() != null) {
            altitudeFeet = update.altitudeFeet();
        }

        if (update.latitude() != null && update.longitude() != null) {
            latitude = update.latitude();
            longitude = update.longitude();
            lastPositionUpdate = update.receivedAt();
        }

        if (update.groundSpeedKnots() != null) {
            groundSpeedKnots = update.groundSpeedKnots();
            lastVelocityUpdate = update.receivedAt();
        }

        if (update.trackDegrees() != null) {
            trackDegrees = update.trackDegrees();
            lastVelocityUpdate = update.receivedAt();
        }

        if (update.verticalRateFpm() != null) {
            verticalRateFpm = update.verticalRateFpm();
            lastVelocityUpdate = update.receivedAt();
        }

        if (update.onGround() != null) {
            onGround = update.onGround();
        }
    }

    public boolean isStale(Duration maxAge) {
        if (lastSeen == null) {
            return true;
        }
        return Duration.between(lastSeen,Instant.now()).compareTo(maxAge) > 0;
    }

    public AircraftSnapshot snapshot() {
        return new AircraftSnapshot(
                icao,
                callsign,
                altitudeFeet,
                latitude,
                longitude,
                groundSpeedKnots,
                trackDegrees,
                verticalRateFpm,
                onGround,
                lastSeen,
                lastPositionUpdate,
                lastVelocityUpdate
        );
    }

    @Override
    public String toString() {
        return String.format(
                "%s %-8s alt=%s lat=%s lon=%s speed=%s track=%s",
                icao,
                callsign != null ? callsign : "",
                altitudeFeet,
                latitude,
                longitude,
                groundSpeedKnots,
                trackDegrees
        );
    }
}