package com.mrombold.adsbreceiver.gdl90;

import java.util.List;
///////////////////////////////////////////////
public record Gdl90Transmission(String description, List<byte[]> packets){
    public static Gdl90Transmission single(String description,byte[] packet) {
        return new Gdl90Transmission(description, List.of(packet));
    }

    public static Gdl90Transmission bundle(String description, byte[]... packets) {
        return new Gdl90Transmission(description, List.of(packets));
    }
}