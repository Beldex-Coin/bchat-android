package io.beldex.bchat.model;

import android.os.Parcel;
import android.os.Parcelable;

import io.beldex.bchat.data.Subaddress;
import io.beldex.bchat.data.Subaddress;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// this is not the TransactionInfo from the API as that is owned by the TransactionHistory
// this is a POJO for the TransactionInfoAdapter
public class TransactionInfo implements Parcelable, Comparable<TransactionInfo> {
    public static final int CONFIRMATION = 10; // blocks

    @RequiredArgsConstructor
    public enum Direction {
        Direction_In(0),
        Direction_Out(1);

        Direction(int i) {
            this.value=i;
        }

        public static Direction fromInteger(int n) {
            switch (n) {
                case 0:
                    return Direction_In;
                case 1:
                    return Direction_Out;
            }
            return null;
        }

        @Getter
        private final int value;
    }

    public Direction direction;
    public boolean isPending;
    public boolean isFailed;
    public long amount;
    public long fee;
    public long blockheight;
    public String hash;
    public long timestamp;
    public String paymentId;
    public int accountIndex;
    public int addressIndex;
    public long confirmations;
    public String subaddressLabel;
    public List<Transfer> transfers;

    public String txKey = null;
    public String notes = null;
    public String address = null;
    public boolean isBns;

    public TransactionInfo(
            int direction,
            boolean isPending,
            boolean isFailed,
            long amount,
            long fee,
            long blockheight,
            String hash,
            long timestamp,
            String paymentId,
            int accountIndex,
            int addressIndex,
            long confirmations,
            String subaddressLabel,
            List<Transfer> transfers,
            boolean isBns) {
        this.direction = Direction.values()[direction];
        this.isPending = isPending;
        this.isFailed = isFailed;
        this.amount = amount;
        this.fee = fee;
        this.blockheight = blockheight;
        this.hash = hash;
        this.timestamp = timestamp;
        this.paymentId = paymentId;
        this.accountIndex = accountIndex;
        this.addressIndex = addressIndex;
        this.confirmations = confirmations;
        this.subaddressLabel = subaddressLabel;
        this.transfers = transfers;
        this.isBns = isBns;
    }

    public boolean isConfirmed() {
        return confirmations >= CONFIRMATION;
    }

    public String getDisplayLabel() {
        if (subaddressLabel.isEmpty() || (Subaddress.DEFAULT_LABEL_FORMATTER.matcher(subaddressLabel).matches()))
            return ("#" + addressIndex);
        else
            return subaddressLabel;
    }

    public String toString() {
        return direction + "@" + blockheight + " " + amount;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(direction.value);
        out.writeByte((byte) (isPending ? 1 : 0));
        out.writeByte((byte) (isFailed ? 1 : 0));
        out.writeLong(amount);
        out.writeLong(fee);
        out.writeLong(blockheight);
        out.writeString(hash);
        out.writeLong(timestamp);
        out.writeString(paymentId);
        out.writeInt(accountIndex);
        out.writeInt(addressIndex);
        out.writeLong(confirmations);
        out.writeString(subaddressLabel);
        out.writeList(transfers);
        out.writeString(txKey);
        out.writeString(notes);
        out.writeString(address);
        out.writeByte((byte) (isBns ? 1 : 0));
    }

    public static final Parcelable.Creator<TransactionInfo> CREATOR = new Parcelable.Creator<TransactionInfo>() {
        public TransactionInfo createFromParcel(Parcel in) {
            return new TransactionInfo(in);
        }

        public TransactionInfo[] newArray(int size) {
            return new TransactionInfo[size];
        }
    };

    private TransactionInfo(Parcel in) {
        direction = Direction.fromInteger(in.readInt());
        isPending = in.readByte() != 0;
        isFailed = in.readByte() != 0;
        amount = in.readLong();
        fee = in.readLong();
        blockheight = in.readLong();
        hash = in.readString();
        timestamp = in.readLong();
        paymentId = in.readString();
        accountIndex = in.readInt();
        addressIndex = in.readInt();
        confirmations = in.readLong();
        subaddressLabel = in.readString();
        transfers = in.readArrayList(Transfer.class.getClassLoader());
        txKey = in.readString();
        notes = in.readString();
        address = in.readString();
        isBns = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int compareTo(TransactionInfo another) {
        long b1 = this.timestamp;
        long b2 = another.timestamp;
        if (b1 > b2) {
            return -1;
        } else if (b1 < b2) {
            return 1;
        } else {
            return this.hash.compareTo(another.hash);
        }
    }
}