package com.thoughtcrimes.securesms.onboarding

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityDisplayNameBinding
import com.beldex.libbchat.utilities.SSKEnvironment.ProfileManagerProtocol
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.data.DefaultNodes
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.NetworkType
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.util.*
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.Executor


class DisplayNameActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityDisplayNameBinding


    //New Line
    private val NODES_PREFS_NAME: String? = "nodes"
    private val SELECTED_NODE_PREFS_NAME = "selected_node"
    private val PREF_DAEMON_TESTNET = "daemon_testnet"
    private val PREF_DAEMON_STAGENET = "daemon_stagenet"
    private val PREF_DAEMON_MAINNET = "daemon_mainnet"

    private var node: NodeInfo? = null

    private var favouriteNodes: MutableSet<NodeInfo> = HashSet<NodeInfo>()

    //private ImageView ivGuntherLogo;
    //private TextView tvGuntherText;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Display Name",true)
        binding = ActivityDisplayNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            displayNameEditText.imeOptions =
                displayNameEditText.imeOptions or 16777216 // Always use incognito keyboard
            displayNameEditText.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, actionID, event ->
                    if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                        actionID == EditorInfo.IME_ACTION_DONE ||
                        (event.action == KeyEvent.ACTION_DOWN &&
                                event.keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        register()
                        return@OnEditorActionListener true
                    }
                    false
                })
            registerButton.setOnClickListener { register() }
        }
        //New Line load favourites with network function
        loadFavouritesWithNetwork()
    }

    override fun onResume() {
        super.onResume()
        //New Line
        pingSelectedNode()
    }

    fun getOrPopulateFavourites(): Set<NodeInfo?> {
        if (favouriteNodes.isEmpty()) {
            for (node in DefaultNodes.values()) {
                val nodeInfo = NodeInfo.fromString(node.name)
                if (nodeInfo != null) {
                    nodeInfo.setFavourite(true)
                    favouriteNodes.add(nodeInfo)
                }
            }
            saveFavourites()
        }
        return favouriteNodes
    }

    private fun saveFavourites() {
        Timber.d("SAVE")
        val editor = getSharedPreferences(
            NODES_PREFS_NAME,
            MODE_PRIVATE
        ).edit()
        editor.clear()
        var i = 1
        for (info in favouriteNodes) {
            val nodeString = info.toNodeString()
            editor.putString(Integer.toString(i), nodeString)
            Timber.d("saved %d:%s", i, nodeString)
            i++
        }
        editor.apply()
    }

    private fun getFavouriteNodes1(): Set<NodeInfo> {
        return favouriteNodes
    }

    private fun autoselect(nodes: Set<NodeInfo?>): NodeInfo? {
        if (nodes.isEmpty()) return null
        NodePinger.execute(nodes, null)
        val nodeList: List<NodeInfo?> = ArrayList<NodeInfo?>(nodes)
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        return nodeList[0]
    }


    private fun pingSelectedNode() {
        AsyncFindBestNode(this).execute<Int>(PING_SELECTED)
    }


    companion object {
        const val PING_SELECTED = 0
        const val FIND_BEST = 1
    }

    private class AsyncFindBestNode(val displayNameActivity: DisplayNameActivity) :
        AsyncTaskCoroutine<Int?, NodeInfo?>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: NodeInfo?) {
            if (result != null) {
                Timber.d("found a good node %s", result.toString())
                Timber.d("Connected", result.address.toString())
                /*Toast.makeText(
                  this,
                   ""+result.getName().toString() + " connected\n",
                   Toast.LENGTH_SHORT
               ).show()*/
            } else {
                Timber.d("Not Connected", "sdf")
            }
        }

        companion object {
            const val PING_SELECTED = 0
            const val FIND_BEST = 1
        }

        override fun doInBackground(vararg params: Int?): NodeInfo? {
            val favourites: Set<NodeInfo?> = displayNameActivity.getOrPopulateFavourites()

            var selectedNode: NodeInfo?
            if (params[0] == FIND_BEST) {
                selectedNode = displayNameActivity.autoselect(favourites)
            } else if (params[0] == PING_SELECTED) {
                selectedNode = displayNameActivity.getNode()
                if (!displayNameActivity.getFavouriteNodes1()
                        .contains(selectedNode)
                ) selectedNode =
                    null // it's not in the favourites (any longer)
                if (selectedNode == null) for (node in favourites) {
                    if (node!!.isSelected) {
                        selectedNode = node
                        break
                    }
                }
                if (selectedNode == null) { // autoselect
                    selectedNode = displayNameActivity.autoselect(favourites)
                } else
                    selectedNode.testRpcService()
            } else throw java.lang.IllegalStateException()
            return if (selectedNode != null && selectedNode.isValid) {
                Timber.d("Testing-->12")
                displayNameActivity.setNode(selectedNode)
                selectedNode
            } else {
                Timber.d("Testing-->13")
                displayNameActivity.setNode(null)
                null
            }
        }
    }

    fun setNode(node: NodeInfo?) {
        setNode(node, true)
    }

    private fun getNode(): NodeInfo? {
        return node
    }

    private fun setNode(node: NodeInfo?, save: Boolean) {
        if (node !== this.node) {
            require(
                !(node != null && node !== WalletManager.getInstance()
                    .getNetworkType())
            ) { "network type does not match" }
            this.node = node
            for (nodeInfo in favouriteNodes) {
                Timber.d("Testing-->14")
                //Important
                nodeInfo.setSelected(nodeInfo === node)
            }
            WalletManager.getInstance().setDaemon(node)
            if (save) saveSelectedNode()
        }
    }

    private fun saveSelectedNode() { // save only if changed
        val nodeInfo = getNode()
        val selectedNodeId = getSelectedNodeId()
        if (nodeInfo != null) {
            if (!nodeInfo.toNodeString().equals(selectedNodeId)) saveSelectedNode(nodeInfo)
        } else {
            if (selectedNodeId != null) saveSelectedNode(null)
        }
    }

    private fun saveSelectedNode(nodeInfo: NodeInfo?) {
        val editor = getSharedPreferences(
            SELECTED_NODE_PREFS_NAME,
            MODE_PRIVATE
        ).edit()
        if (nodeInfo == null) {
            editor.clear()
        } else {
            editor.putString("0", getNode()?.toNodeString())
        }
        editor.apply()
    }


    private fun register() {
        val displayName = binding.displayNameEditText.text.toString().trim()
        if (displayName.isEmpty()) {
            return Toast.makeText(
                this,
                R.string.activity_display_name_display_name_missing_error,
                Toast.LENGTH_SHORT
            ).show()
        }
        if (displayName.toByteArray().size > ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            return Toast.makeText(
                this,
                R.string.activity_display_name_display_name_too_long_error,
                Toast.LENGTH_SHORT
            ).show()
        }
        //New Line
        if(TextSecurePreferences.getProfileName(this)!=null){
            if(displayName == TextSecurePreferences.getProfileName(this)){
                removeWallet()
            }
        }

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.displayNameEditText.windowToken, 0)
        TextSecurePreferences.setProfileName(this, displayName)

        //New Line
        val uuid = UUID.randomUUID()
        val password = uuid.toString()
        _createWallet(displayName, password)

        //Important
        /*val intent = Intent(this, RegisterActivity::class.java)
        push(intent)*/
    }
    //New Line
    private fun removeWallet(){
        val walletFolder: File = Helper.getWalletRoot(this)
        val walletName = TextSecurePreferences.getWalletName(this)
        val walletFile = File(walletFolder, walletName!!)
        val walletKeys =File(walletFolder, "$walletName.keys")
        val walletAddress = File(walletFolder,"$walletName.address.txt")
        if(walletFile.exists()) {
            walletFile.delete() // when recovering wallets, the cache seems corrupt - so remove it
        }
        if(walletKeys.exists()) {
            walletKeys.delete()
        }
        if(walletAddress.exists()) {
            walletAddress.delete()
        }
    }

    val MNEMONIC_LANGUAGE = "English"

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this@DisplayNameActivity, msg, Toast.LENGTH_LONG).show() }
    }

    fun checkAndCloseWallet(aWallet: Wallet): Boolean {
        val walletStatus = aWallet.status
        if (!walletStatus.isOk) {
            Timber.e(walletStatus.errorString)
            toast(walletStatus.errorString)
        }
        aWallet.close()
        return walletStatus.isOk
    }

    private fun _createWallet(name: String, password: String) {
        Log.d("create Wallet 1","OK")
        createWallet(name, password,
            object : WalletCreator {
                override fun createWallet(aFile: File?, password: String?): Boolean {
                    Log.d("create Wallet 2","OK")
                    //val currentNode: NodeInfo = getNode()
                    // get it from the connected node if we have one, and go back ca. 4 days
                    //val restoreHeight: Long = if (currentNode != null) currentNode.getHeight() - 2000 else -1
                    val newWallet: Wallet = WalletManager.getInstance()
                        .createWallet(
                            aFile,
                            password,
                            MNEMONIC_LANGUAGE,
                            999769
                        )
                    return checkAndCloseWallet(newWallet)
                }
            })
    }
    private fun _getWallet() {
        Log.d("create Wallet 1","OK")
        getWallet(
            object : GetWalletFunction {
                override fun getWallet(): Boolean {
                    Log.d("create Wallet 2","OK")
                    //val currentNode: NodeInfo = getNode()
                    // get it from the connected node if we have one, and go back ca. 4 days
                    //val restoreHeight: Long = if (currentNode != null) currentNode.getHeight() - 2000 else -1
                    val newWallet: Wallet = WalletManager.getInstance()
                        .getWallet()
                    return checkAndCloseWallet(newWallet)
                }
            })
    }


    //New Line
    private fun createWallet(
        name: String?, password: String?,
        walletCreator: WalletCreator
    ) {
        Timber.d("create Wallet","OK")
        if (name != null && password != null) {

            AsyncCreateWallet(name, password, walletCreator,this)
                .execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
        }
    }

    private fun getWallet(
        getWalletFunction: GetWalletFunction
    ) {
        Timber.d("create Wallet","OK")
/*
            AsyncGetWallet(getWalletFunction,this)
                .execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)*/
    }

    interface WalletCreator {
        fun createWallet(aFile: File?, password: String?): Boolean
    }
    interface GetWalletFunction {
        fun getWallet(): Boolean
    }

    private class AsyncCreateWallet(
        val walletName: String,
        val walletPassword:String,
        val walletCreator: WalletCreator,
        val displayNameActivity: DisplayNameActivity
    ) : AsyncTaskCoroutine<Executor?, Boolean?>() {
        var newWalletFile: File? = null
        override fun onPreExecute() {
            super.onPreExecute()
        //    displayNameActivity.acquireWakeLock()
            displayNameActivity.showProgressDialog(R.string.generate_wallet_creating, 250)
        }


        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            /*displayNameActivity.releaseWakeLock(
                5000 // millisconds
            )*/
            if (displayNameActivity.isDestroyed) {
                return
            }
            displayNameActivity.dismissProgressDialog()
            if (result == true) {
                //startDetails(newWalletFile, walletPassword, GenerateReviewFragment.VIEW_TYPE_ACCEPT)
                    Log.d("Wallet","OK")
                //displayNameActivity._getKeys("bxdqxWtcatDFte41zYeWGjBzRgQkFB8AA5gMA3yXLA41RTbrEVoN3976RT2CNHp7PLAR2MsQG1BhMXuLi6HEWWcj2smC3Vfgw");
                TextSecurePreferences.setWalletName(displayNameActivity,walletName)
                val intent = Intent(displayNameActivity, RegisterActivity::class.java)
                val b = Bundle()
                b.putString("type","accept")
                b.putString("path", newWalletFile?.absolutePath)
                b.putString("password", walletPassword)
                b.putString("displayName",displayNameActivity.binding.displayNameEditText.text.toString())
                intent.putExtras(b)
                displayNameActivity.push(intent)
            } else {
                //walletGenerateError()
                print("Error: Create Wallet")
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            // check if the wallet we want to create already exists
            val walletFolder: File = displayNameActivity.getStorageRoot()
            if (!walletFolder.isDirectory) {
                Timber.e("Wallet dir " + walletFolder.absolutePath + "is not a directory")
                return false
            }
            val cacheFile = File(walletFolder, walletName)
            val keysFile = File(walletFolder, "$walletName.keys")
            val addressFile = File(walletFolder, "$walletName.address.txt")
            if (cacheFile.exists() || keysFile.exists() || addressFile.exists()) {
                Timber.e("Some wallet files already exist for %s", cacheFile.absolutePath)
                return false
            }
            newWalletFile = File(walletFolder, walletName)
            val success = walletCreator.createWallet(newWalletFile, walletPassword)
            return if (success) {
                true
            } else {
                Timber.e("Could not create new wallet in %s", newWalletFile!!.absolutePath)
                false
            }
        }
    }

    /*private class AsyncGetWallet(
        val getWalletFunction: GetWalletFunction,
        val displayNameActivity: DisplayNameActivity
    ) : AsyncTaskCoroutine<Executor?, Boolean?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            displayNameActivity.acquireWakeLock()
            *//*displayNameActivity.showProgressDialog(R.string.generate_wallet_creating, 250)*//*
        }


        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            displayNameActivity.releaseWakeLock(
                5000 // millisconds
            )
            if (displayNameActivity.isDestroyed) {
                return
            }
            //displayNameActivity.dismissProgressDialog()
            if (result == true) {
                //startDetails(newWalletFile, walletPassword, GenerateReviewFragment.VIEW_TYPE_ACCEPT)
                Log.d("Get Wallet","OK")
                //displayNameActivity._getKeys("bxdqxWtcatDFte41zYeWGjBzRgQkFB8AA5gMA3yXLA41RTbrEVoN3976RT2CNHp7PLAR2MsQG1BhMXuLi6HEWWcj2smC3Vfgw");

                val intent = Intent(displayNameActivity, RegisterActivity::class.java)
                displayNameActivity.push(intent)
            } else {
                //walletGenerateError()
                print("Error: Create Wallet")
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            // check if the wallet we want to create already exists
            val success = getWalletFunction.createWallet(newWalletFile, walletPassword)
            return if (success) {
                true
            } else {
                Timber.e("Could not create new wallet in %s", newWalletFile!!.absolutePath)
                false
            }
        }
    }*/

    fun getStorageRoot(): File {
        return Helper.getWalletRoot(this)
    }

    private fun loadFavouritesWithNetwork() {
        Helper.runWithNetwork {
            loadFavourites()
            true
        }
    }

    private fun addFavourite(nodeString: String): NodeInfo? {
        val nodeInfo = NodeInfo.fromString(nodeString)
        if (nodeInfo != null) {
            //Important
            nodeInfo.setFavourite(true)
            favouriteNodes.add(nodeInfo)
        } else Timber.w("nodeString invalid: %s", nodeString)
        return nodeInfo
    }

    private fun getSelectedNodeId(): String? {
        return getSharedPreferences(
            SELECTED_NODE_PREFS_NAME,
            MODE_PRIVATE
        )
            .getString("0", null)
    }

    private fun loadLegacyList(legacyListString: String?) {
        if (legacyListString == null) return
        val nodeStrings = legacyListString.split(";".toRegex()).toTypedArray()
        for (nodeString in nodeStrings) {
            addFavourite(nodeString)
        }
    }


    private fun loadFavourites() {
        Timber.d("loadFavourites")
        favouriteNodes.clear()
        val selectedNodeId = getSelectedNodeId()
        val storedNodes = getSharedPreferences(
            NODES_PREFS_NAME,
            MODE_PRIVATE
        ).all
        for (nodeEntry in storedNodes.entries) {
            if (nodeEntry != null) { // just in case, ignore possible future errors
                val nodeId = nodeEntry.value as String
                val addedNode: NodeInfo = addFavourite(nodeId)!!
                if (addedNode != null) {
                    if (nodeId == selectedNodeId) {
                        //Important
                        addedNode.setSelected(true)
                    }
                }
            }
        }
        if (storedNodes.isEmpty()) { // try to load legacy list & remove it (i.e. migrate the data once)
            val sharedPref = getPreferences(MODE_PRIVATE)
            when (WalletManager.getInstance().networkType) {
                NetworkType.NetworkType_Mainnet -> {
                    loadLegacyList(
                        sharedPref.getString(
                            PREF_DAEMON_MAINNET,
                            null
                        )
                    )
                    sharedPref.edit().remove(PREF_DAEMON_MAINNET)
                        .apply()
                }
                NetworkType.NetworkType_Stagenet -> {
                    loadLegacyList(
                        sharedPref.getString(
                            PREF_DAEMON_STAGENET,
                            null
                        )
                    )
                    sharedPref.edit()
                        .remove(PREF_DAEMON_STAGENET).apply()
                }
                NetworkType.NetworkType_Testnet -> {
                    loadLegacyList(
                        sharedPref.getString(
                            PREF_DAEMON_TESTNET,
                            null
                        )
                    )
                    sharedPref.edit().remove(PREF_DAEMON_TESTNET)
                        .apply()
                }
                else -> throw IllegalStateException("unsupported net " + WalletManager.getInstance().networkType)
            }
        }
    }
}