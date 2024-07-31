package io.beldex.bchat.wallet.service.exchange;

public class ExchangeException extends Exception {
    private final int code;
    private final String errorMsg;

    public String getErrorMsg() {
        return errorMsg;
    }

    public ExchangeException(final int code) {
        super();
        this.code = code;
        this.errorMsg = null;
    }

    public ExchangeException(final String errorMsg) {
        super();
        this.code = 0;
        this.errorMsg = errorMsg;
    }

    public ExchangeException(final int code, final String errorMsg) {
        super();
        this.code = code;
        this.errorMsg = errorMsg;
    }

    public int getCode() {
        return code;
    }
}
