package com.thoughtcrimes.securesms.data;

import com.thoughtcrimes.securesms.util.Helper;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserNotes {
    public String txNotes = "";
    public String note = "";
    public String xmrtoTag = null;
    public String xmrtoKey = null;
    public String xmrtoAmount = null; // could be a double - but we are not doing any calculations
    public String xmrtoCurrency = null;
    public String xmrtoDestination = null;

    public UserNotes(final String txNotes) {
        if (txNotes == null) {
            return;
        }
        this.txNotes = txNotes;
        Pattern p = Pattern.compile("^\\{([a-z]+)-(\\w{6,}),([0-9.]*)([A-Z]+),(\\w*)\\} ?(.*)");
        Matcher m = p.matcher(txNotes);
        if (m.find()) {
            xmrtoTag = m.group(1);
            xmrtoKey = m.group(2);
            xmrtoAmount = m.group(3);
            xmrtoCurrency = m.group(4);
            xmrtoDestination = m.group(5);
            note = m.group(6);
        } else {
            note = txNotes;
        }
    }

    public void setNote(String newNote) {
        if (newNote != null) {
            note = newNote;
        } else {
            note = "";
        }
        txNotes = buildTxNote();
    }

    public void setXmrtoOrder(CreateOrder order) {
        if (order != null) {
            xmrtoTag = order.TAG;
            xmrtoKey = order.getOrderId();
            xmrtoAmount = Helper.getDisplayAmount(order.getBtcAmount());
            xmrtoCurrency = order.getBtcCurrency();
            xmrtoDestination = order.getBtcAddress();
        } else {
            xmrtoTag = null;
            xmrtoKey = null;
            xmrtoAmount = null;
            xmrtoDestination = null;
        }
        txNotes = buildTxNote();
    }

    private String buildTxNote() {
        StringBuilder sb = new StringBuilder();
        if (xmrtoKey != null) {
            if ((xmrtoAmount == null) || (xmrtoDestination == null))
                throw new IllegalArgumentException("Broken notes");
            sb.append("{");
            sb.append(xmrtoTag);
            sb.append("-");
            sb.append(xmrtoKey);
            sb.append(",");
            sb.append(xmrtoAmount);
            sb.append(xmrtoCurrency);
            sb.append(",");
            sb.append(xmrtoDestination);
            sb.append("}");
            if ((note != null) && (!note.isEmpty()))
                sb.append(" ");
        }
        sb.append(note);
        return sb.toString();
    }
    public interface CreateOrder {
        String TAG = "side";

        String getBtcCurrency();

        double getBtcAmount();

        String getBtcAddress();

        String getQuoteId();

        String getOrderId();

        double getXmrAmount();

        String getXmrAddress();

        Date getCreatedAt(); // createdAt

        Date getExpiresAt(); // expiresAt

    }
}
