package com.thoughtcrimes.securesms.data;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DefaultNodes {
    // Mainnet
    BELDEX6("mainnet.beldex.io:29095"),
    BELDEX5("publicnode5.rpcnode.stream:29095"),
    BELDEX1("publicnode1.rpcnode.stream:29095"),
    BELDEX2 ("publicnode2.rpcnode.stream:29095"),
    BELDEX3 ("publicnode3.rpcnode.stream:29095"),
    BELDEX4("publicnode4.rpcnode.stream:29095"),
    BELDEX("explorer.beldex.io:19091");

    //Testnet
   /* BELDEX("38.242.196.72:19095"),
    BELDEX1("154.26.139.105:19095");*/

    @Getter
    private final String uri;

    DefaultNodes(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
