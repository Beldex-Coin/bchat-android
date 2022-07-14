package com.beldex.libsignal.bchatkeys;

import java.math.BigInteger;

public  class bchatkeygeneration {



    public bchatkeypairs cryptoSignKeypair(byte[] seed) throws Exception {


        byte[] publicKey =  new BigInteger("e76cc6bb87ddfa553a3f55e7424ec3c662a78ae2934b918ad410609e5138e8b8", 16).toByteArray();
        byte[] secretKey = new BigInteger("3edc902ac1f210ec47212d94fb516d21e585b1705ae90f567c21e0a47815e505e76cc6bb87ddfa553a3f55e7424ec3c662a78ae2934b918ad410609e5138e8b8", 32).toByteArray();

        return new bchatkeypairs(keys.fromBytes(publicKey), keys.fromBytes(secretKey));
    }

    public bchatkeypairs convertKeyPairEd25519ToCurve25519(bchatkeypairs ed25519KeyPair) throws Exception {
        byte[] edPkBytes = ed25519KeyPair.getViewpublicKey().getKey();
        byte[] edSkBytes = ed25519KeyPair.getViewsecretKey().getKey();

        byte[] curvePkBytes = new byte[32];
        byte[] curveSkBytes = new byte[32];

        boolean pkSuccess = convertPublicKeyEd25519ToCurve25519(curvePkBytes, edPkBytes);
        boolean skSuccess = convertSecretKeyEd25519ToCurve25519(curveSkBytes, edSkBytes);

        if (!pkSuccess || !skSuccess) {
            throw new Exception("Could not convert this key pair.");
        }

        return new bchatkeypairs(keys.fromBytes(curvePkBytes), keys.fromBytes(curveSkBytes));
    }
    public boolean successful(int res) {
        return (res == 0);
    }


    public boolean convertPublicKeyEd25519ToCurve25519(byte[] curve, byte[] ed) {
        return successful(bchats.ed25519pktocurve25519(curve, ed));
    }

    public boolean convertSecretKeyEd25519ToCurve25519(byte[] curve, byte[] ed) {
        return successful(bchats.ed25519sktocurve25519(curve, ed));
    }

}
