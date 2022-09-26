package com.thoughtcrimes.securesms.wallet.send;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.thoughtcrimes.securesms.data.Listener;

import io.beldex.bchat.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SendFragmentNew#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendFragmentNew extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public static SendFragmentNew newInstance(Listener listener) {
        SendFragmentNew instance = new SendFragmentNew();
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

    // TODO: Rename and change types and number of parameters
    public static SendFragmentNew newInstance(String param1, String param2) {
        SendFragmentNew fragment = new SendFragmentNew();
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

        return inflater.inflate(R.layout.fragment_send_new, container, false);
    }
}