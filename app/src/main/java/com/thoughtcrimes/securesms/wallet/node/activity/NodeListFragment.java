package com.thoughtcrimes.securesms.wallet.node.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thoughtcrimes.securesms.wallet.node.NodeFragment;

import io.beldex.bchat.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NodeListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NodeListFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NodeListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NodeListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NodeListFragment newInstance(String param1, String param2) {
        NodeListFragment fragment = new NodeListFragment();
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
    public interface NodeListInterface{
    void replaceFragment(Fragment fragment);
    }

    private NodeListFragment.NodeListInterface activityCallback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NodeListFragment.NodeListInterface) {
            this.activityCallback = (NodeListFragment.NodeListInterface) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_node_list, container, false);

        TextView textView = view.findViewById(R.id.replaceFragment);
        textView.setOnClickListener(view1 -> {
            activityCallback.replaceFragment(new NodeFragment());
        });
        return view;

    }


}