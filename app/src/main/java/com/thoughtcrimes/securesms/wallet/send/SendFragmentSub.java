package com.thoughtcrimes.securesms.wallet.send;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputLayout;
import com.thoughtcrimes.securesms.data.BarcodeData;
import com.thoughtcrimes.securesms.data.Crypto;
import com.thoughtcrimes.securesms.data.TxData;
import com.thoughtcrimes.securesms.data.UserNotes;
import com.thoughtcrimes.securesms.model.PendingTransaction;
import com.thoughtcrimes.securesms.util.Helper;
import com.thoughtcrimes.securesms.util.OpenAliasHelper;
import com.thoughtcrimes.securesms.util.ServiceHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.beldex.bchat.R;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SendFragmentSub#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendFragmentSub extends SendWizardFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private EditText beldexAddressField;
    private ImageView scanQRCode;
    private TextInputLayout beldexAddressTxtLayout;
    private boolean resolvingOA = false;
    final private Set<Crypto> possibleCryptos = new HashSet<>();
    private Crypto selectedCrypto = null;
    final static public int MIXIN = 0;

    public static SendFragmentSub newInstance(Listener listener) {
        SendFragmentSub instance = new SendFragmentSub();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public void setSendListener(Listener listener) {
        this.sendListener = listener;
    }

    OnScanListener onScanListener;

    public interface OnScanListener {
        void onScan();
    }

    public interface Listener {
        void setBarcodeData(BarcodeData data);

        BarcodeData getBarcodeData();

        BarcodeData popBarcodeData();

        // by hales
        /* void setMode(SendFragment.Mode mode);*/

        TxData getTxData();
    }



    // TODO: Rename and change types and number of parameters
    public static SendFragmentSub newInstance(String param1, String param2) {
        SendFragmentSub fragment = new SendFragmentSub();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_send_new, container, false);

        scanQRCode = view.findViewById(R.id.scanQrCode);
        beldexAddressField = view.findViewById(R.id.beldexAddressEditTxt);
        beldexAddressTxtLayout = view.findViewById(R.id.beldexAddressEditTxtLayout);
        scanQRCode.setOnClickListener(view1 -> {
            onScanListener.onScan();
        });
        beldexAddressTxtLayout.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String enteredAddress = beldexAddressTxtLayout.getEditText().getText().toString().trim();
                String dnsOA = dnsFromOpenAlias(enteredAddress);
                Timber.d("OpenAlias is %s", dnsOA);
                if (dnsOA != null) {
                    processOpenAlias(dnsOA);
                }
            }
        });
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScanListener) {
            onScanListener = (OnScanListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ScanListener");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        //processScannedData();
    }

    public void processScannedData(BarcodeData barcodeData) {
        Log.d("Beldex","ProcessScannedData called");
        sendListener.setBarcodeData(barcodeData);
        if (isResumed())
            processScannedData();
    }
    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
       /* etDummy.requestFocus();*/
    }

    public void processScannedData() {
        BarcodeData barcodeData = sendListener.getBarcodeData();
        if (barcodeData != null) {
            Timber.d("GOT DATA");
            // by hales
              if (!Helper.ALLOW_SHIFT && (barcodeData.asset != Crypto.XMR)) {
            if (!Helper.ALLOW_SHIFT) {
                Timber.d("BUT ONLY XMR SUPPORTED");
                barcodeData = null;
                sendListener.setBarcodeData(barcodeData);
            }
            if (barcodeData.address != null) {
                beldexAddressTxtLayout.getEditText().setText(barcodeData.address);
                // by hales
                possibleCryptos.clear();
                selectedCrypto = null;
                if (barcodeData.isAmbiguous()) {
                    possibleCryptos.addAll(barcodeData.ambiguousAssets);
                } else {
                    possibleCryptos.add(barcodeData.asset);
                    selectedCrypto = barcodeData.asset;
                }
                // by hales
               /* if (Helper.ALLOW_SHIFT)
                    updateCryptoButtons(false);*/

                /*if (checkAddress()) {
                    if (barcodeData.security == BarcodeData.Security.OA_NO_DNSSEC)
                        beldexAddressTxtLayout.setError(getString(R.string.send_address_no_dnssec));
                    else if (barcodeData.security == BarcodeData.Security.OA_DNSSEC)
                        beldexAddressTxtLayout.setError(getString(R.string.send_address_openalias));
                }*/

            } else {
                beldexAddressTxtLayout.getEditText().getText().clear();
                beldexAddressTxtLayout.setError(null);
            }

            // by hales
            String scannedNotes = barcodeData.addressName;
            if (scannedNotes == null) {
                scannedNotes = barcodeData.description;
            } else if (barcodeData.description != null) {
                scannedNotes = scannedNotes + ": " + barcodeData.description;
            }
            if (scannedNotes != null) {
                //by hales
            }
                /*etNotes.getEditText().setText(scannedNotes);*/
            } else {
                /*etNotes.getEditText().getText().clear();
                etNotes.setError(null);*/
            }
        } else
            Timber.d("barcodeData=null");
    }
    private boolean checkAddressNoError() {
        return selectedCrypto != null;
    }
    @Override
    public boolean onValidateFields() {
        if (!checkAddressNoError()) {
            // by hales
            /*shakeAddress();*/
            String enteredAddress = beldexAddressTxtLayout.getEditText().getText().toString().trim();
            String dnsOA = dnsFromOpenAlias(enteredAddress);
            Timber.d("OpenAlias is %s", dnsOA);
            if (dnsOA != null) {
                processOpenAlias(dnsOA);
            }
            return false;
        }

        if (sendListener != null) {
            TxData txData = sendListener.getTxData();
            // by hales
          /*  if (txData instanceof TxDataBtc) {
                ((TxDataBtc) txData).setBtcAddress(beldexAddressTxtLayout.getEditText().getText().toString());
                ((TxDataBtc) txData).setBtcSymbol(selectedCrypto.getSymbol());
                txData.setDestinationAddress(null);
                ServiceHelper.ASSET = selectedCrypto.getSymbol().toLowerCase();
            } else {*/
                txData.setDestinationAddress(beldexAddressTxtLayout.getEditText().getText().toString());
                ServiceHelper.ASSET = null;
            //}

            txData.setPriority(PendingTransaction.Priority.Priority_Default);
            txData.setMixin(SendFragmentSub.MIXIN);
        }
        return true;
    }

    private void processOpenAlias(String dnsOA) {
        if (resolvingOA) return; // already resolving - just wait
        sendListener.popBarcodeData();
        if (dnsOA != null) {
            resolvingOA = true;
            beldexAddressTxtLayout.setError(getString(R.string.send_address_resolve_openalias));
            OpenAliasHelper.resolve(dnsOA, new OpenAliasHelper.OnResolvedListener() {
                @Override
                public void onResolved(Map<Crypto, BarcodeData> dataMap) {
                    resolvingOA = false;
                    BarcodeData barcodeData = dataMap.get(Crypto.values());
                    //by hales
                   /* if (barcodeData == null)
                        barcodeData = dataMap.get(Crypto.BTC);*/
                    if (barcodeData != null) {
                        Timber.d("Security=%s, %s", barcodeData.security.toString(), barcodeData.address);
                        processScannedData(barcodeData);
                    } else {
                        beldexAddressTxtLayout.setError(getString(R.string.send_address_not_openalias));
                        Timber.d("NO XMR OPENALIAS TXT FOUND");
                    }
                }

                @Override
                public void onFailure() {
                    resolvingOA = false;
                    beldexAddressTxtLayout.setError(getString(R.string.send_address_not_openalias));
                    Timber.e("OA FAILED");
                }
            });
        } // else ignore
    }
    String dnsFromOpenAlias(String openalias) {
        Timber.d("checking openalias candidate %s", openalias);
        if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias;
        if (Patterns.EMAIL_ADDRESS.matcher(openalias).matches()) {
            openalias = openalias.replaceFirst("@", ".");
            if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias;
        }
        return null; // not an openalias
    }
}