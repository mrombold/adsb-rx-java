package com.mrombold.adsbreceiver.adsb1090;

import com.mrombold.adsbreceiver.model.AircraftUpdate;
import java.time.Instant;
///////////////////////////////////////////////////////////////////////
public class Adsb1090Parser {
    public AircraftUpdate parse(String line) {
        String[] fields = line.split(",", -1);
        /*
         * SBS/BaseStation messages contain 22 fields.
         *
         * Using split(",", -1) is important because Java normally
         * throws away empty fields at the end of a string.
         */
        if (fields.length < 22) {
            return null;
        }

        if (!fields[0].equals("MSG")) {
            return null;
        }

        String icao = emptyToNull(fields[4]);

        if (icao == null) {
            return null;
        }

        String callsign = emptyToNull(fields[10]);
        Integer altitude = parseInteger(fields[11]);
        Double groundSpeed = parseDouble(fields[12]);
        Double track = parseDouble(fields[13]);
        Double latitude = parseDouble(fields[14]);
        Double longitude = parseDouble(fields[15]);
        Integer verticalRate = parseInteger(fields[16]);
        Boolean onGround = parseBoolean(fields[21]);

        return new AircraftUpdate(icao.toUpperCase(), callsign, altitude, latitude, longitude, groundSpeed, track, verticalRate, onGround, Instant.now());
    }

    private String emptyToNull(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    private Integer parseInteger(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        /*
         * SBS normally represents false as 0
         * and true as -1.
         */
        return !value.equals("0");
    }
}