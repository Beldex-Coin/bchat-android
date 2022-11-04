package com.thoughtcrimes.securesms.model;

import android.util.Log;

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
        Priority_Slow(1),
        Priority_Medium(2),
        Priority_High(3),
        Priority_Last(4),
        Priority_Flash(5);

        public static Priority fromInteger(int n) {
            switch (n) {
                case 0:
                    return Priority_Default;
                case 1:
                    return Priority_Slow;
                case 2:
                    return Priority_Medium;
                case 3:
                    return Priority_High;
                case 4:
                    return Priority_Last;
                case 5:
                    return Priority_Flash;
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

   /* public Status getStatus() {
        return Status.values()[getStatusJ()];
    }*/
   public Status getStatus() {
       int status = getStatusJ();
       if (status == 0) return Status.Status_Ok;
       if (status == 1) return Status.Status_Error;
       return Status.Status_Critical;
   }

    public native int getStatusJ();

    public String getErrorString() {
        Log.d("PendingTransaction before getErrorStringJ","");
        String error = getErrorStringJ();
        Log.d("PendingTransaction getErrorString:" , error);
        return error;
    }

    public native String getErrorStringJ();

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
