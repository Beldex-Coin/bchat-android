package com.beldex.libsignal.bchatkeys;




public class bchats {

    public static native int ed25519pktocurve25519(
            byte[] curve25519SecretKey,
            byte[] ed25519SecretKey
    );

    public static native int ed25519sktocurve25519(
            byte[] curve25519SecretKey,
            byte[] ed25519SecretKey
    );
}
