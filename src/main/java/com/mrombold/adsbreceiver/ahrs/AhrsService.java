package com.mrombold.adsbreceiver.ahrs;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
////////////////////////////////////////////////////////////////////
public class AhrsService {

    private final AtomicReference<AhrsData> current = new AtomicReference<>();
    public void update(AhrsData data) {
        current.set(data);
    }

    public Optional<AhrsData> getFreshData() {
        AhrsData data = current.get();
        if (data == null) {
            return Optional.empty();
        }
        Duration age = Duration.between(data.receivedAt(), Instant.now());

        if (age.compareTo(Duration.ofSeconds(1)) > 0) {
            return Optional.empty();
        }

        return Optional.of(data);
    }
}