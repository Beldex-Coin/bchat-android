package com.thoughtcrimes.securesms.wallet.send;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.qrcode.decoder.Mode;
import com.thoughtcrimes.securesms.data.BarcodeData;
import com.thoughtcrimes.securesms.data.TxData;
import com.thoughtcrimes.securesms.data.UserNotes;
import com.thoughtcrimes.securesms.wallet.node.Toolbar;
import com.thoughtcrimes.securesms.wallet.scan.OnUriScannedListener;

import java.lang.ref.WeakReference;

import io.beldex.bchat.R;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SendFragmentMain#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendFragmentMain extends Fragment  implements SendFragmentSub.Listener, OnUriScannedListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Listener activityCallback;
    private BarcodeData barcodeData;
    private SpendViewPager spendViewPager;
    private SpendPagerAdapter pagerAdapter;
    static private final int MAX_FALLBACK = Integer.MAX_VALUE;
    private TxData txData = new TxData();


    public SendFragmentMain() {
        // Required empty public constructor
    }




    public interface Listener {
        SharedPreferences getPrefs();

        long getTotalFunds();

        boolean isStreetMode();

        void onPrepareSend(String tag, TxData data);

        String getWalletName();

        void onSend(UserNotes notes);

        void onDisposeRequest();

        void onFragmentDone();

        void setToolbarButton(int type);

        void setTitle(String title);

        void setSubtitle(String subtitle);

        void setOnUriScannedListener(OnUriScannedListener onUriScannedListener);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragmentMain.
     */
    // TODO: Rename and change types and number of parameters
    public static SendFragmentMain newInstance(String param1, String param2) {
        SendFragmentMain fragment = new SendFragmentMain();
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
      /*  Fragment childFragment = new SendFragmentSub();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.sendFragment_Main, childFragment).commit();*/

        final View view = inflater.inflate(R.layout.fragment_send_main, container, false);

        spendViewPager = view.findViewById(R.id.pager);
        pagerAdapter = new SpendPagerAdapter(getChildFragmentManager());
        spendViewPager.setOffscreenPageLimit(pagerAdapter.getCount()); // load & keep all pages in cache
        spendViewPager.setAdapter(pagerAdapter);

        spendViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int fallbackPosition = MAX_FALLBACK;
            private int currentPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int newPosition) {
                Timber.d("onPageSelected=%d/%d", newPosition, fallbackPosition);
                if (fallbackPosition < newPosition) {
                    spendViewPager.setCurrentItem(fallbackPosition);
                } else {
                    pagerAdapter.getFragment(currentPosition).onPauseFragment();
                    pagerAdapter.getFragment(newPosition).onResumeFragment();
                    //by hales
                   /* updatePosition(newPosition);*/
                    currentPosition = newPosition;
                    fallbackPosition = MAX_FALLBACK;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    if (!spendViewPager.validateFields(spendViewPager.getCurrentItem())) {
                        fallbackPosition = spendViewPager.getCurrentItem();
                    } else {
                        fallbackPosition = spendViewPager.getCurrentItem() + 1;
                    }
                }
            }
        });
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        activityCallback.setTitle(getString(R.string.send));
        // by hales
       /* if (spendViewPager.getCurrentItem() == SpendPagerAdapter.POS_SUCCESS) {
            activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
        } else {
            activityCallback.setToolbarButton(Toolbar.BUTTON_CANCEL);
        }*/
    }


    @Override
    public void onAttach(@NonNull Context context) {
        Timber.d("onAttach %s", context);
        super.onAttach(context);
        if (context instanceof Listener) {
            activityCallback = (Listener) context;
            activityCallback.setOnUriScannedListener(this);
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }
    @Override
    public boolean onUriScanned(BarcodeData barcodeData) {
        if (spendViewPager.getCurrentItem() == SpendPagerAdapter.POS_ADDRESS) {
            final SendWizardFragment fragment = pagerAdapter.getFragment(SpendPagerAdapter.POS_ADDRESS);
            if (fragment instanceof SendFragmentSub) {
                ((SendFragmentSub) fragment).processScannedData(barcodeData);
                return true;
            }
        }
        return false;
       /* Log.d("Beldex","onUriScanned fun");
        if (spendViewPager.getCurrentItem() == SpendPagerAdapter.POS_ADDRESS) {
            final SendFragmentSub fragment = SendFragmentSub.newInstance(SendFragmentMain.this);
            fragment.processScannedData(barcodeData);
            return true;
        }
        return false;*/
    }

    @Override
    public void onDetach() {
        activityCallback.setOnUriScannedListener(null);
        super.onDetach();
    }


    public void onTransactionSent(final String txId) {
        Timber.d("txid=%s", txId);
        pagerAdapter.addSuccess();
        Timber.d("numPages=%d", spendViewPager.getAdapter().getCount());
        activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
        spendViewPager.setCurrentItem(SpendPagerAdapter.POS_SUCCESS);
    }

    public class SpendPagerAdapter extends FragmentStatePagerAdapter {
        private static final int POS_ADDRESS = 0;
        private static final int POS_AMOUNT = 1;
        private static final int POS_CONFIRM = 2;
        private static final int POS_SUCCESS = 3;
        private int numPages = 3;

        SparseArray<WeakReference<SendWizardFragment>> myFragments = new SparseArray<>();

        public SpendPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        public void addSuccess() {
            numPages++;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return numPages;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Timber.d("instantiateItem %d", position);
            SendWizardFragment fragment = (SendWizardFragment) super.instantiateItem(container, position);
            myFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            Timber.d("destroyItem %d", position);
            myFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public SendWizardFragment getFragment(int position) {
            WeakReference<SendWizardFragment> ref = myFragments.get(position);
            if (ref != null)
                return myFragments.get(position).get();
            else
                return null;
        }



        @NonNull
        @Override
        public SendWizardFragment getItem(int position) {
            Log.d("getItem(%d) CREATE", String.valueOf(position));
            switch (position) {
                case POS_ADDRESS:
                case POS_AMOUNT:
                case POS_CONFIRM:
                case POS_SUCCESS:
                    return SendFragmentSub.newInstance(SendFragmentMain.this);
                default:
                    throw new IllegalArgumentException("no such send position(" + position + ")");
            }


        }

        @Override
        public CharSequence getPageTitle(int position) {
            Timber.d("getPageTitle(%d)", position);
            if (position >= numPages) return null;
            switch (position) {
                case POS_ADDRESS:
                    return getString(R.string.send);
                default:
                    return null;
            }
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            Timber.d("getItemPosition %s", String.valueOf(object));
            if (object instanceof SendFragmentMain) {
                // keep these pages
                return POSITION_UNCHANGED;
            } else {
                return POSITION_NONE;
            }
        }
    }


    @Override
    public void setBarcodeData(BarcodeData data) {
        barcodeData = data;

    }

    @Override
    public BarcodeData getBarcodeData() {
        return barcodeData;
    }

    @Override
    public BarcodeData popBarcodeData() {
        Timber.d("POPPED");
        BarcodeData data = barcodeData;
        barcodeData = null;
        return data;
    }

    @Override
    public TxData getTxData() {
        return txData;
    }

    void disableNavigation() {
        spendViewPager.allowSwipe(false);
    }

    void enableNavigation() {
        spendViewPager.allowSwipe(true);
    }

    public Listener getActivityCallback() {
        return activityCallback;
    }


}