
package io.beldex.bchat.wallet;


import io.beldex.bchat.data.BarcodeData;

public interface OnUriScannedListener {
    boolean onUriScanned(BarcodeData barcodeData);
}
