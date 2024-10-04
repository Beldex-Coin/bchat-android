package io.beldex.bchat.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.beldex.libbchat.utilities.SSKEnvironment.ProfileManagerProtocol
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.data.NetworkNodes
import io.beldex.bchat.data.NodeInfo
import io.beldex.bchat.model.AsyncTaskCoroutine
import io.beldex.bchat.model.NetworkType
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.util.BChatThreadPoolExecutor
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.NodePinger
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityDisplayNameBinding
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executor
import java.util.regex.Pattern


class DisplayNameActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityDisplayNameBinding


    //New Line
    private val NODES_PREFS_NAME: String? = "nodes"
    private val SELECTED_NODE_PREFS_NAME = "selected_node"
    private val PREF_DAEMON_TESTNET = "daemon_testnet"
    private val PREF_DAEMON_STAGENET = "daemon_stagenet"
    private val PREF_DAEMON_MAINNET = "daemon_mainnet"
    private val namePattern = Pattern.compile("[A-Za-z0-9]+")

    private var node: NodeInfo? = null

    private var favouriteNodes: MutableSet<NodeInfo> = HashSet<NodeInfo>()

    //private ImageView ivGuntherLogo;
    //private TextView tvGuntherText;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo(getString(R.string.display_name),false)
        binding = ActivityDisplayNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            displayNameEditText.imeOptions =
                displayNameEditText.imeOptions or 16777216 // Always use incognito keyboard
            displayNameEditText.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, actionID, _ ->
                    if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                        actionID == EditorInfo.IME_ACTION_DONE
                    ) {
                        register()
                        return@OnEditorActionListener true
                    }
                    false
                })

            registerButton.setTextColor(
                ContextCompat.getColor(
                    this@DisplayNameActivity,
                    R.color.disable_button_text_color
                )
            )
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                registerButton.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@DisplayNameActivity,
                        R.drawable.disabled_button_background
                    )
                );
            } else {
                registerButton.background =
                    ContextCompat.getDrawable(
                        this@DisplayNameActivity,
                        R.drawable.disabled_button_background
                    );
            }
            registerButton.isEnabled = displayNameEditText.text.isNotEmpty()
            displayNameEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                    if (s.isEmpty() && s.isBlank()) {
                        registerButton.isEnabled = false
                        registerButton.setTextColor(
                            ContextCompat.getColor(
                                this@DisplayNameActivity,
                                R.color.disable_button_text_color
                            )
                        )
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            registerButton.setBackgroundDrawable(
                                ContextCompat.getDrawable(
                                    this@DisplayNameActivity,
                                    R.drawable.disabled_button_background
                                )
                            );
                        } else {
                            registerButton.background =
                                ContextCompat.getDrawable(
                                    this@DisplayNameActivity,
                                    R.drawable.disabled_button_background
                                );
                        }
                    } else {
                        registerButton.isEnabled = true
                        registerButton.setTextColor(
                            ContextCompat.getColor(
                                this@DisplayNameActivity, R.color.white
                            )
                        )
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            registerButton.setBackgroundDrawable(
                                ContextCompat.getDrawable(
                                    this@DisplayNameActivity,
                                    R.drawable.prominent_filled_button_medium_background
                                )
                            );
                        } else {
                            registerButton.background =
                                ContextCompat.getDrawable(
                                    this@DisplayNameActivity,
                                    R.drawable.prominent_filled_button_medium_background
                                );
                        }
                    }
                }
            })
            registerButton.setOnClickListener {
                if (displayNameEditText.text.isNotEmpty()) {
                    register()
                }
            }
        }
        //New Line load favourites with network function
        if (CheckOnline.isOnline(this)) {
            loadFavouritesWithNetwork()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.registerButton.isEnabled = true
        //New Line
        if (CheckOnline.isOnline(this)) {
            pingSelectedNode()
        }
    }

    fun getOrPopulateFavourites(context: Context): Set<NodeInfo?> {
        if (favouriteNodes.isEmpty()) {
            for (node in NetworkNodes.getNodes(context)) {
                val nodeInfo = NodeInfo.fromString(node)
                if (nodeInfo != null) {
                    nodeInfo.isFavourite = true
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
            val favourites: Set<NodeInfo?> = displayNameActivity.getOrPopulateFavourites(displayNameActivity)

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
                Log.d("Testing-->12 ","$selectedNode")
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
            if(!(node!=null && node.networkType !== WalletManager.getInstance()
                    .networkType)){
            require(
                !(node != null && node.networkType !== WalletManager.getInstance()
                    .networkType)
            ) { "network type does not match" }
            }
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
        if (!displayName.matches(namePattern.toRegex())) {
            return Toast.makeText(
                    this,
                    R.string.display_name_validation,
                    Toast.LENGTH_SHORT
            ).show()
        }
        binding.registerButton.isEnabled = false
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.displayNameEditText.windowToken, 0)
        TextSecurePreferences.setProfileName(this, displayName)

        //New Line
        val uuid = UUID.randomUUID()
        val password = uuid.toString()
        _createWallet(displayName, password)
    }
    //New Line
    private fun removeWallet(){
        if(TextSecurePreferences.getWalletName(this)!=null) {
            val walletFolder: File = Helper.getWalletRoot(this)
            val walletName = TextSecurePreferences.getWalletName(this)
            val walletFile = File(walletFolder, walletName!!)
            val walletKeys = File(walletFolder, "$walletName.keys")
            val walletAddress = File(walletFolder, "$walletName.address.txt")
            if (walletFile.exists()) {
                walletFile.delete() // when recovering wallets, the cache seems corrupt - so remove it
            }
            if (walletKeys.exists()) {
                walletKeys.delete()
            }
            if (walletAddress.exists()) {
                walletAddress.delete()
            }
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
                    val currentNode: NodeInfo? = getNode()
                    Log.d("Beldex", "Value of current Node $currentNode")
                    // get it from the connected node if we have one, and go back ca. 4 days
                    val restoreHeight: Long = if (currentNode != null) currentNode.height else -1
                    Log.d("Beldex", "Value of restoreHeight $restoreHeight")
                    val newWallet: Wallet = WalletManager.getInstance()
                        .createWallet(
                            aFile,
                            password,
                            MNEMONIC_LANGUAGE,
                            restoreHeight
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
                //SteveJosephh21
                TextSecurePreferences.setWalletPassword(displayNameActivity,walletPassword)
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
        val storedNodes: Map<String?,*>? = getSharedPreferences(NODES_PREFS_NAME, MODE_PRIVATE).all
        for (nodeEntry: Map.Entry<String?, *>? in storedNodes!!.entries) {
            if (nodeEntry != null) { // just in case, ignore possible future errors
                val nodeId = nodeEntry.value as String
                val addedNode: NodeInfo? = addFavourite(nodeId)!!
                if (addedNode != null) {
                    if (nodeId == selectedNodeId) {
                        //Important
                        addedNode.isSelected = true
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