package io.beldex.bchat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Transfer implements Parcelable {
    public long amount;
    public String address;

    public Transfer(long amount, String address) {
        this.amount = amount;
        this.address = address;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(amount);
        out.writeString(address);
    }

    public static final Parcelable.Creator<Transfer> CREATOR = new Parcelable.Creator<Transfer>() {
        public Transfer createFromParcel(Parcel in) {
            return new Transfer(in);
        }

        public Transfer[] newArray(int size) {
            return new Transfer[size];
        }
    };

    private Transfer(Parcel in) {
        amount = in.readLong();
        address = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
