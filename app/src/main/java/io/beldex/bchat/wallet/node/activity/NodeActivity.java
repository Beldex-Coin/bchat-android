package io.beldex.bchat.wallet.node.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import io.beldex.bchat.data.DefaultNodes;
import io.beldex.bchat.data.NetworkNodes;
import io.beldex.bchat.data.Node;
import io.beldex.bchat.data.NodeInfo;
import io.beldex.bchat.model.NetworkType;
import io.beldex.bchat.model.WalletManager;
import io.beldex.bchat.util.Helper;
import io.beldex.bchat.util.NodePinger;
import io.beldex.bchat.wallet.node.NodeFragment;
import io.beldex.bchat.wallet.node.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.beldex.bchat.R;
import timber.log.Timber;

public class NodeActivity extends AppCompatActivity implements NodeFragment.Listener {

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
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        loadFavouritesWithNetwork();
        if (TextSecurePreferences.isScreenSecurityEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        Fragment nodeFragment = new NodeFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.nodeList_frame, nodeFragment, NodeFragment.class.getName()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_add_node) {
            TextSecurePreferences.setNodeIsTested(this,false);
            NodeFragment fragment = (NodeFragment) getSupportFragmentManager().findFragmentById(R.id.nodeList_frame);
            fragment.callDialog();
        }
        else if(id == R.id.action_reset_node) {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.nodeList_frame);
            if ((WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) &&
                    (f instanceof NodeFragment)) {
                ((NodeFragment) f).restoreDefaultNodes();
            }
        }
        else if(id == android.R.id.home){
            onBackPressed();
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
        }
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

    public void pingSelectedNode() {
        new AsyncFindBestNode().execute(AsyncFindBestNode.PING_SELECTED);
    }

    private class AsyncFindBestNode extends AsyncTask<Integer, Void, NodeInfo> {
        final static int PING_SELECTED = 0;
        final static int FIND_BEST = 1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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
        }

        @Override
        protected void onCancelled(NodeInfo result) { //TODO: cancel this on exit from fragment
            Timber.d("cancelled with %s", result);
        }
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
        return favouriteNodes;
    }

    @Override
    public Set<NodeInfo> getOrPopulateFavourites() {
        if (favouriteNodes.isEmpty()) {
            for (String node : NetworkNodes.INSTANCE.getNodes()) {
                NodeInfo nodeInfo = NodeInfo.fromString(node);
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
        favouriteNodes.clear();
        for (NodeInfo node : nodes) {
            if (node.isFavourite()) {
                favouriteNodes.add(node);
            }
        }
        saveFavourites();
    }

    private void saveFavourites() {
        SharedPreferences.Editor editor = getSharedPreferences(NODES_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        int i = 1;
        for (Node info : favouriteNodes) {
            final String nodeString = info.toNodeString();
            editor.putString(Integer.toString(i), nodeString);
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
//endregion