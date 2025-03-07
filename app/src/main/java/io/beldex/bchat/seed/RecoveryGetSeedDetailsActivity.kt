package io.beldex.bchat.seed
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.ArrayMap
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.goterl.lazysodium.utils.KeyPair
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.data.NetworkNodes
import io.beldex.bchat.data.NodeInfo
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.model.AsyncTaskCoroutine
import io.beldex.bchat.model.NetworkType
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.onboarding.AppLockActivity
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.BChatThreadPoolExecutor
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.NodePinger
import io.beldex.bchat.util.RestoreHeight
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRecoveryGetSeedDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executor
import java.util.regex.Pattern

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

    private var restoreFromDateHeight = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val namePattern = Pattern.compile("[A-Za-z0-9\\s]+")
    private val myFormat = "yyyy-MM-dd" // mention the format you need
    val sdf = SimpleDateFormat(myFormat, Locale.US)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoveryGetSeedDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo(getString(R.string.restore_from_seed), false)

        getSeed = intent.extras?.getString("seed")
        // create an OnDateSetListener
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }

        with(binding){
            /*restoreSeedWalletRestoreDate.setOnClickListener {
                restoreSeedWalletRestoreDate.inputType = InputType.TYPE_NULL;
                DatePickerDialog(this@RecoveryGetSeedDetailsActivity,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }*/

            //SteveJosephh21
            restoreSeedWalletRestoreDate.setOnClickListener {
                restoreSeedWalletRestoreDate.inputType = InputType.TYPE_NULL;
                val datePickerDialog = DatePickerDialog(this@RecoveryGetSeedDetailsActivity,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                datePickerDialog.show()
            }

            restoreSeedWalletName.imeOptions = restoreSeedWalletName.imeOptions or 16777216 // Always use incognito keyboard
            restoreSeedWalletName.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    restoreSeedRestoreButton.isEnabled =
                        (s.isNotEmpty() && restoreSeedWalletRestoreHeight.text.trim().isNotEmpty()) || (s.isNotEmpty() && restoreSeedWalletRestoreDate.text.trim().isNotEmpty())
                }

                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                }
            })
            restoreSeedWalletName.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, actionID, _ ->
                    if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                        actionID == EditorInfo.IME_ACTION_DONE
                    ) {
                        register()
                        return@OnEditorActionListener true
                    }
                    false
                })
            restoreSeedWalletRestoreHeight.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    if (restoreSeedWalletRestoreHeight.text.toString().length == 9) {
                        Toast.makeText(
                            this@RecoveryGetSeedDetailsActivity,
                            R.string.enter_a_valid_height,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    restoreSeedRestoreButton.isEnabled =
                        (s.isNotEmpty() && restoreSeedWalletName.text.trim().isNotEmpty()) || (restoreSeedWalletName.text.trim().isNotEmpty() && restoreSeedWalletRestoreDate.text.trim().isNotEmpty())
                }

                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                }
            })
            restoreSeedWalletRestoreHeight.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, actionID, _ ->
                    if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                        actionID == EditorInfo.IME_ACTION_DONE
                    ) {
                        register()
                        return@OnEditorActionListener true
                    }
                    false
                })
            restoreSeedRestoreButton.setOnClickListener { register() }
        }

        binding.restoreFromDateButton.setOnClickListener {
            binding.restoreFromSeedBlockHeightTitle.text = getString(R.string.restore_from_date_title)
            binding.restoreSeedWalletRestoreDateCard.visibility = View.VISIBLE
            binding.restoreSeedWalletRestoreHeightCard.visibility = View.GONE
            binding.restoreFromHeightButton.visibility = View.VISIBLE
            binding.restoreFromDateButton.visibility = View.GONE
            binding.restoreSeedWalletRestoreHeight.text.clear()
        }
        binding.restoreFromHeightButton.setOnClickListener {
            binding.restoreFromSeedBlockHeightTitle.text = getString(R.string.restore_from_height_title)
            binding.restoreSeedWalletRestoreDateCard.visibility = View.GONE
            binding.restoreSeedWalletRestoreHeightCard.visibility = View.VISIBLE
            binding.restoreFromHeightButton.visibility = View.GONE
            binding.restoreFromDateButton.visibility = View.VISIBLE
            binding.restoreSeedWalletRestoreDate.text=""
            binding.restoreSeedRestoreButton.isEnabled = false
        }


        //New Line load favourites with network function
        if (CheckOnline.isOnline(this)) {
            loadFavouritesWithNetwork()
        }
    }
    private fun updateDateInView() {
        binding.restoreSeedWalletRestoreDate.text = sdf.format(cal.time)
        binding.restoreSeedRestoreButton.isEnabled =
            (binding.restoreSeedWalletName.text.trim().isNotEmpty() && binding.restoreSeedWalletRestoreHeight.text.trim().isNotEmpty()) || (binding.restoreSeedWalletName.text.trim().isNotEmpty() && binding.restoreSeedWalletRestoreDate.text.trim().isNotEmpty())
        if (cal.time != null) {
            restoreFromDateHeight = RestoreHeight.getInstance().getHeight(sdf.format(cal.time)).toInt()
        }
    }

    private val pinCodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            TextSecurePreferences.setCopiedSeed(this,true)
            //New Line AirDrop
            TextSecurePreferences.setAirdropAnimationStatus(this,true)

            TextSecurePreferences.setScreenLockEnabled(this, true)
            /*Hales63*/

            TextSecurePreferences.setScreenLockTimeout(this, 950400)
            TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
            val intent1 = Intent(this, KeyCachingService::class.java)
            intent1.action = KeyCachingService.LOCK_TOGGLED_EVENT
            this.startService(intent1)
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            push(intent)
            finish()
        }
    }

    private fun register() {
        val displayName = binding.restoreSeedWalletName.text.toString().trim()
        val restoreHeight = binding.restoreSeedWalletRestoreHeight.text.toString()
        val restoreFromDate = binding.restoreSeedWalletRestoreDate.text.toString()
        if (displayName.isEmpty()) {
            return Toast.makeText(this, R.string.activity_display_name_display_name_missing_error, Toast.LENGTH_SHORT).show()
        }
        if (displayName.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            return Toast.makeText(this, R.string.activity_display_name_display_name_too_long_error, Toast.LENGTH_SHORT).show()
        }

        if (!displayName.matches(namePattern.toRegex())) {
            return Toast.makeText(
                    this,
                    R.string.display_name_validation,
                    Toast.LENGTH_SHORT
            ).show()
        }

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.restoreSeedWalletName.windowToken, 0)
        TextSecurePreferences.setProfileName(this, displayName)
        val uuid = UUID.randomUUID()
        val password = uuid.toString()
        //SteveJosephh21
        if (restoreHeight.isNotEmpty() && binding.restoreSeedWalletRestoreHeightCard.isVisible) {
            val restoreHeightBig = BigInteger(restoreHeight)
            if (restoreHeightBig.toLong() >= 0) {
                val currentDate = sdf.format(Date())
                val currentHeight = RestoreHeight.getInstance().getHeight(currentDate)
                if (restoreHeightBig.toLong() <= currentHeight) {
                    binding.restoreSeedWalletRestoreDate.text = ""
                    binding.restoreSeedRestoreButton.isEnabled = false
                    _recoveryWallet(displayName, password, getSeed, restoreHeight.toLong())
                } else {
                    Toast.makeText(this, getString(R.string.restore_height_excess_error_message), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.restore_height_error_message), Toast.LENGTH_SHORT).show()
            }
        } else if (restoreFromDate.isNotEmpty() && binding.restoreSeedWalletRestoreDateCard.isVisible) {
            binding.restoreSeedWalletRestoreHeight.setText("")
            binding.restoreSeedRestoreButton.isEnabled = false
            _recoveryWallet(displayName, password, getSeed, restoreFromDateHeight.toLong())
        } else if (restoreHeight.isEmpty() && binding.restoreSeedWalletRestoreDateCard.isVisible) {
            Toast.makeText(this, getString(R.string.activity_restore_from_date_missing_error), Toast.LENGTH_SHORT).show()
        } else if (restoreFromDate.isEmpty() && binding.restoreSeedWalletRestoreHeightCard.isVisible) {
            Toast.makeText(this, getString(R.string.activity_restore_from_height_missing_error), Toast.LENGTH_SHORT).show()
        }
    }
    // region Updating
    private fun updateKeyPair() {
        /*Hales63*/
       /* TextSecurePreferences.setRestorationTime(this, 0)
        TextSecurePreferences.setHasViewedSeed(this, false)
*/
        binding.restoreSeedRestoreButton.isEnabled = true
        val intent = Intent(Intent.ACTION_VIEW, "onboarding://manage_pin?finish=true&action=${PinCodeAction.CreatePinCode.action}".toUri())
        pinCodeLauncher.launch(intent)
//        val intent = Intent(this, CreatePasswordActivity::class.java)
//        intent.putExtra("callPage",2)

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
            val favourites: Set<NodeInfo?> = recoveryGetSeedDetailsActivity.getOrPopulateFavourites(recoveryGetSeedDetailsActivity)

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
                recoveryGetSeedDetailsActivity.setNode(selectedNode)
                selectedNode
            } else {
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
            Log.d("networkType","${node!!.networkType},   ${WalletManager.getInstance().networkType}")
            if(!(node!=null && node.networkType !== WalletManager.getInstance()
                    .networkType)) {
                require(
                    !(node != null && node.networkType !== WalletManager.getInstance().networkType)
                ) { "network type does not match" }
            }
            this.node = node
            for (nodeInfo in favouriteNodes) {
                Timber.d("Testing-->14 ${node.toString()}")
                //Important
                nodeInfo.isSelected = nodeInfo === node
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

    private fun _recoveryWallet(
        name: String,
        password: String,
        getSeed: String?,
        restoreHeight: Long
    ) {
        val trimmedName = name.replace(" ","")
        createWallet(trimmedName, password,
            object : WalletCreator {
                override fun createWallet(aFile: File?, password: String?): Boolean {
                    //val currentNode: NodeInfo = getNode()
                    // get it from the connected node if we have one, and go back ca. 4 days
                    //val restoreHeight: Long = if (currentNode != null) currentNode.getHeight() - 2000 else -1
                    val newWallet: Wallet = WalletManager.getInstance()
                        .recoveryWallet(
                            aFile,
                            password,
                            getSeed,
                            restoreHeight
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

                /*val intent = Intent(recoveryGetSeedDetailsActivity, RegisterActivity::class.java)
                val b = Bundle()
                b.putString("type","accept")
                b.putString("path", newWalletFile?.absolutePath)
                b.putString("password", walletPassword)
                intent.putExtras(b)
                recoveryGetSeedDetailsActivity.push(intent)*/
                TextSecurePreferences.setWalletName(recoveryGetSeedDetailsActivity,walletName)
                //SteveJosephh21
                TextSecurePreferences.setWalletPassword(recoveryGetSeedDetailsActivity,walletPassword)
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
                cacheFile.delete()
                keysFile.delete()
                addressFile.delete()
                //return false
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
        val storedNodes: Map<String?,*>? = getSharedPreferences(NODES_PREFS_NAME, MODE_PRIVATE).all
        for (nodeEntry: Map.Entry<String?, *>? in storedNodes!!.entries) {
            val jobsList = arrayListOf<Job>()
            lifecycleScope.launch(Dispatchers.IO) {
                if (nodeEntry != null) {
                    jobsList.add(
                        launch {
                            val nodeId = nodeEntry.value as String
                            val addedNode: NodeInfo? = addFavourite(nodeId)!!
                            if (addedNode != null) {
                                if (nodeId == selectedNodeId) {
                                    //Important
                                    addedNode.isSelected = true
                                }
                            }
                        }
                    )
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