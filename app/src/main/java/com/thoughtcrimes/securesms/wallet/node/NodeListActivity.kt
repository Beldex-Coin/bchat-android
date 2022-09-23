package com.thoughtcrimes.securesms.wallet.node

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.data.NodeInfo
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityNodeListBinding
import timber.log.Timber
import java.io.File
import java.util.HashSet

class NodeListActivity : PassphraseRequiredActionBarActivity(),
    NodeInfoAdapter.OnInteractionListener {

    private lateinit var binding: ActivityNodeListBinding
    private val activityCallback: Listener? = null
    private val nodeList: Set<NodeInfo> = HashSet()
    private val nodesAdapter: NodeInfoAdapter? = null

   /* private val nodelistadapter by lazy {
        NodeInfoAdapter(this, this)
    }*/

    interface Listener {
        val storageRoot: File?

        fun setToolbarButton(type: Int)
        fun setSubtitle(title: String?)
        fun setTitle(title: String?)
        var favouriteNodes: Set<NodeInfo?>?
        val orPopulateFavourites: Set<NodeInfo?>?

        fun setFavouriteNodes(favouriteNodes: Collection<NodeInfo?>?)

        fun setNode(node: NodeInfo?)
    }

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityNodeListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_node_page_title)

       // binding.list.adapter = nodesAdapter
        //binding.list.layoutManager = LinearLayoutManager(this)

        nodesAdapter?.setNodes()



    }

   /* private class AsyncFindNodes :
        AsyncTask<Int?, NodeInfo?, Boolean>(), NodePinger.Listener {
        override fun onPreExecute() {
            super.onPreExecute()
            filterFavourites()
            nodesAdapter.setNodes(null)
            nodesAdapter.allowClick(false)
            tvPull.setText(getString(R.string.node_scanning))
        }


         override fun onProgressUpdate(vararg values: NodeInfo) {
            Timber.d("onProgressUpdate")
            if (!isCancelled) if (values != null) nodesAdapter.addNode(values[0]) else nodesAdapter.setNodes(
                null
            )
        }

        override fun onPostExecute(result: Boolean) {
            Timber.d("done scanning")
            complete()
        }

        override fun onCancelled(result: Boolean) {
            Timber.d("cancelled scanning")
            complete()
        }

        private fun complete() {
            asyncFindNodes = null
            if (!isAdded()) return
            //if (isCancelled()) return;
            tvPull.setText(getString(R.string.node_pull_hint))
            pullToRefresh.setRefreshing(false)
            nodesAdapter.setNodes(nodeList)
            nodesAdapter.allowClick(true)
            updateRefreshElements()
        }

        override fun publish(nodeInfo: NodeInfo?) {
            publishProgress(nodeInfo)
        }

        companion object {
            const val SCAN = 0
            const val RESTORE_DEFAULTS = 1
            const val PING = 2
        }

        override fun doInBackground(vararg p0: Int?): Boolean {
            if (params[0] == RESTORE_DEFAULTS) { // true = restore defaults
                for (node in DefaultNodes.values()) {
                    val nodeInfo = NodeInfo.fromString(node.getUri())
                    if (nodeInfo != null) {
                        nodeInfo.setFavourite(true)
                        nodeList.add(nodeInfo)
                    }
                }
                NodePinger.execute(nodeList, this)
                return true
            } else if (params[0] == PING) {
                NodePinger.execute(nodeList, this)
                return true
            } else if (params[0] == SCAN) {
                // otherwise scan the network
                Timber.d("scanning")
                val seedList: MutableSet<NodeInfo> = HashSet()
                seedList.addAll(nodeList)
                nodeList.clear()
                Timber.d("seed %d", seedList.size)
                var d = Dispatcher { info -> publishProgress(info) }
                d.seedPeers(seedList)
                d.awaitTermination(com.m2049r.xmrwallet.NodeFragment.NODES_TO_FIND)

                // we didn't find enough because we didn't ask around enough? ask more!
                if (d.getRpcNodes().size() < com.m2049r.xmrwallet.NodeFragment.NODES_TO_FIND &&
                    d.getPeerCount() < com.m2049r.xmrwallet.NodeFragment.NODES_TO_FIND + seedList.size
                ) {
                    // try again
                    publishProgress(*null as Array<NodeInfo?>?)
                    d = Dispatcher(object : Listener() {
                        fun onGet(info: NodeInfo?) {
                            publishProgress(info)
                        }
                    })
                    // also seed with monero seed nodes (see p2p/net_node.inl:410 in monero src)
                    seedList.add(NodeInfo(InetSocketAddress("107.152.130.98", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("212.83.175.67", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("5.9.100.248", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("163.172.182.165", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("161.67.132.39", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("198.74.231.92", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("195.154.123.123", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("212.83.172.165", 18080)))
                    seedList.add(NodeInfo(InetSocketAddress("192.110.160.146", 18080)))
                    d.seedPeers(seedList)
                    d.awaitTermination(com.m2049r.xmrwallet.NodeFragment.NODES_TO_FIND)
                }
                // final (filtered) result
                nodeList.addAll(d.getRpcNodes())
                return true
            }
            return false
        }
    }
*/
    override fun onInteraction(view: View?, nodeItem: NodeInfo?) {
       /* Timber.d("onInteraction")
        if (nodeItem != null) {
            if (!nodeItem.isFavourite) {
                nodeItem.setFavourite(true)
                activityCallback?.setFavouriteNodes(nodeList)
            }
        }
        AsyncTask.execute {
            activityCallback?.setNode(nodeItem) // this marks it as selected & saves it as well
            nodeItem.setSelecting(false)
            try {
                requireActivity().runOnUiThread(Runnable { nodesAdapter?.allowClick(true) })
            } catch (ex: NullPointerException) {
                // it's ok
            }
        }*/
    }

    override fun onLongInteraction(view: View?, item: NodeInfo?): Boolean {
       /* val diag: EditDialog = createEditDialog(nodeItem)
        if (diag != null) {
            diag.show()
        }

        */
        return true
    }


   /* internal class EditDialog(nodeInfo: NodeInfo?) {
        val nodeInfo: NodeInfo? = null
        var nodeBackup: NodeInfo? = null
        private fun applyChanges(): Boolean {
            nodeInfo!!.clear()
            showTestResult()
            val portString = etNodePort.editText!!.text.toString().trim { it <= ' ' }
            val port: Int
            if (portString.isEmpty()) {
                port = Node.getDefaultRpcPort()
            } else {
                try {
                    port = portString.toInt()
                } catch (ex: NumberFormatException) {
                    etNodePort.error = getString(R.string.node_port_numeric)
                    return false
                }
            }
            etNodePort.error = null
            if (port <= 0 || port > 65535) {
                etNodePort.error = getString(R.string.node_port_range)
                return false
            }
            val host = etNodeHost.editText!!.text.toString().trim { it <= ' ' }
            if (host.isEmpty()) {
                etNodeHost.error = getString(R.string.node_host_empty)
                return false
            }
            val setHostSuccess = Helper.runWithNetwork(object : Action() {
                fun run(): Boolean {
                    return try {
                        nodeInfo.setHost(host)
                        true
                    } catch (ex: UnknownHostException) {
                        etNodeHost.error = getString(R.string.node_host_unresolved)
                        false
                    }
                }
            })
            if (!setHostSuccess) {
                etNodeHost.error = getString(R.string.node_host_unresolved)
                return false
            }
            etNodeHost.error = null
            nodeInfo.setRpcPort(port)
            // setName() may trigger reverse DNS
            Helper.runWithNetwork(object : Action() {
                fun run(): Boolean {
                    nodeInfo.name = etNodeName.editText!!.text.toString().trim { it <= ' ' }
                    return true
                }
            })
            nodeInfo.setUsername(etNodeUser.editText!!.text.toString().trim { it <= ' ' })
            nodeInfo.setPassword(etNodePass.editText!!.text.toString()) // no trim for pw
            return true
        }

        private var shutdown = false
        private fun apply() {
            if (applyChanges()) {
                closeDialog()
                if (nodeBackup == null) { // this is a (FAB) new node
                    nodeInfo.setFavourite(true)
                    nodeList.add(nodeInfo)
                }
                shutdown = true
                AsyncTestNode().execute()
            }
        }

        private fun closeDialog() {
            checkNotNull(editDialog)
            Helper.hideKeyboardAlways(getActivity())
            editDialog!!.dismiss()
            editDialog = null
            this@NodeFragment.editDialog = null
        }

        private fun show() {
            editDialog!!.show()
        }

        private fun test() {
            if (applyChanges()) AsyncTestNode().execute()
        }

        private fun showKeyboard() {
            Helper.showKeyboard(editDialog)
        }

        var editDialog: AlertDialog? = null
        var etNodeName: TextInputLayout
        var etNodeHost: TextInputLayout
        var etNodePort: TextInputLayout
        var etNodeUser: TextInputLayout
        var etNodePass: TextInputLayout
        var tvResult: TextView
        fun showTestResult() {
            if (nodeInfo!!.isSuccessful) {
                tvResult.setText(
                    getString(
                        R.string.node_result,
                        com.m2049r.xmrwallet.NodeFragment.FORMATTER.format(nodeInfo.getHeight()),
                        nodeInfo.getMajorVersion(),
                        nodeInfo.getResponseTime(),
                        nodeInfo.getHostAddress()
                    )
                )
            } else {
                tvResult.text =
                    NodeInfoAdapter.getResponseErrorText(getActivity(), nodeInfo.responseCode)
            }
        }

        private inner class AsyncTestNode :
            AsyncTask<Void?, Void?, Boolean>() {
            override fun onPreExecute() {
                super.onPreExecute()
                tvResult.setText(getString(R.string.node_testing, nodeInfo!!.getHostAddress()))
            }

            protected override fun doInBackground(vararg params: Void): Boolean {
                nodeInfo!!.testRpcService()
                return true
            }

            override fun onPostExecute(result: Boolean) {
                if (editDialog != null) {
                    showTestResult()
                }
                if (shutdown) {
                    if (nodeBackup == null) {
                        nodesAdapter.addNode(nodeInfo)
                    } else {
                        nodesAdapter.setNodes()
                    }
                }
            }
        }

        init {
            //AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
            val alertDialogBuilder = AlertDialog.Builder(getActivity(), R.style.backgroundColor)
            val li = LayoutInflater.from(alertDialogBuilder.context)
            val promptsView: View = li.inflate(R.layout.prompt_editnode, null)
            alertDialogBuilder.setView(promptsView)
            etNodeName = promptsView.findViewById(R.id.etNodeName)
            etNodeHost = promptsView.findViewById(R.id.etNodeHost)
            etNodePort = promptsView.findViewById(R.id.etNodePort)
            etNodeUser = promptsView.findViewById(R.id.etNodeUser)
            etNodePass = promptsView.findViewById(R.id.etNodePass)
            tvResult = promptsView.findViewById(R.id.tvResult)
            if (nodeInfo != null) {
                this.nodeInfo = nodeInfo
                nodeBackup = NodeInfo(nodeInfo)
                etNodeName.editText!!.setText(nodeInfo.name)
                etNodeHost.editText.setText(nodeInfo.getHost())
                etNodePort.editText!!.setText(Integer.toString(nodeInfo.getRpcPort()))
                etNodeUser.editText.setText(nodeInfo.getUsername())
                etNodePass.editText.setText(nodeInfo.getPassword())
                showTestResult()
            } else {
                this.nodeInfo = NodeInfo()
                nodeBackup = null
            }

            // set dialog message
            alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.label_ok), null)
                .setNeutralButton(getString(R.string.label_test), null)
                .setNegativeButton(getString(R.string.label_cancel),
                    DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                        closeDialog()
                        nodesAdapter.setNodes() // to refresh test results
                    })
            editDialog = alertDialogBuilder.create()
            // these need to be here, since we don't always close the dialog
            editDialog!!.setOnShowListener { dialog ->
                val testButton =
                    (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
                testButton.setOnClickListener { test() }
                val button =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                button.setOnClickListener { apply() }
            }
            if (Helper.preventScreenshot()) {
                editDialog!!.window!!
                    .setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
            }
            etNodePass.editText!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    editDialog!!.getButton(DialogInterface.BUTTON_NEUTRAL).requestFocus()
                    test()
                    return@OnEditorActionListener true
                }
                false
            })
        }
    }*/

}