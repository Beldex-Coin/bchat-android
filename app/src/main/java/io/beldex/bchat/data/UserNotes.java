package io.beldex.bchat.data;

import io.beldex.bchat.util.Helper;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserNotes {
    public String txNotes = "";
    public String note = "";
    public String bdxtoTag = null;
    public String bdxtoKey = null;
    public String bdxtoAmount = null; // could be a double - but we are not doing any calculations
    public String bdxtoCurrency = null;
    public String bdxtoDestination = null;

    public UserNotes(final String txNotes) {
        if (txNotes == null) {
            return;
        }
        this.txNotes = txNotes;
        Pattern p = Pattern.compile("^\\{([a-z]+)-(\\w{6,}),([0-9.]*)([A-Z]+),(\\w*)\\} ?(.*)");
        Matcher m = p.matcher(txNotes);
        if (m.find()) {
            bdxtoTag = m.group(1);
            bdxtoKey = m.group(2);
            bdxtoAmount = m.group(3);
            bdxtoCurrency = m.group(4);
            bdxtoDestination = m.group(5);
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

    public void setBdxtoOrder(CreateOrder order) {
        if (order != null) {
            bdxtoTag = order.TAG;
            bdxtoKey = order.getOrderId();
            bdxtoAmount = Helper.getDisplayAmount(order.getBtcAmount());
            bdxtoCurrency = order.getBtcCurrency();
            bdxtoDestination = order.getBtcAddress();
        } else {
            bdxtoTag = null;
            bdxtoKey = null;
            bdxtoAmount = null;
            bdxtoDestination = null;
        }
        txNotes = buildTxNote();
    }

    private String buildTxNote() {
        StringBuilder sb = new StringBuilder();
        if (bdxtoKey != null) {
            if ((bdxtoAmount == null) || (bdxtoDestination == null))
                throw new IllegalArgumentException("Broken notes");
            sb.append("{");
            sb.append(bdxtoTag);
            sb.append("-");
            sb.append(bdxtoKey);
            sb.append(",");
            sb.append(bdxtoAmount);
            sb.append(bdxtoCurrency);
            sb.append(",");
            sb.append(bdxtoDestination);
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

        double getBdxAmount();

        String getBdxAddress();

        Date getCreatedAt(); // createdAt

        Date getExpiresAt(); // expiresAt

    }
}
