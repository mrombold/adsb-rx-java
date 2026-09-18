package com.mrombold.adsbreceiver.gdl90;

import com.mrombold.adsbreceiver.ahrs.AhrsData;
import com.mrombold.adsbreceiver.model.AircraftSnapshot;
import com.mrombold.adsbreceiver.model.OwnshipPosition;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
//////////////////////////////////////////////////////////
public class Gdl90Encoder {

    private static final double LAT_LON_RESOLUTION = 180.0 / 8_388_608.0;
    private static final double TRACK_RESOLUTION = 360.0 / 256.0;

    public byte[] heartbeat(boolean gpsValid) {
        byte[] msg = new byte[7];
        msg[0] = 0x00;
        /*
         * UAT initialized.
         */
        msg[1] = 0x01;

        if (gpsValid) {
            msg[1] |= (byte) 0x80;
        }

        int seconds = LocalTime.now(ZoneOffset.UTC).toSecondOfDay();
        /*
         * UTC valid.
         */
        msg[2] = (byte) (((seconds >> 16) << 7) | 0x01);
        msg[3] = (byte) (seconds & 0xFF);
        msg[4] = (byte) ((seconds >> 8) & 0xFF);

        return Gdl90Framing.frame(msg);
    }

    public byte[] stratuxHeartbeat(
            boolean gpsValid,
            boolean ahrsValid) {

        byte[] msg = new byte[2];

        /*
         * Stratux heartbeat message ID.
         */
        msg[0] = (byte) 0xCC;
        msg[1] = 0;

        /*
         * Bit 1 = GPS available.
         */
        if (gpsValid) {
            msg[1] |= 0x02;
        }

        /*
         * Bit 0 = AHRS available.
         */
        if (ahrsValid) {
            msg[1] |= 0x01;
        }

        /*
         * Protocol version 1.
         *
         * Version occupies bits 2+.
         */
        msg[1] |= (1 << 2);

        return Gdl90Framing.frame(msg);
    }

    public byte[] stratuxStatus(
            boolean gpsValid,
            boolean ahrsValid,
            int satellitesVisible,
            int satellitesUsed) {

        byte[] msg = new byte[29];

        /*
         * "SX" Stratux status identifier.
         *
         * 0x53 = 'S'
         * 0x58 = 'X'
         */
        msg[0] = 'S';
        msg[1] = 'X';

        /*
         * Same values currently used
         * by the Go implementation.
         */
        msg[2] = 1;
        msg[3] = 1;

        /*
         * Placeholder software version.
         *
         * The Go program currently sends:
         *
         * 9, 9, 9, 9
         */
        msg[4] = 9;
        msg[5] = 9;
        msg[6] = 9;
        msg[7] = 9;

        /*
         * Hardware revision unknown.
         */
        msg[8]  = (byte) 0xFF;
        msg[9]  = (byte) 0xFF;
        msg[10] = (byte) 0xFF;
        msg[11] = (byte) 0xFF;

        /*
         * Status flags copied from
         * the current Go implementation.
         */
        if (ahrsValid) {
            msg[12] |= 0x01;
            msg[13] |= (1 << 2);
        }

        if (gpsValid) {
            msg[13] |= (1 << 7);
        }

        /*
         * GPS satellite counts.
         */
        msg[16] = (byte) Math.min(255, Math.max(0, satellitesVisible));
        msg[17] = (byte) Math.min(255, Math.max(0, satellitesUsed));

        /*
         * Other fields remain zero
         * until we actually collect:
         *
         * traffic source counts
         * message rates
         * CPU temperature
         * tower count
         * etc.
         */
        return Gdl90Framing.frame(msg);
    }


    public byte[] ownship(OwnshipPosition position) {
        Boolean onGround = null;

        if (position.groundSpeedKnots() != null) {
            onGround = position.groundSpeedKnots() < 5.0;
        }

        /*
         * IMPORTANT:
         *
         * GDL90 Ownship Report altitude is
         * PRESSURE altitude.
         *
         * We currently only have GPS altitude,
         * so send altitude unavailable here.
         *
         * Geometric altitude is sent separately.
         */

        return encodeReport(
                0x0A,
                0xF00000,
                position.latitude(),
                position.longitude(),
                null,
                position.groundSpeedKnots(),
                position.verticalSpeedFpm() != null ? (int) Math.round(position.verticalSpeedFpm()) : null,
                position.trackDegrees(),
                onGround,
                "JAVAADS",
                0x80
        );
    }

    public byte[] ownshipGeometricAltitude(OwnshipPosition position) {

        byte[] msg = new byte[5];
        msg[0] = 0x0B;
        Double altitudeMeters = position.altitudeHaeMeters();
        if (altitudeMeters == null) {
            altitudeMeters = position.altitudeMslMeters();
        }

        int altitudeFeet = 0;
        if (altitudeMeters != null) {
            altitudeFeet = (int) Math.round(altitudeMeters * 3.28084);
        }

        short encoded = (short) Math.round(altitudeFeet / 5.0);

        msg[1] = (byte) ((encoded >> 8) & 0xFF);

        msg[2] = (byte) (encoded & 0xFF);

        /*
         * Vertical figure of merit unavailable.
         */
        msg[3] = 0x7F;
        msg[4] = (byte) 0xFF;

        return Gdl90Framing.frame(msg);
    }

    public byte[] traffic(AircraftSnapshot aircraft) {
        int address;
        try { address = Integer.parseInt(aircraft.icao(),16);
        } catch (NumberFormatException e) {
            address = 0;
        }

        return encodeReport(
                0x14,
                address,
                aircraft.latitude(),
                aircraft.longitude(),
                aircraft.altitudeFeet(),
                aircraft.groundSpeedKnots(),
                aircraft.verticalRateFpm(),
                aircraft.trackDegrees(),
                aircraft.onGround(),
                aircraft.callsign(),
                0x00
        );
    }

    public byte[] uplink(byte[] frame) {
        if (frame.length != 432) {
            throw new IllegalArgumentException("UAT uplink must be 432 bytes");
        }
        byte[] msg = new byte[436];
        msg[0] = 0x07;
        /*
         * Bytes 1-3 are time of reception.
         * Leave zero for now.
         */
        System.arraycopy(frame,0, msg, 4, 432);
        return Gdl90Framing.frame(msg);
    }

    /*
     * ForeFlight-style AHRS message.
     *
     * We will leave the AHRS transmitter
     * disabled initially.
     */
    public byte[] ahrs(AhrsData data) {
        byte[] msg = new byte[12];

        msg[0] = 0x65;
        msg[1] = 0x01;

        short roll = (short) Math.round(data.rollDegrees() * 10);
        short pitch = (short) Math.round(data.pitchDegrees() * 10);
        int heading = (int) Math.round(data.headingDegrees() * 10);

        putShort(msg,2, roll);
        putShort(msg, 4, pitch);
        putShort(msg, 6, (short) heading);

        /*
         * IAS/TAS unavailable.
         */
        msg[8] = (byte) 0xFF;
        msg[9] = (byte) 0xFF;
        msg[10] = (byte) 0xFF;
        msg[11] = (byte) 0xFF;

        return Gdl90Framing.frame(msg);
    }

    private byte[] encodeReport(
            int messageId,
            int address,
            Double latitude,
            Double longitude,
            Integer altitudeFeet,
            Double speedKnots,
            Integer verticalRateFpm,
            Double trackDegrees,
            Boolean onGround,
            String callsign,
            int nicNacp) {

        byte[] msg = new byte[28];
        msg[0] = (byte) messageId;

        /*
         * Address type 0:
         * ADS-B ICAO address.
         */
        msg[1] = 0;
        msg[2] = (byte) ((address >> 16) & 0xFF);
        msg[3] = (byte) ((address >> 8) & 0xFF);
        msg[4] = (byte) (address & 0xFF);

        putLatLon(msg,5, latitude != null ? latitude : 0.0);
        putLatLon(msg, 8, longitude != null ? longitude : 0.0);

        int altitude = encodeAltitude(altitudeFeet);

        msg[11] = (byte) ((altitude >> 4) & 0xFF );

        int misc = 0;

        if (trackDegrees != null) {
            /*
             * True track valid.
             */
            misc |= 0x01;
        }

        /*
         * If explicitly on ground, leave
         * airborne bit clear.
         *
         * Otherwise assume airborne.
         */
        if (!Boolean.TRUE.equals(onGround)) {
            misc |= 0x08;
        }

        msg[12] = (byte) (((altitude & 0x0F) << 4) | misc);
        msg[13] = (byte) nicNacp;

        int speed = encodeSpeed(speedKnots);
        int vertical = encodeVerticalVelocity(verticalRateFpm);

        msg[14] = (byte) ((speed >> 4) & 0xFF);
        msg[15] = (byte) (((speed & 0x0F) << 4) | ((vertical >> 8) & 0x0F));
        msg[16] = (byte) (vertical & 0xFF);
        msg[17] = encodeTrack(trackDegrees);
        /*
         * Emitter category unknown.
         */
        msg[18] = 0;

        Arrays.fill(msg,19,27,(byte) ' ');

        if (callsign != null) {
            String clean = callsign.trim().toUpperCase();
            int length = Math.min(8, clean.length());

            for (int i = 0; i < length; i++) {
                char character = clean.charAt(i);
                if (character >= 32 && character <= 126) {
                    msg[19 + i] = (byte) character;
                }
            }
        }

        /*
         * Priority / emergency = none.
         */
        msg[27] = 0;
        return Gdl90Framing.frame(msg);
    }

    private void putLatLon(byte[] msg, int offset, double degrees) {
        int encoded = (int) (degrees / LAT_LON_RESOLUTION);

        msg[offset] = (byte) ((encoded >> 16) & 0xFF);
        msg[offset + 1] = (byte) ((encoded >> 8) & 0xFF);
        msg[offset + 2] = (byte) (encoded & 0xFF);
    }

    private int encodeAltitude(Integer feet) {
        if (feet == null || feet < -1000 || feet > 101350) {
            return 0xFFF;
        }
        return ((feet / 25) + 40) & 0xFFF;
    }

    private int encodeSpeed(Double knots) {

        if (knots == null ||
                knots < 0 ||
                knots > 4094) {

            return 0xFFF;
        }

        return (int) Math.round(knots);
    }

    private int encodeVerticalVelocity(Integer fpm) {
        if (fpm == null) {
            /*
             * GDL90 unavailable value.
             */
            return 0x800;
        }

        int value = Math.round(fpm / 64.0f);

        value = Math.max(-2047, Math.min(2047, value));
        return value & 0xFFF;
    }

    private byte encodeTrack(Double trackDegrees) {
        if (trackDegrees == null) {
            return 0;
        }

        double track = trackDegrees;

        while (track < 0) {
            track += 360;
        }

        while (track >= 360) {
            track -= 360;
        }

        int value =(int) Math.round(track /TRACK_RESOLUTION);
        return (byte) (value & 0xFF);
    }

    private void putShort(byte[] target, int offset, short value) {
        target[offset] = (byte) ((value >> 8) & 0xFF);
        target[offset + 1] = (byte) (value & 0xFF);
    }
}