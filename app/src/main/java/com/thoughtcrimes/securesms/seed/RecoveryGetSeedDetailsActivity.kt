package com.thoughtcrimes.securesms.seed
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import com.goterl.lazysodium.utils.KeyPair
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRecoveryGetSeedDetailsBinding
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.data.DefaultNodes
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.NetworkType
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.onboarding.AppLockActivity
import com.thoughtcrimes.securesms.onboarding.CreatePasswordActivity
import com.thoughtcrimes.securesms.util.*
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class RecoveryGetSeedDetailsActivity :  BaseActionBarActivity() {
    private lateinit var binding:ActivityRecoveryGetSeedDetailsBinding
    var cal = Calendar.getInstance()

    //New Line of Code
    private var seed: ByteArray? = null
    private var ed25519KeyPair: KeyPair? = null
    private var x25519KeyPair: ECKeyPair? = null

    //New Line
    private val NODES_PREFS_NAME: String? = "nodes"
    private val SELECTED_NODE_PREFS_NAME = "selected_node"
    private val PREF_DAEMON_TESTNET = "daemon_testnet"
    private val PREF_DAEMON_STAGENET = "daemon_stagenet"
    private val PREF_DAEMON_MAINNET = "daemon_mainnet"

    private var node: NodeInfo? = null

    private var favouriteNodes: MutableSet<NodeInfo> = HashSet<NodeInfo>()

    private var getSeed:String?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoveryGetSeedDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Restore from Seed", true)

        getSeed = intent.extras?.getString("seed")
        // create an OnDateSetListener
        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int,
                                   dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        }

        with(binding){
            restoreSeedWalletRestoreDate.setOnClickListener {
                restoreSeedWalletRestoreDate.inputType = InputType.TYPE_NULL;
                DatePickerDialog(this@RecoveryGetSeedDetailsActivity,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }

            restoreSeedWalletName.imeOptions = restoreSeedWalletName.imeOptions or 16777216 // Always use incognito keyboard
            restoreSeedWalletName.setOnEditorActionListener(
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
            restoreSeedRestoreButton.setOnClickListener { register() }
        }

        //New Line load favourites with network function
        loadFavouritesWithNetwork()
    }
    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.restoreSeedWalletRestoreDate.setText(sdf.format(cal.time))
    }

    private fun register() {
        val displayName = binding.restoreSeedWalletName.text.toString().trim()
        if (displayName.isEmpty()) {
            return Toast.makeText(this, R.string.activity_display_name_display_name_missing_error, Toast.LENGTH_SHORT).show()
        }
        if (displayName.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            return Toast.makeText(this, R.string.activity_display_name_display_name_too_long_error, Toast.LENGTH_SHORT).show()
        }
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.restoreSeedWalletName.windowToken, 0)
        TextSecurePreferences.setProfileName(this, displayName)

        //New Line Of Code
        //updateKeyPair()
        //New Line
        val uuid = UUID.randomUUID()
        val password = uuid.toString()
        _recoveryWallet(displayName, password,getSeed)
    }

    // region Updating
    private fun updateKeyPair() {
        /*Hales63*/
       /* TextSecurePreferences.setRestorationTime(this, 0)
        TextSecurePreferences.setHasViewedSeed(this, false)
*/
        val intent = Intent(this, CreatePasswordActivity::class.java)
        intent.putExtra("callPage",2)
        push(intent)
        finish()

        //Old Code
       /* val keyPairGenerationResult = KeyPairUtilities.generate()
        seed = keyPairGenerationResult.seed
        ed25519KeyPair = keyPairGenerationResult.ed25519KeyPair
        x25519KeyPair = keyPairGenerationResult.x25519KeyPair
        callAppLockActivity(seed!!,ed25519KeyPair!!,x25519KeyPair!!)*/
    }

    // region Interaction
    private fun callAppLockActivity(
        seed: ByteArray,
        ed25519KeyPair: KeyPair,
        x25519KeyPair: ECKeyPair
    ) {
        /*KeyPairUtilities.store(this, seed, ed25519KeyPair, x25519KeyPair)
        val userHexEncodedPublicKey = x25519KeyPair.hexEncodedPublicKey
        val registrationID = KeyHelper.generateRegistrationId(false)
        TextSecurePreferences.setLocalRegistrationId(this, registrationID)
        TextSecurePreferences.setLocalNumber(this, userHexEncodedPublicKey)*/
        TextSecurePreferences.setRestorationTime(this, 0)
        TextSecurePreferences.setHasViewedSeed(this, false)
        //New Line
        TextSecurePreferences.setHasSeenWelcomeScreen(this, true)

        val intent = Intent(this, AppLockActivity::class.java)
        push(intent)
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

    private class AsyncFindBestNode(val recoveryGetSeedDetailsActivity: RecoveryGetSeedDetailsActivity) :
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
            val favourites: Set<NodeInfo?> = recoveryGetSeedDetailsActivity.getOrPopulateFavourites()

            var selectedNode: NodeInfo?
            if (params[0] == FIND_BEST) {
                selectedNode = recoveryGetSeedDetailsActivity.autoselect(favourites)
            } else if (params[0] == PING_SELECTED) {
                selectedNode = recoveryGetSeedDetailsActivity.getNode()
                if (!recoveryGetSeedDetailsActivity.getFavouriteNodes1()
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
                    selectedNode = recoveryGetSeedDetailsActivity.autoselect(favourites)
                } else
                    selectedNode.testRpcService()
            } else throw java.lang.IllegalStateException()
            return if (selectedNode != null && selectedNode.isValid) {
                Timber.d("Testing-->12")
                recoveryGetSeedDetailsActivity.setNode(selectedNode)
                selectedNode
            } else {
                Timber.d("Testing-->13")
                recoveryGetSeedDetailsActivity.setNode(null)
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

    val MNEMONIC_LANGUAGE = "English"

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this@RecoveryGetSeedDetailsActivity, msg, Toast.LENGTH_LONG).show() }
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

    private fun _recoveryWallet(name: String, password: String, getSeed: String?) {
        Log.d("recovery Wallet 1","OK")
        createWallet(name, password,
            object : WalletCreator {
                override fun createWallet(aFile: File?, password: String?): Boolean {
                    Log.d("recovery Wallet 2","OK")
                    //val currentNode: NodeInfo = getNode()
                    // get it from the connected node if we have one, and go back ca. 4 days
                    //val restoreHeight: Long = if (currentNode != null) currentNode.getHeight() - 2000 else -1
                    val newWallet: Wallet = WalletManager.getInstance()
                        .recoveryWallet(
                            aFile,
                            password,
                            getSeed,
                            0
                        )
                    IdentityKeyUtil.save(this@RecoveryGetSeedDetailsActivity,
                        IdentityKeyUtil.IDENTITY_W_PUBLIC_KEY_PREF,newWallet.publicViewKey)
                    IdentityKeyUtil.save(this@RecoveryGetSeedDetailsActivity,
                        IdentityKeyUtil.IDENTITY_W_PUBLIC_TWO_KEY_PREF,newWallet.secretViewKey)
                    IdentityKeyUtil.save(this@RecoveryGetSeedDetailsActivity,
                        IdentityKeyUtil.IDENTITY_W_PUBLIC_THREE_KEY_PREF,newWallet.publicSpendKey)
                    IdentityKeyUtil.save(this@RecoveryGetSeedDetailsActivity,
                        IdentityKeyUtil.IDENTITY_W_PUBLIC_FOUR_KEY_PREF,newWallet.secretSpendKey)
                    IdentityKeyUtil.save(this@RecoveryGetSeedDetailsActivity,
                        IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF,newWallet.address)
                    TextSecurePreferences.setSenderAddress(this@RecoveryGetSeedDetailsActivity,newWallet.address)
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

            AsyncCreateWallet(name, password, walletCreator, this)
                .execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
        }
    }

    interface WalletCreator {
        fun createWallet(aFile: File?, password: String?): Boolean
    }

    private class AsyncCreateWallet(
        val walletName: String,
        val walletPassword:String,
        val walletCreator: WalletCreator,
        val recoveryGetSeedDetailsActivity: RecoveryGetSeedDetailsActivity
    ) : AsyncTaskCoroutine<Executor?, Boolean?>() {
        var newWalletFile: File? = null
        override fun onPreExecute() {
            super.onPreExecute()
            //recoveryGetSeedDetailsActivity.acquireWakeLock()
            recoveryGetSeedDetailsActivity.showProgressDialog(R.string.generate_wallet_creating, 250)
        }


        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            /*recoveryGetSeedDetailsActivity.releaseWakeLock(
                5000 // millisconds
            )*/
            if (recoveryGetSeedDetailsActivity.isDestroyed) {
                return
            }
            recoveryGetSeedDetailsActivity.dismissProgressDialog()
            if (result == true) {
                //startDetails(newWalletFile, walletPassword, GenerateReviewFragment.VIEW_TYPE_ACCEPT)
                Log.d("Recovery Wallet","OK")

                /*val intent = Intent(recoveryGetSeedDetailsActivity, RegisterActivity::class.java)
                val b = Bundle()
                b.putString("type","accept")
                b.putString("path", newWalletFile?.absolutePath)
                b.putString("password", walletPassword)
                intent.putExtras(b)
                recoveryGetSeedDetailsActivity.push(intent)*/
                TextSecurePreferences.setWalletName(recoveryGetSeedDetailsActivity,walletName)
                recoveryGetSeedDetailsActivity.updateKeyPair()
            } else {
                //walletGenerateError()
                print("Error: Recovery Wallet")
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            // check if the wallet we want to create already exists
            val walletFolder: File = recoveryGetSeedDetailsActivity.getStorageRoot()
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