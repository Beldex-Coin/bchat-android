
package com.thoughtcrimes.securesms.wallet.scan;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import io.beldex.bchat.R;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import timber.log.Timber;

public class ScannerFragment extends Fragment implements ZXingScannerView.ResultHandler {

    private OnScannedListener onScannedListener;

    public interface OnScannedListener {
        boolean onScanned(String qrCode);
    }

    private ZXingScannerView mScannerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");
        mScannerView = new ZXingScannerView(getActivity());
        return mScannerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        if ((rawResult.getBarcodeFormat() == BarcodeFormat.QR_CODE)) {
            if (onScannedListener.onScanned(rawResult.getText())) {
                return;
            } else {
                Toast.makeText(getActivity(), getString(R.string.send_qr_address_invalid), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.send_qr_invalid), Toast.LENGTH_SHORT).show();
        }

        // Note from dm77:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler handler = new Handler();
        handler.postDelayed(() -> mScannerView.resumeCameraPreview(ScannerFragment.this), 2000);
    }

    @Override
    public void onPause() {
        Timber.d("onPause");
        mScannerView.stopCamera();
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnScannedListener) {
            this.onScannedListener = (OnScannedListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }
}
