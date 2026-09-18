package com.mrombold.adsbreceiver.ahrs;

import java.time.Instant;
///////////////////////////////////////////////////////////
public record AhrsData(
        double rollDegrees,
        double pitchDegrees,
        double headingDegrees,
        double rollRateDegreesPerSecond,
        double pitchRateDegreesPerSecond,
        double yawRateDegreesPerSecond,
        Instant receivedAt
) {
}