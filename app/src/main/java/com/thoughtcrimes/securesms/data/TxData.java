package com.thoughtcrimes.securesms.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.thoughtcrimes.securesms.model.PendingTransaction;
import com.thoughtcrimes.securesms.model.Wallet;
import com.thoughtcrimes.securesms.util.Helper;

// https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents
public class TxData implements Parcelable {

    public TxData() {
    }

    public TxData(TxData txData) {
        this.dstAddr = txData.dstAddr;
        this.amount = txData.amount;
        this.mixin = txData.mixin;
        this.priority = txData.priority;
    }

    public TxData(String dstAddr,
                  long amount,
                  int mixin,
                  PendingTransaction.Priority priority) {
        this.dstAddr = dstAddr;
        this.amount = amount;
        this.mixin = mixin;
        this.priority = priority;
    }

    public String getDestinationAddress() {
        return dstAddr;
    }

    public long getAmount() {
        return amount;
    }

    public double getAmountAsDouble() {
        return 1.0 * amount / Helper.ONE_BDX;
    }

    public int getMixin() {
        return mixin;
    }

    public PendingTransaction.Priority getPriority() {
        return priority;
    }

    public void setDestinationAddress(String dstAddr) {
        this.dstAddr = dstAddr;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setAmount(double amount) {
        this.amount = Wallet.getAmountFromDouble(amount);
    }

    public void setMixin(int mixin) {
        this.mixin = mixin;
    }

    public void setPriority(PendingTransaction.Priority priority) {
        this.priority = priority;
    }

    public UserNotes getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(UserNotes userNotes) {
        this.userNotes = userNotes;
    }

    private String dstAddr;
    private long amount;
    private int mixin;
    private PendingTransaction.Priority priority;

    private UserNotes userNotes;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(dstAddr);
        out.writeLong(amount);
        out.writeInt(mixin);
        out.writeInt(priority.getValue());
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<TxData> CREATOR = new Parcelable.Creator<TxData>() {
        public TxData createFromParcel(Parcel in) {
            return new TxData(in);
        }

        public TxData[] newArray(int size) {
            return new TxData[size];
        }
    };

    protected TxData(Parcel in) {
        dstAddr = in.readString();
        amount = in.readLong();
        mixin = in.readInt();
        priority = PendingTransaction.Priority.fromInteger(in.readInt());

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("dstAddr:");
        sb.append(dstAddr);
        sb.append(",amount:");
        sb.append(amount);
        sb.append(",mixin:");
        sb.append(mixin);
        sb.append(",priority:");
        sb.append(priority);
        return sb.toString();
    }
}
