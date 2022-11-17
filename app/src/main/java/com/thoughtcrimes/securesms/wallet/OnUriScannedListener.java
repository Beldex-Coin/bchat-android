
package com.thoughtcrimes.securesms.wallet;


import com.thoughtcrimes.securesms.data.BarcodeData;

public interface OnUriScannedListener {
    boolean onUriScanned(BarcodeData barcodeData);
}
