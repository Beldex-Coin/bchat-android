package io.beldex.bchat.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import lombok.Getter;
import lombok.Setter;

public class TxDataBtc extends TxData {
    @Getter
    @Setter
    private String btcSymbol; // the actual non-BDX thing we're sending
    @Getter
    @Setter
    private String bdxtoOrderId; // shown in success screen
    @Getter
    @Setter
    private String btcAddress;
    @Getter
    @Setter
    private double btcAmount;

    public TxDataBtc() {
        super();
    }

    public TxDataBtc(TxDataBtc txDataBtc) {
        super(txDataBtc);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(btcSymbol);
        out.writeString(bdxtoOrderId);
        out.writeString(btcAddress);
        out.writeDouble(btcAmount);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<TxDataBtc> CREATOR = new Creator<TxDataBtc>() {
        public TxDataBtc createFromParcel(Parcel in) {
            return new TxDataBtc(in);
        }

        public TxDataBtc[] newArray(int size) {
            return new TxDataBtc[size];
        }
    };

    protected TxDataBtc(Parcel in) {
        super(in);
        btcSymbol = in.readString();
        bdxtoOrderId = in.readString();
        btcAddress = in.readString();
        btcAmount = in.readDouble();
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("bdxtoOrderId:");
        sb.append(bdxtoOrderId);
        sb.append(",btcSymbol:");
        sb.append(btcSymbol);
        sb.append(",btcAddress:");
        sb.append(btcAddress);
        sb.append(",btcAmount:");
        sb.append(btcAmount);
        return sb.toString();
    }

    public boolean validateAddress(@NonNull String address) {
        if ((btcSymbol == null) || (btcAddress == null)) return false;
        final Crypto crypto = Crypto.withSymbol(btcSymbol);
        if (crypto == null) return false;
        if (crypto.isCasefull()) { // compare as-is
            return address.equals(btcAddress);
        } else { // normalize & compare (e.g. ETH with and without checksum capitals
            return address.toLowerCase().equals(btcAddress.toLowerCase());
        }
    }
}
