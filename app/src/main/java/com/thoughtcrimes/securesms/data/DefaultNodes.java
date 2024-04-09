package com.thoughtcrimes.securesms.data;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DefaultNodes {
    // Mainnet
    BELDEX2("publicnode1.rpcnode.stream:29095"),
    BELDEX3 ("publicnode2.rpcnode.stream:29095"),
    BELDEX4 ("publicnode3.rpcnode.stream:29095"),
    BELDEX5("publicnode4.rpcnode.stream:29095"),
    BELDEX6("publicnode5.rpcnode.stream:29095");

    //Testnet
    /*BELDEX("38.242.196.72:19095"),
    BELDEX1("154.26.139.105:19095");*/

    private final String uri;

    DefaultNodes(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
