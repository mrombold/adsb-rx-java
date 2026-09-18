package com.mrombold.adsbreceiver.model;

import java.time.Instant;
///////////////////////////////////////////////////////////////
public record OwnshipPosition(
        double latitude,
        double longitude,
        Double altitudeMslMeters,
        Double altitudeHaeMeters,
        Double trackDegrees,
        Double groundSpeedKnots,
        Double verticalSpeedFpm,
        int fixMode,
        int satellitesVisible,
        int satellitesUsed,
        Instant receivedAt
) {
}