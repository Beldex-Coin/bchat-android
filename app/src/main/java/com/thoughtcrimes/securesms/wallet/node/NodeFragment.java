
package com.thoughtcrimes.securesms.wallet.node;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.google.android.material.textfield.TextInputLayout;
import com.thoughtcrimes.securesms.data.DefaultNodes;
import com.thoughtcrimes.securesms.data.Node;
import com.thoughtcrimes.securesms.data.NodeInfo;
import com.thoughtcrimes.securesms.model.NetworkType;
import com.thoughtcrimes.securesms.model.WalletManager;
import com.thoughtcrimes.securesms.util.Helper;
import com.thoughtcrimes.securesms.util.NodePinger;
import com.thoughtcrimes.securesms.wallet.WalletActivity;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.beldex.bchat.R;
import timber.log.Timber;

public class NodeFragment extends Fragment
        implements NodeInfoAdapter.OnInteractionListener, View.OnClickListener {

    static private int NODES_TO_FIND = 10;

    static public NumberFormat FORMATTER = NumberFormat.getInstance();

    private SwipeRefreshLayout pullToRefresh;
    private TextView tvPull;
    private View fab;

    private Set<NodeInfo> nodeList = new HashSet<>();

    private NodeInfoAdapter nodesAdapter;

    private Listener activityCallback;

    public interface Listener {
        File getStorageRoot();

        void setToolbarButton(int type);

        void setSubtitle(String title);

        void setTitle(int title);

        Set<NodeInfo> getFavouriteNodes();

        Set<NodeInfo> getOrPopulateFavourites();

        void setFavouriteNodes(Collection<NodeInfo> favouriteNodes);

        void setNode(NodeInfo node);

    }

    void filterFavourites() {
        for (Iterator<NodeInfo> iter = nodeList.iterator(); iter.hasNext(); ) {
            Node node = iter.next();
            if (!node.isFavourite()) iter.remove();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onPause() {
        Timber.d("onPause() %d", nodeList.size());
        if (asyncFindNodes != null)
            asyncFindNodes.cancel(true);
        if (activityCallback != null)
            activityCallback.setFavouriteNodes(nodeList);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        //activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
        activityCallback.setTitle(R.string.label_nodes);
        updateRefreshElements();
    }

    boolean isRefreshing() {
        return asyncFindNodes != null;
    }

    void updateRefreshElements() {
        //by Hales63
      /* if (isRefreshing()) {
            activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
            fab.setVisibility(View.GONE);
        } else {
            activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
            fab.setVisibility(View.GONE);
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");
        Log.d("Beldex","Node list onCreateView");
        View view = inflater.inflate(R.layout.activity_node_list, container, false);

        fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(this);

        RecyclerView recyclerView = view.findViewById(R.id.list);
        nodesAdapter = new NodeInfoAdapter(getActivity(), this);
        recyclerView.setAdapter(nodesAdapter);

        tvPull = view.findViewById(R.id.tvPull);

        pullToRefresh = view.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(() -> {
            if (WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) {
                refresh(AsyncFindNodes.SCAN);
            } else {
                Toast.makeText(getActivity(), "Node scanning only mainnet", Toast.LENGTH_LONG).show();
                pullToRefresh.setRefreshing(false);
            }
        });

        Helper.hideKeyboard(getActivity());

        nodeList = new HashSet<>(activityCallback.getFavouriteNodes());
        nodesAdapter.setNodes(nodeList);


        ViewGroup llNotice = view.findViewById(R.id.llNotice);
        //by Hales63
       // Notice.showAll(llNotice, ".*_nodes");

        refresh(AsyncFindNodes.PING); // start connection tests

        return view;
    }

    private AsyncFindNodes asyncFindNodes = null;

    private boolean refresh(int type) {
        if (asyncFindNodes != null) return false; // ignore refresh request as one is ongoing
        asyncFindNodes = new AsyncFindNodes();
        updateRefreshElements();
        asyncFindNodes.execute(type);
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_node_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }



    // Callbacks from NodeInfoAdapter
    @Override
    public void onInteraction(final View view, final NodeInfo nodeItem) {
        AlertDialog.Builder d  = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.switch_node, null);
        d.setView(dialogView);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button yesButton = dialogView.findViewById(R.id.yesButton);
        AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.d("onInteraction");
                if (!nodeItem.isFavourite()) {
                    nodeItem.setFavourite(true);
                    activityCallback.setFavouriteNodes(nodeList);
                }
                AsyncTask.execute(() -> {
                    Log.d("Beldex","Value of current node 2 " +nodeItem.getHost());
                    activityCallback.setNode(nodeItem);
                    // this marks it as selected & saves it as well
                    nodeItem.setSelecting(false);
                    TextSecurePreferences.changeDaemon(requireContext(),true);
                    try {
                        requireActivity().runOnUiThread(() -> nodesAdapter.allowClick(true));
                    } catch (NullPointerException ignored) {
                    }
                });
                alertDialog.dismiss();
            }
        });

       /* new android.app.AlertDialog.Builder(getContext(), R.style.BChatAlertDialog_Wallet)
                .setTitle(R.string.switch_node_alert)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Timber.d("onInteraction");
                    if (!nodeItem.isFavourite()) {
                        nodeItem.setFavourite(true);
                        activityCallback.setFavouriteNodes(nodeList);
                    }
                    AsyncTask.execute(() -> {
                        Log.d("Beldex","Value of current node 2 " +nodeItem.getHost());
                        activityCallback.setNode(nodeItem);
                        // this marks it as selected & saves it as well
                        nodeItem.setSelecting(false);
                        TextSecurePreferences.changeDaemon(requireContext(),true);
                        try {
                            requireActivity().runOnUiThread(() -> nodesAdapter.allowClick(true));
                        } catch (NullPointerException ignored) {
                        }
                    });

                }).setNegativeButton(android.R.string.cancel, null)
                .show();*/
    }

    // open up edit dialog
    @Override
    public boolean onLongInteraction(final View view, final NodeInfo nodeItem) {
        Timber.d("onLongInteraction");
        EditDialog diag = createEditDialog(nodeItem);
        if (diag != null) {
            diag.show();
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab) {
            EditDialog diag = createEditDialog(null);
            if (diag != null) {
                diag.show();
            }
        }
    }

    public void callDialog(){
        EditDialog diag = createEditDialog(null);
        if (diag != null) {
            diag.show();
        }
    }

    private class AsyncFindNodes extends AsyncTask<Integer, NodeInfo, Boolean>
            implements NodePinger.Listener {
        final static int SCAN = 0;
        final static int RESTORE_DEFAULTS = 1;
        final static int PING = 2;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            filterFavourites();
            nodesAdapter.setNodes(null);
            nodesAdapter.allowClick(false);
            tvPull.setText("Scanning networking");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            if (params[0] == RESTORE_DEFAULTS) { // true = restore defaults
                for (DefaultNodes node : DefaultNodes.values()) {
                    NodeInfo nodeInfo = NodeInfo.fromString(node.getUri());
                    if (nodeInfo != null) {
                        nodeInfo.setFavourite(true);
                        nodeList.add(nodeInfo);
                    }
                }
                NodePinger.execute(nodeList, this);
                Log.d("Beldex","Node list majorversion6 ");
                return true;
            } else if (params[0] == PING) {
                Log.d("Beldex","Node list majorversion7 ");
                NodePinger.execute(nodeList, this);
                return true;
            } else if (params[0] == SCAN) {
                // otherwise scan the network
                Timber.d("scanning");
                Set<NodeInfo> seedList = new HashSet<>();
                seedList.addAll(nodeList);
                nodeList.clear();
                Timber.d("seed %d", seedList.size());
                Dispatcher d = new Dispatcher(info -> publishProgress(info));
                d.seedPeers(seedList);
                d.awaitTermination(NODES_TO_FIND);

                // we didn't find enough because we didn't ask around enough? ask more!
                if ((d.getRpcNodes().size() < NODES_TO_FIND) &&
                        (d.getPeerCount() < NODES_TO_FIND + seedList.size())) {
                    // try again
                    publishProgress((NodeInfo[]) null);
                    d = new Dispatcher(new Dispatcher.Listener() {
                        @Override
                        public void onGet(NodeInfo info) {
                            publishProgress(info);
                        }
                    });
                    // also seed with monero seed nodes (see p2p/net_node.inl:410 in monero src)
                    seedList.add(new NodeInfo(new InetSocketAddress("107.152.130.98", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("212.83.175.67", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("5.9.100.248", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("163.172.182.165", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("161.67.132.39", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("198.74.231.92", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("195.154.123.123", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("212.83.172.165", 18080)));
                    seedList.add(new NodeInfo(new InetSocketAddress("192.110.160.146", 18080)));
                    d.seedPeers(seedList);
                    d.awaitTermination(NODES_TO_FIND);
                }
                // final (filtered) result
                nodeList.addAll(d.getRpcNodes());
                return true;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(NodeInfo... values) {
            Timber.d("onProgressUpdate");
            if (!isCancelled())
                if (values != null)
                    nodesAdapter.addNode(values[0]);
                else
                    nodesAdapter.setNodes(null);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Timber.d("done scanning");
            complete();
        }

        @Override
        protected void onCancelled(Boolean result) {
            Timber.d("cancelled scanning");
            complete();
        }

        private void complete() {
            asyncFindNodes = null;
            if (!isAdded()) return;
            //if (isCancelled()) return;
            tvPull.setText("Add node manually or pull down to scan");
            pullToRefresh.setRefreshing(false);
            nodesAdapter.setNodes(nodeList);
            nodesAdapter.allowClick(true);
            updateRefreshElements();
        }

        public void publish(NodeInfo nodeInfo) {
            publishProgress(nodeInfo);
        }
    }

    @Override
    public void onDetach() {
        Timber.d("detached");
        super.onDetach();
    }

    private EditDialog editDialog = null; // for preventing opening of multiple dialogs

    private EditDialog createEditDialog(final NodeInfo nodeInfo) {
        if (editDialog != null) return null; // we are already open
        editDialog = new EditDialog(nodeInfo);
        return editDialog;
    }

    class EditDialog {
        final NodeInfo nodeInfo;
        final NodeInfo nodeBackup;

        private boolean applyChanges() {
            nodeInfo.clear();
            showTestResult();

            final String portString = etNodePort.getEditText().getText().toString().trim();
            int port;
            if (portString.isEmpty()) {
                port = Node.getDefaultRpcPort();
            } else {
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException ex) {
                    etNodePort.setError("must be numeric");
                    return false;
                }
            }
            etNodePort.setError(null);
            if ((port <= 0) || (port > 65535)) {
                etNodePort.setError("must be 1-65535");
                return false;
            }

            final String host = etNodeHost.getEditText().getText().toString().trim();
            if (host.isEmpty()) {
                etNodeHost.setError("we need this");
                return false;
            }
            final boolean setHostSuccess = Helper.runWithNetwork(new Helper.Action() {
                @Override
                public boolean run() {
                    try {
                        nodeInfo.setHost(host);
                        return true;
                    } catch (UnknownHostException ex) {
                        etNodeHost.setError("cannot resolve host");
                        return false;
                    }
                }
            });
            if (!setHostSuccess) {
                etNodeHost.setError("Cannot resolve host");
                return false;
            }
            etNodeHost.setError(null);
            nodeInfo.setRpcPort(port);
            // setName() may trigger reverse DNS
            Helper.runWithNetwork(new Helper.Action() {
                @Override
                public boolean run() {
                    nodeInfo.setName(etNodeName.getEditText().getText().toString().trim());
                    return true;
                }
            });
            nodeInfo.setUsername(etNodeUser.getEditText().getText().toString().trim());
            nodeInfo.setPassword(etNodePass.getEditText().getText().toString()); // no trim for pw
            return true;
        }

        private boolean shutdown = false;

        private void apply() {
            if (applyChanges()) {
                closeDialog();
                if (nodeBackup == null) { // this is a (FAB) new node
                    nodeInfo.setFavourite(true);
                    nodeList.add(nodeInfo);
                }
                shutdown = true;
                new AsyncTestNode().execute();
            }
        }

        private void closeDialog() {
            if (editDialog == null) throw new IllegalStateException();
            Helper.hideKeyboardAlways(requireActivity());
            editDialog.dismiss();
            editDialog = null;
            NodeFragment.this.editDialog = null;
        }

        private void show() {
            editDialog.show();
        }

        private void test() {
            if (applyChanges())
                new AsyncTestNode().execute();
        }

        private void showKeyboard() {
            Helper.showKeyboard(editDialog);
        }

        AlertDialog editDialog = null;

        TextInputLayout etNodeName;
        TextInputLayout etNodeHost;
        TextInputLayout etNodePort;
        TextInputLayout etNodeUser;
        TextInputLayout etNodePass;
        TextView tvResult;
        ImageView iVVerified;
        ImageView iVConnectionError;
        CardView tvResultCardView;

        void showTestResult() {
            if (nodeInfo.isSuccessful()) {
               /* tvResult.setText(getString(R.string.node_result,
                        FORMATTER.format(nodeInfo.getHeight()), nodeInfo.getMajorVersion(),
                        nodeInfo.getResponseTime(), nodeInfo.getHostAddress()));*/
                tvResult.setText(getText(R.string.add_node_success));
                tvResultCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(),(R.color.button_green)));

                iVVerified.setVisibility(View.VISIBLE);
                iVConnectionError.setVisibility(View.GONE);
                tvResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.text));
                TextSecurePreferences.setNodeIsTested(requireContext(),true);

                Log.d("Beldex","showTestResult in NodeFragment()");
            } else {
                Log.d("Beldex","showTestResult in NodeFragment()");
                tvResult.setText(NodeInfoAdapter.getResponseErrorText(getActivity(), nodeInfo.getResponseCode()));
                tvResultCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(),R.color.red));
                TextSecurePreferences.setNodeIsTested(requireContext(),false);
                iVVerified.setVisibility(View.GONE);
                iVConnectionError.setVisibility(View.VISIBLE);
            }
        }

        EditDialog(final NodeInfo nodeInfo) {
            //AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity(),R.style.BChatAlertDialog_AddNode);
            LayoutInflater li = LayoutInflater.from(alertDialogBuilder.getContext());
            View promptsView = li.inflate(R.layout.prompt_editnode, null);
            alertDialogBuilder.setView(promptsView);

            etNodeName = promptsView.findViewById(R.id.nodeNameEditTxtLayout);
            etNodeHost = promptsView.findViewById(R.id.nodeAddressEditTxtLayout);
            etNodePort = promptsView.findViewById(R.id.nodePortEditTxtLayout);
            etNodeUser = promptsView.findViewById(R.id.nodeUsernameEditTxtLayout);
            etNodePass = promptsView.findViewById(R.id.nodePasswordEditTxtLayout);
            tvResult = promptsView.findViewById(R.id.tvResult);
            iVVerified = promptsView.findViewById(R.id.iVVerified);
            iVConnectionError = promptsView.findViewById(R.id.iVConnectionError);
            tvResultCardView = promptsView.findViewById(R.id.testResult_cardview);

            if (nodeInfo != null) {
                this.nodeInfo = nodeInfo;
                nodeBackup = new NodeInfo(nodeInfo);
                etNodeName.getEditText().setText(nodeInfo.getName());
                etNodeHost.getEditText().setText(nodeInfo.getHost());
                etNodePort.getEditText().setText(Integer.toString(nodeInfo.getRpcPort()));
                etNodeUser.getEditText().setText(nodeInfo.getUsername());
                etNodePass.getEditText().setText(nodeInfo.getPassword());
                showTestResult();
            } else {
                this.nodeInfo = new NodeInfo();
                nodeBackup = null;
            }

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Add", null)
                    .setNeutralButton("Test", null)
                    .setNegativeButton(getString(R.string.cancel),
                            (dialog, id) -> {
                                closeDialog();
                                nodesAdapter.setNodes(); // to refresh test results
                            });

            editDialog = alertDialogBuilder.create();
            // these need to be here, since we don't always close the dialog
            editDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface dialog) {
                    Button testButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                    testButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            test();
                        }
                    });

                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d("Beldex","Value of NodeIsTested " +TextSecurePreferences.getNodeIsTested(requireContext()));
                            if (!TextSecurePreferences.getNodeIsTested(requireContext()) && tvResult.getText().toString().equals("")) {
                                Toast.makeText(requireActivity(), getString(R.string.make_sure_you_test_the_node_before_adding_it), Toast.LENGTH_SHORT).show();
                            } else if (tvResult.getText().toString().equals(getString(R.string.node_general_error))) {
                                Toast.makeText(requireActivity(), getString(R.string.unable_to_connect_test_failed), Toast.LENGTH_SHORT).show();
                            } else if (tvResult.getText().toString().equals(getString(R.string.add_node_success))) {
                                apply();
                            }
                        }
                    });
                }
            });

            if (Helper.preventScreenshot()) {
                editDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }

            etNodePass.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        editDialog.getButton(DialogInterface.BUTTON_NEUTRAL).requestFocus();
                        test();
                        return true;
                    }
                    return false;
                }
            });
        }

        private class AsyncTestNode extends AsyncTask<Void, Void, Boolean> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                tvResultCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(),(R.color.button_green)));
                iVVerified.setVisibility(View.GONE);
                iVConnectionError.setVisibility(View.GONE);
                TextSecurePreferences.setNodeIsTested(requireContext(),true);
                tvResult.setText(getString(R.string.node_testing, nodeInfo.getHostAddress()));
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                nodeInfo.testRpcService();
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (editDialog != null) {
                    showTestResult();
                }
                if (shutdown) {
                    if (nodeBackup == null) {
                        nodesAdapter.addNode(nodeInfo);
                    } else {
                        nodesAdapter.setNodes();
                    }
                }
            }
        }
    }

    public void restoreDefaultNodes() {
        if (WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) {
            if (!refresh(AsyncFindNodes.RESTORE_DEFAULTS)) {
                Toast.makeText(getActivity(), R.string.toast_default_nodes, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.node_wrong_net, Toast.LENGTH_LONG).show();
        }
    }
}