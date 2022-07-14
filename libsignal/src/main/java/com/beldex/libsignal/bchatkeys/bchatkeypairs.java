package com.beldex.libsignal.bchatkeys;

public class bchatkeypairs {

    private keys viewsecretKey;
    private keys viewpublicKey;

    public bchatkeypairs(keys publicKey, keys secretKey) {
        this.viewpublicKey = publicKey;
        this.viewsecretKey = secretKey;
    }

    public keys getViewsecretKey() {
        return viewsecretKey;
    }

    public keys getViewpublicKey() {
        return viewpublicKey;
    }

}
