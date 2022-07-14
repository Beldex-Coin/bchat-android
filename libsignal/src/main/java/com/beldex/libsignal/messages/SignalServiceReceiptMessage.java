package com.beldex.libsignal.messages;

import java.util.List;

public class SignalServiceReceiptMessage {

  public enum Type {
    UNKNOWN, DELIVERY, READ
  }

  private final Type       type;
  private final List<Long> timestamps;
  private final long       when;

  public SignalServiceReceiptMessage(Type type, List<Long> timestamps, long when) {
    this.type       = type;
    this.timestamps = timestamps;
    this.when       = when;
  }

  public Type getType() {
    return type;
  }

  public List<Long> getTimestamps() {
    return timestamps;
  }

  public long getWhen() {
    return when;
  }

  public boolean isDeliveryReceipt() {
    return type == Type.DELIVERY;
  }

  public boolean isReadReceipt() {
    return type == Type.READ;
  }

  public int getTTL() { return 0; }
}
