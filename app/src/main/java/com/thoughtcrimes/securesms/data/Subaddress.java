package com.thoughtcrimes.securesms.data;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class Subaddress implements Comparable<Subaddress> {
    @Getter
    final private int accountIndex;
    @Getter
    final private int addressIndex;
    @Getter
    final private String address;
    @Getter
    private final String label;
    @Getter
    @Setter
    private long amount;

    public Subaddress(int accountIndex, int subAddressIndex, String subaddress, String subaddressLabel) {
        this.accountIndex=accountIndex;
        this.addressIndex=subAddressIndex;
        this.address=subaddress;
        this.label=subaddressLabel;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public int compareTo(Subaddress another) { // newer is <
        final int compareAccountIndex = another.accountIndex - accountIndex;
        if (compareAccountIndex == 0)
            return another.addressIndex - addressIndex;
        return compareAccountIndex;
    }

    public String getSquashedAddress() {
        return address.substring(0, 8) + "…" + address.substring(address.length() - 8);
    }

    public static final Pattern DEFAULT_LABEL_FORMATTER = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}:[0-9]{2}:[0-9]{2}$");

    public String getDisplayLabel() {
        if (label.isEmpty() || (DEFAULT_LABEL_FORMATTER.matcher(label).matches()))
            return ("#" + addressIndex);
        else
            return label;
    }
}
