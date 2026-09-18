package com.mrombold.adsbreceiver.gdl90;

import java.io.ByteArrayOutputStream;
//////////////////////////////////////////////////////////
public final class Gdl90Framing {

    private static final int FLAG = 0x7E;
    private static final int ESCAPE = 0x7D;
    private static final int[] CRC_TABLE = buildCrcTable();

    private Gdl90Framing() {
    }

    public static byte[] frame(byte[] message) {
        int crc = crc16(message);
        ByteArrayOutputStream output = new ByteArrayOutputStream(message.length + 8);
        output.write(FLAG);
        for (byte value : message) {
            writeEscaped(output,value & 0xFF);
        }
        /*
         * GDL90 CRC is sent LSB first.
         */
        writeEscaped(output,crc & 0xFF);
        writeEscaped(output,(crc >> 8) & 0xFF);
        output.write(FLAG);
        return output.toByteArray();
    }

    private static void writeEscaped(ByteArrayOutputStream output, int value) {
        if (value == FLAG || value == ESCAPE) {
            output.write(ESCAPE);
            output.write(value ^ 0x20);
        } else {
            output.write(value);
        }
    }

    private static int crc16(byte[] data) {
        int crc = 0;
        for (byte value : data) {
            crc = CRC_TABLE[(crc >> 8) & 0xFF] ^ ((crc << 8) & 0xFFFF) ^ (value & 0xFF);
            crc &= 0xFFFF;
        }
        return crc;
    }

    private static int[] buildCrcTable() {
        int[] table = new int[256];
        for (int i = 0; i < 256; i++) {
            int crc = i << 8;
            for (int bit = 0; bit < 8; bit++) {
                int polynomial = (crc & 0x8000) != 0 ? 0x1021 : 0;
                crc = ((crc << 1) ^ polynomial) & 0xFFFF;
            }
            table[i] = crc;
        }
        return table;
    }
}