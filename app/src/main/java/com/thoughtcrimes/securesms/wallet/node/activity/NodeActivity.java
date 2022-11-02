package com.thoughtcrimes.securesms.wallet.node.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;

import com.thoughtcrimes.securesms.data.DefaultNodes;
import com.thoughtcrimes.securesms.data.Node;
import com.thoughtcrimes.securesms.data.NodeInfo;
import com.thoughtcrimes.securesms.model.NetworkType;
import com.thoughtcrimes.securesms.model.WalletManager;
import com.thoughtcrimes.securesms.service.KeyCachingService;
import com.thoughtcrimes.securesms.util.Helper;
import com.thoughtcrimes.securesms.util.NodePinger;
import com.thoughtcrimes.securesms.wallet.node.NodeFragment;
import com.thoughtcrimes.securesms.wallet.node.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.beldex.bchat.R;
import timber.log.Timber;

public class NodeActivity extends AppCompatActivity implements NodeFragment.Listener, NodeListFragment.NodeListInterface {

    private Toolbar toolbar;
    Set<NodeInfo> favouriteNodes = new HashSet<>();
    private NodeInfo node = null;

    public NodeInfo getNode() {
        return node;
    }

    private static final String NODES_PREFS_NAME = "nodes";
    private static final String SELECTED_NODE_PREFS_NAME = "selected_node";
    private static final String PREF_DAEMON_TESTNET = "daemon_testnet";
    private static final String PREF_DAEMON_STAGENET = "daemon_stagenet";
    private static final String PREF_DAEMON_MAINNET = "daemon_mainnet";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node);
        loadFavouritesWithNetwork();
        Fragment walletFragment = new NodeFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.nodeList_frame, walletFragment, NodeFragment.class.getName()).commit();
        Timber.d("fragment added");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_add_node) {
            NodeFragment fragment = (NodeFragment) getSupportFragmentManager().findFragmentById(R.id.nodeList_frame);
            fragment.callDialog();
        }
        if (id == R.id.action_reset_node) {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.nodeList_frame);
            if ((WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) &&
                    (f instanceof NodeFragment)) {
                ((NodeFragment) f).restoreDefaultNodes();
            }
        }
            return super.onOptionsItemSelected(item);
        }



    private void loadFavouritesWithNetwork() {
        Helper.runWithNetwork(() -> {
            loadFavourites();
            return true;
        });
    }

    private void loadFavourites() {
        Timber.d("loadFavourites");
        favouriteNodes.clear();
        final String selectedNodeId = getSelectedNodeId();
        Map<String, ?> storedNodes = getSharedPreferences(NODES_PREFS_NAME, Context.MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> nodeEntry : storedNodes.entrySet()) {
            if (nodeEntry != null) { // just in case, ignore possible future errors
                final String nodeId = (String) nodeEntry.getValue();
                final NodeInfo addedNode = addFavourite(nodeId);
                if (addedNode != null) {
                    if (nodeId.equals(selectedNodeId)) {
                        addedNode.setSelected(true);
                    }
                }
            }
        }
        if (storedNodes.isEmpty()) { // try to load legacy list & remove it (i.e. migrate the data once)
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            switch (WalletManager.getInstance().getNetworkType()) {
                case NetworkType_Mainnet:
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_MAINNET, null));
                    sharedPref.edit().remove(PREF_DAEMON_MAINNET).apply();
                    break;
                case NetworkType_Stagenet:
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_STAGENET, null));
                    sharedPref.edit().remove(PREF_DAEMON_STAGENET).apply();
                    break;
                case NetworkType_Testnet:
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_TESTNET, null));
                    sharedPref.edit().remove(PREF_DAEMON_TESTNET).apply();
                    break;
                default:
                    throw new IllegalStateException("unsupported net " + WalletManager.getInstance().getNetworkType());
            }
        }
    }

    private NodeInfo addFavourite(String nodeString) {
        final NodeInfo nodeInfo = NodeInfo.fromString(nodeString);
        if (nodeInfo != null) {
            nodeInfo.setFavourite(true);
            favouriteNodes.add(nodeInfo);
        } else
            Timber.w("nodeString invalid: %s", nodeString);
        return nodeInfo;
    }

    private void loadLegacyList(final String legacyListString) {
        if (legacyListString == null) return;
        final String[] nodeStrings = legacyListString.split(";");
        for (final String nodeString : nodeStrings) {
            addFavourite(nodeString);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pingSelectedNode();
    }

    @Override
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nodeList_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void pingSelectedNode() {
        new AsyncFindBestNode().execute(AsyncFindBestNode.PING_SELECTED);
    }

    private class AsyncFindBestNode extends AsyncTask<Integer, Void, NodeInfo> {
        final static int PING_SELECTED = 0;
        final static int FIND_BEST = 1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //pbNode.setVisibility(View.VISIBLE);
            //llNode.setVisibility(View.INVISIBLE);
        }

        @Override
        protected NodeInfo doInBackground(Integer... params) {
            Set<NodeInfo> favourites = getOrPopulateFavourites();
            NodeInfo selectedNode;
            if (params[0] == FIND_BEST) {
                selectedNode = autoselect(favourites);
            } else if (params[0] == PING_SELECTED) {
                selectedNode = getNode();
                if (!getFavouriteNodes().contains(selectedNode))
                    selectedNode = null; // it's not in the favourites (any longer)
                if (selectedNode == null)
                    for (NodeInfo node : favourites) {
                        if (node.isSelected()) {
                            selectedNode = node;
                            break;
                        }
                    }
                if (selectedNode == null) { // autoselect
                    selectedNode = autoselect(favourites);
                } else
                    selectedNode.testRpcService();
            } else throw new IllegalStateException();
            if ((selectedNode != null) && selectedNode.isValid()) {
                setNode(selectedNode);
                return selectedNode;
            } else {
                setNode(null);
                return null;
            }
        }

        @Override
        protected void onPostExecute(NodeInfo result) {
         /*   if (!isAdded()) return;*/
            //pbNode.setVisibility(View.INVISIBLE);
            //llNode.setVisibility(View.VISIBLE);
            if (result != null) {
                Timber.d("found a good node %s", result.toString());
                showNode(result);
            } /*else {
                //tvNodeName.setText(getResources().getText(R.string.node_create_hint));
                // tvNodeName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                // tvNodeAddress.setText(null);
                //tvNodeAddress.setVisibility(View.GONE);
            }*/
        }

        @Override
        protected void onCancelled(NodeInfo result) { //TODO: cancel this on exit from fragment
            Timber.d("cancelled with %s", result);
        }
    }

    private void showNode(NodeInfo nodeInfo) {
        //tvNodeName.setText(nodeInfo.getName());
        //tvNodeName.setCompoundDrawablesWithIntrinsicBounds(NodeInfoAdapter.getPingIcon(nodeInfo), 0, 0, 0);
        //Helper.showTimeDifference(tvNodeAddress, nodeInfo.getTimestamp());
        //tvNodeAddress.setVisibility(View.VISIBLE);
    }

    private NodeInfo autoselect(Set<NodeInfo> nodes) {
        if (nodes.isEmpty()) return null;
        NodePinger.execute(nodes, null);
        List<NodeInfo> nodeList = new ArrayList<>(nodes);
        Collections.sort(nodeList, NodeInfo.BestNodeComparator);
        return nodeList.get(0);
    }

    @Override
    public File getStorageRoot() {
        return Helper.getWalletRoot(getApplicationContext());
    }

    @Override
    public void setToolbarButton(int type) {
        toolbar.setButton(type);
    }

    @Override
    public void setSubtitle(String title) {
        toolbar.setSubtitle(title);

    }

    @Override
    public Set<NodeInfo> getFavouriteNodes() {
        Log.d("Beldex", "Node list %s" + favouriteNodes);
        return favouriteNodes;
    }

    @Override
    public Set<NodeInfo> getOrPopulateFavourites() {
        if (favouriteNodes.isEmpty()) {
            for (DefaultNodes node : DefaultNodes.values()) {
                NodeInfo nodeInfo = NodeInfo.fromString(node.getUri());
                if (nodeInfo != null) {
                    nodeInfo.setFavourite(true);
                    favouriteNodes.add(nodeInfo);
                }
            }
            saveFavourites();
        }
        return favouriteNodes;
    }

    @Override
    public void setFavouriteNodes(Collection<NodeInfo> nodes) {
        Timber.d("adding %d nodes", nodes.size());
        favouriteNodes.clear();
        for (NodeInfo node : nodes) {
            Timber.d("adding %s %b", node, node.isFavourite());
            if (node.isFavourite()) {
                favouriteNodes.add(node);
            }
        }
        saveFavourites();
    }

    private void saveFavourites() {
        Timber.d("SAVE");
        SharedPreferences.Editor editor = getSharedPreferences(NODES_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        int i = 1;
        for (Node info : favouriteNodes) {
            final String nodeString = info.toNodeString();
            editor.putString(Integer.toString(i), nodeString);
            Timber.d("saved %d:%s", i, nodeString);
            i++;
        }
        editor.apply();
    }

    @Override
    public void setNode(NodeInfo node) {
        setNode(node, true);

    }
    private void setNode(NodeInfo node, boolean save) {
        if (node != this.node) {
            if ((node != null) && (node.getNetworkType() != WalletManager.getInstance().getNetworkType()))
                throw new IllegalArgumentException("network type does not match");
            this.node = node;
            for (NodeInfo nodeInfo : favouriteNodes) {
                nodeInfo.setSelected(nodeInfo == node);
            }
            Log.d("Daemon Address setNode() ",""+node.getAddress());
            WalletManager.getInstance().setDaemon(node);
            if (save)
                saveSelectedNode();
        }
    }

    private String getSelectedNodeId() {
        return getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE)
                .getString("0", null);
    }

    private void saveSelectedNode() { // save only if changed
        final NodeInfo nodeInfo = getNode();
        final String selectedNodeId = getSelectedNodeId();
        if (nodeInfo != null) {
            if (!nodeInfo.toNodeString().equals(selectedNodeId))
                saveSelectedNode(nodeInfo);
        } else {
            if (selectedNodeId != null)
                saveSelectedNode(null);
        }
    }

    private void saveSelectedNode(NodeInfo nodeInfo) {
        SharedPreferences.Editor editor = getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (nodeInfo == null) {
            editor.clear();
        } else {
            editor.putString("0", getNode().toNodeString());
        }
        editor.apply();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}