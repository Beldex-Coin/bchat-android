package com.thoughtcrimes.securesms.model;

public class PendingTransaction {
    static {
        System.loadLibrary("app");
    }

    public long handle;

    PendingTransaction(long handle) {
        this.handle = handle;
    }

    public enum Status {
        Status_Ok,
        Status_Error,
        Status_Critical
    }

    public enum Priority {
        Priority_Default(0),
        Priority_Low(1),
        Priority_Medium(2),
        Priority_High(3),
        Priority_Last(4);

        public static Priority fromInteger(int n) {
            switch (n) {
                case 0:
                    return Priority_Default;
                case 1:
                    return Priority_Low;
                case 2:
                    return Priority_Medium;
                case 3:
                    return Priority_High;
            }
            return null;
        }

        public int getValue() {
            return value;
        }

        private int value;

        Priority(int value) {
            this.value = value;
        }


    }

    public Status getStatus() {
        return Status.values()[getStatusJ()];
    }

    public native int getStatusJ();

    public native String getErrorString();

    // commit transaction or save to file if filename is provided.
    public native boolean commit(String filename, boolean overwrite);

    public native long getAmount();

    public native long getDust();

    public native long getFee();

    public String getFirstTxId() {
        String id = getFirstTxIdJ();
        if (id == null)
            throw new IndexOutOfBoundsException();
        return id;
    }

    public native String getFirstTxIdJ();

    public native long getTxCount();

}
