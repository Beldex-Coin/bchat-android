package com.beldex.libsignal.bchatkeys;

public class keys {
    private byte[] key;

    private keys(byte[] key) {
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }


    public static keys fromBytes(byte[] bytes) {
        return new keys(bytes);
    }
}

