package io.beldex.bchat.wallet.jetpackcomposeUI.node

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.changeDaemon
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getNodeIsMainnet
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getNodeIsTested
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setNodeIsMainnet
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setNodeIsTested
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.data.NetworkNodes.getNodes
import io.beldex.bchat.data.Node
import io.beldex.bchat.data.NodeInfo
import io.beldex.bchat.model.NetworkType
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.my_account.ui.CardContainer
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.NodePinger
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.wallet.node.DiffCallback
import io.beldex.bchat.wallet.node.Dispatcher
import io.beldex.bchat.wallet.node.NodeInfoAdapter
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.wallet.CheckOnline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.UnknownHostException
import javax.inject.Inject


@AndroidEntryPoint
class NodeComposeActivity : ComponentActivity() {

    private val nodeViewModel: NodeViewModel by viewModels()

    @Inject
    lateinit var walletManager: WalletManager

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnrememberedMutableState", "MutableCollectionMutableState", "CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view = LocalView.current
            val window = (view.context as Activity).window
            val statusBarColor = if (isDarkTheme) Color.Black else Color.White
            SideEffect {
                window.statusBarColor = statusBarColor.toArgb()
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
            BChatTheme(darkTheme = isDarkTheme) {
                val context = LocalContext.current
                val activity = (context as? Activity)
                if (TextSecurePreferences.isScreenSecurityEnabled(context))
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                Surface(
                    modifier=Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    Scaffold(
                            containerColor=MaterialTheme.colorScheme.primary,
                    ) {
                        val lifecycleOwner=LocalLifecycleOwner.current
                        pingSelectedNode(context, nodeViewModel)
                        nodeViewModel.loadFavouritesWithNetwork()
                        var nodesValue by remember {
                            mutableStateOf("")
                        }
                        nodeViewModel.currentNode.observe(lifecycleOwner){ nodes->
                            nodesValue = nodes
                        }
                        NodeScreenContainer(title=stringResource(id=R.string.label_nodes), onBackClick={
                            val returnIntent = Intent()
                            returnIntent.putExtra("selected_node_key",nodesValue)
                            (context as Activity).run {
                                setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                                (context as ComponentActivity).finish()
                            }
                        }) {
                            val test = true
                            NodeScreen(test)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class,ExperimentalAnimationApi::class)
@SuppressLint("UnrememberedMutableState", "MutableCollectionMutableState", "CoroutineCreationDuringComposition")
@Composable
fun NodeScreen(test:Boolean = false) {

    val nodeViewModel: NodeViewModel=hiltViewModel()
    val context=LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current

    var nodesValue by remember {
        mutableStateOf("")
    }
    nodeViewModel.currentNode.observe(lifecycleOwner){ nodes->
        nodesValue = nodes
    }

    BackHandler {
        val returnIntent = Intent()
        returnIntent.putExtra("selected_node_key",nodesValue)
        (context as Activity).run {
            setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
            (context as ComponentActivity).finish()
        }
    }

    var callAsyncNodes by remember {
        mutableStateOf(test)
    }

    var data by rememberSaveable(Unit) {
        mutableStateOf(nodeViewModel.favouritesNodes.value)
    }

    nodeViewModel.favouritesNodes.observe(lifecycleOwner) { nodes ->
        if (nodes != null) {
            data=nodes
        }

    }

    var nodeName by remember {
        mutableStateOf("")
    }

    var nodeAddress by remember {
        mutableStateOf("")
    }

    var errorAction by remember {
        mutableStateOf(false)
    }

    var showAddNode by remember {
        mutableStateOf(false)
    }
    var showAddNodeEdit by remember {
        mutableStateOf(false)
    }

    var showChangeNodePopup by remember {
        mutableStateOf(false)
    }

    var showRefreshNodePopup by remember {
        mutableStateOf(false)
    }

    var isVisible by remember { mutableStateOf(true) }

    var selectedItemIndex by remember {
        mutableIntStateOf(-1)
    }

    val configuration=LocalConfiguration.current
    val screenWidth=configuration.screenWidthDp

    val fontSize=when {
        screenWidth < 360 -> 14.sp
        else -> 16.sp
    }

    val scan=0
    val restoreDefault=1
    val ping=2
    val nodesToFind=10


    fun getResponseErrorText(ctx: Context, responseCode: Int): String {
        return when (responseCode) {
            0 -> {
                ctx.resources.getString(R.string.node_general_error)
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                ctx.resources.getString(R.string.node_auth_error)
            }
            else -> {
                ctx.resources.getString(R.string.node_test_error, responseCode)
            }
        }
    }

    fun publish(node: NodeInfo?) {}

    fun filterFavourites() {
        val iterator: MutableIterator<NodeInfo> =nodeViewModel.favouritesNodes.value!!.iterator()
        while (iterator.hasNext()) {
            val node: Node=iterator.next()
            if (!node.isFavourite) iterator.remove()
        }
    }

    class AsyncFindNodes : AsyncTask<Int, NodeInfo, Boolean>(), NodePinger.Listener {
        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()
            filterFavourites()
            //nodeViewModel.setNode(null, true, context)
            //nodesAdapter.allowClick(false)
            //tvPull.setText("Scanning networking")
        }

        @Deprecated("Deprecated in Java")
        @SuppressLint("WrongThread")
        override fun doInBackground(vararg params: Int?): Boolean? {
            if (params[0] == restoreDefault) {
                // true = restore defaults
                nodeViewModel.favouritesNodes.value?.clear()
                for (node in getNodes(context)) {
                    val nodeInfo=NodeInfo.fromString(node)
                    if (nodeInfo != null) {
                        nodeInfo.isFavourite=true
                        nodeViewModel.favouritesNodes.value?.add(nodeInfo)
                    }
                }
                nodeViewModel.favouritesNodes.value?.toMutableList()?.random()?.isFavourite=true
                nodeViewModel.setNode(nodeViewModel.favouritesNodes.value?.toMutableList()?.random(), true, context)
                nodeViewModel.favouritesNodes.value?.toMutableList()?.random()?.isSelecting=true
                changeDaemon(context, true)
                nodeViewModel.saveNodes(nodeViewModel.favouritesNodes.value!!)
                NodePinger.execute(nodeViewModel.favouritesNodes.value, this)
                return true
            } else if (params[0] == ping) {
                NodePinger.execute(nodeViewModel.favouritesNodes.value, this)
                return true
            } else if (params[0] == scan) {
                // otherwise scan the network
                val seedList=mutableSetOf<NodeInfo>()
                seedList.addAll(nodeViewModel.favouritesNodes.value!! as Collection<NodeInfo>)
                nodeViewModel.favouritesNodes.value!!.clear()
                var d = Dispatcher { info -> publishProgress(info) }
                d.seedPeers(seedList)
                d.awaitTermination(nodesToFind)
                // we didn't find enough because we didn't ask around enough? ask more!
                if ((d.rpcNodes.size < nodesToFind) && (d.peerCount < nodesToFind + seedList.size)) {
                    // try again
                    publishProgress(null)
                    d=Dispatcher { info -> publishProgress(info) }
                    // also seed with beldex seed nodes (see p2p/net_node.inl:410 in beldex src)
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
                    d.awaitTermination(nodesToFind)
                }
                // final (filtered) result
                nodeViewModel.updateNodeList(d.rpcNodes as NodeInfo)
                //nodeList.addAll(d.rpcNodes)
                return true
            }
            return false
        }

        @Deprecated("Deprecated in Java")
        override fun onProgressUpdate(vararg values: NodeInfo) {
            if (!isCancelled) {
                if (values != null) {
                    nodeViewModel.addNode(values[0], context)
                } else {
                    nodeViewModel.setNode(null, true, context)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Boolean) {
            complete()
        }

        @Deprecated("Deprecated in Java")
        override fun onCancelled(result: Boolean) {
            complete()
        }

        private fun complete() {
            //asyncFindNodes = null
            // if (!isAdded) return
            ///tvPull.setText("Add node manually or pull down to scan")
            //pullToRefresh.setRefreshing(false)
            //nodeViewModel.setNode(nodeViewModel.favouritesNodes.value, true, context)
            //nodesAdapter.allowClick(true)
        }

        override fun publish(nodeInfo: NodeInfo) {
            publish(nodeInfo)
        }
    }

    if(callAsyncNodes){
        val task=AsyncFindNodes()
        task.execute(ping)
        callAsyncNodes = false
    }

    fun refresh(type: Int): Boolean {
        val task=AsyncFindNodes()
        task.execute(type)
        return true
    }

    fun refreshTheNodes() {
        if (WalletManager.getInstance().networkType == NetworkType.NetworkType_Mainnet) {
            refresh(scan)
        } else {
            Toast.makeText(context, "Node scanning only mainnet", Toast.LENGTH_LONG).show()
            // need to disabled refresh button
            //pullToRefresh.setRefreshing(false)
        }
    }

    fun restoreDefaultNodes() {
        if (WalletManager.getInstance().networkType == NetworkType.NetworkType_Mainnet) {
            if (!refresh(restoreDefault)) {
                Toast.makeText(context, R.string.toast_default_nodes, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.node_wrong_net, Toast.LENGTH_LONG).show()
        }
    }

    if (showAddNode) {
        AddNodePopUp(onDismiss={
            showAddNode=false
            showAddNodeEdit=false
            selectedItemIndex=-1
        }, nodeInfo=NodeInfo(), nodeViewModel.favouritesNodes.value!!, showAddNodeEdit, selectedItemIndex)
    }

    if (showChangeNodePopup) {
        SwitchNodePopUp(onDismiss={
            showChangeNodePopup=false
        }, nodeViewModel, nodeViewModel.favouritesNodes.value?.toMutableList()!![selectedItemIndex])
    }

    if (showRefreshNodePopup) {
        if(CheckOnline.isOnline(context)) {
            RefreshNodePopup(onDismiss={
                showRefreshNodePopup=false
            }, onCallRefresh = {
                showRefreshNodePopup = false
                if(CheckOnline.isOnline(context)) {
                    isVisible = !isVisible
                    restoreDefaultNodes()
                    //refreshTheNodes()
                    lifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        isVisible = true
                    }
                } else {
                    Toast.makeText(
                        context,
                        R.string.please_check_your_internet_connection,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }else{
            showRefreshNodePopup=false
            Toast.makeText(
                context,
                R.string.please_check_your_internet_connection,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

        Column(modifier=Modifier
            .fillMaxSize()
            .padding(vertical=10.dp)) {


            Column(modifier =Modifier
                .fillMaxWidth()
                .weight(1f)) {


            AnimatedContent(targetState=isVisible, label="NodeList",
                    modifier=Modifier)
            {
                if (it) {
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp), horizontalAlignment=Alignment.CenterHorizontally

                    ) {
                        itemsIndexed(data!!.toMutableList()) { index, item ->
                            Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.editTextBackground), border=BorderStroke(width=2.dp, color=if (item.isSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.editTextBackground), shape=RoundedCornerShape(12.dp), elevation=CardDefaults.cardElevation(defaultElevation=0.dp), modifier=Modifier
                                .fillMaxWidth()
                                .padding(horizontal=16.dp)
                                .combinedClickable(
                                    onClick={
                                        selectedItemIndex=index
                                        showChangeNodePopup=true

                                    },
                                    onLongClick={
                                        selectedItemIndex=index
                                        showAddNodeEdit=true
                                        showAddNode=true

                                    },
                                )) {
                                nodeName=item.name
                                nodeAddress=item.address

                                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center, modifier=Modifier.padding(horizontal=24.dp)) {
                                    Icon(painter=painterResource(id=R.drawable.ic_connected), contentDescription="", tint=if (errorAction) MaterialTheme.appColors.errorMessageColor else MaterialTheme.appColors.primaryButtonColor, modifier=Modifier.size(10.dp)

                                    )

                                    Column(horizontalAlignment=Alignment.Start, verticalArrangement=Arrangement.Center, modifier=Modifier
                                        .padding(vertical=10.dp, horizontal=20.dp)
                                        .weight(0.7f)) {
                                        if (item.isTested) {
                                            if (item.isValid) {
                                                errorAction=false
                                                //IMPORTANT
                                                //Helper.showTimeDifference(nodeAddress., item.timestamp)
                                                // need to update node status image
                                                /*nodeStatusView_Connect.setVisibility(View.VISIBLE)
                                            nodeStatusView_Error.setVisibility(View.INVISIBLE)*/
                                            } else {
                                                nodeAddress=getResponseErrorText(context, item.responseCode)
                                                errorAction=true
                                                // need to update color for address and status image
                                                /*nodeAddress.setTextColor(ThemeHelper.getThemedColor(context, R.attr.colorError))
                                             nodeStatusView_Error.setVisibility(View.VISIBLE)
                                             nodeStatusView_Connect.setVisibility(View.VISIBLE)*/
                                            }
                                        } else {
                                            nodeAddress=context.resources.getString(R.string.node_testing, item.hostAddress)
                                        }

                                        Text(text=nodeName, style=BChatTypography.titleMedium.copy(color=if (item.isSelected) MaterialTheme.appColors.textColor else if (errorAction) MaterialTheme.appColors.errorMessageColor else MaterialTheme.appColors.primaryButtonColor, fontSize=fontSize, fontWeight=FontWeight(600)))
                                        Text(text=nodeAddress, style=BChatTypography.titleSmall.copy(color=MaterialTheme.appColors.editTextColor, fontSize=12.sp, fontWeight=FontWeight(400)), modifier=Modifier.padding(vertical=5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            Column(

                verticalArrangement=Arrangement.Bottom, horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.fillMaxWidth()) {

                Row(modifier=Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Button(onClick={
                        showRefreshNodePopup=true
                    }, colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.appColors.searchBackground),
                        modifier=Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text="Refresh", style=MaterialTheme.typography.bodyMedium.copy(color=MaterialTheme.appColors.secondaryContentColor, fontWeight=FontWeight(400), fontSize = 14.sp), modifier=Modifier.padding(10.dp))
                        Icon(painter=painterResource(id=R.drawable.ic_refresh), contentDescription="Refresh", modifier=Modifier, tint=MaterialTheme.appColors.secondaryContentColor)
                    }

                    Spacer(modifier=Modifier.width(16.dp))

                    Button(onClick={
                        showAddNode=true
                    }, colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.appColors.primaryButtonColor),
                        modifier=Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text=stringResource(id=R.string.node_fab_add), style=MaterialTheme.typography.bodyMedium.copy(color=Color.White, fontWeight=FontWeight(400), fontSize = 14.sp), modifier=Modifier.padding(10.dp))
                    }
                }
            }
        }
}

@Composable
fun RefreshNodePopup(onDismiss: () -> Unit,onCallRefresh: () -> Unit) {

    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest=onDismiss,
    ) {

        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground), elevation=CardDefaults.cardElevation(defaultElevation=4.dp), modifier=Modifier.fillMaxWidth()) {
            Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center, modifier=Modifier
                .fillMaxWidth()
                .padding(10.dp)) {

                Text(
                    text=stringResource(id=R.string.refresh_nodes),
                    textAlign=TextAlign.Center,
                    style=MaterialTheme.typography.titleMedium.copy(
                        fontSize=16.sp,
                        fontWeight=FontWeight(700),
                        color=MaterialTheme.appColors.secondaryContentColor),
                    modifier=Modifier.padding(top =20.dp, start =40.dp, end = 40.dp))

                Text(
                    text=stringResource(id=R.string.refresh_node_alert),
                    textAlign=TextAlign.Center,
                    style=MaterialTheme.typography.titleMedium.copy(
                        fontSize=14.sp,
                        fontWeight=FontWeight(400),
                        color=MaterialTheme.appColors.editTextColor),
                    modifier=Modifier.padding(top =10.dp, bottom = 20.dp, start =40.dp, end = 40.dp))

                Row(
                    modifier=Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick={ onDismiss() },
                        colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.negativeGreenButton
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(width = 0.5.dp, color = MaterialTheme.appColors.negativeGreenButtonBorder),
                        modifier=Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text=stringResource(id=R.string.cancel),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.appColors.negativeGreenButtonText,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp
                            ),
                            modifier=Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier=Modifier.width(16.dp))

                    Button(
                        onClick={
                            println("refresh node in call onclick function")
                           onCallRefresh()
                            println("refresh node in call onclick function 1")
                        },
                        colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.primaryButtonColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier=Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text= stringResource(id = R.string.yes),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                color=Color.White,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp
                            ),
                            modifier=Modifier.padding(10.dp)
                        )
                    }
                }

            }
        }
    }

}


@Composable
fun AddNodePopUp(onDismiss: () -> Unit, nodeInfo: NodeInfo, nodeList: MutableSet<NodeInfo>, showAddNodeEdit: Boolean, selectedIndex: Int) {

    val nodeBackup: NodeInfo?=null
    val context=LocalContext.current
    val nodeViewModel: NodeViewModel=viewModel()


    var nodeAddress by remember {
        mutableStateOf(if (showAddNodeEdit) nodeList.toList()[selectedIndex].name else "")
    }
    var nodeAddressErrorAction by remember {
        mutableStateOf(false)
    }
    var nodeAddressErrorText by remember {
        mutableStateOf("")
    }
    var nodePort by remember {
        mutableStateOf(if (showAddNodeEdit) (nodeList.toList()[selectedIndex].rpcPort).toString() else "")
    }
    var nodePortErrorAction by remember {
        mutableStateOf(false)
    }
    var nodePortErrorText by remember {
        mutableStateOf("")
    }

    var nodeName by remember {
        mutableStateOf(if (showAddNodeEdit) nodeList.toList()[selectedIndex].name else "")
    }

    var nodeUserName by remember {
        mutableStateOf("")
    }
    var nodePassword by remember {
        mutableStateOf("")
    }
    var nodeStatusSuccessAction by remember {
        mutableStateOf(false)
    }
    var nodeStatusErrorAction by remember {
        mutableStateOf(false)
    }
    var nodeStatus by remember {
        mutableStateOf("")
    }
    var testProgressAction by remember {
        mutableStateOf(false)
    }

    var shutdown by remember {
        mutableStateOf(false)
    }

    fun showTestResult() {
        if(nodeInfo.isTested) {
            if (nodeInfo.isSuccessful) {
                nodeStatusSuccessAction=true
                nodeStatusErrorAction=false
                nodeStatus=context.getString(R.string.add_node_success)
                setNodeIsTested(context, true)
            } else {
                nodeStatusSuccessAction=false
                nodeStatusErrorAction=true
                nodeStatus=(NodeInfoAdapter.getResponseErrorText(context, nodeInfo.responseCode))
                setNodeIsTested(context, false)
            }
        }
    }

    fun applyChanges(): Boolean {
        nodeInfo.clear()
        //showTestResult()
        val port: Int=if (nodePort.isEmpty()) {
            Node.getDefaultRpcPort()
        } else {
            try {
                nodePort.toInt()
            } catch (ex: NumberFormatException) {
                nodePortErrorAction=true
                nodePortErrorText="must be numeric"
                return false
            }
        }
        nodePortErrorAction=false
        nodePortErrorText=""
        if (port <= 0 || port > 65535) {
            nodePortErrorAction=true
            nodePortErrorText="must be 1-65535"
            return false
        }
        if (nodeAddress.isEmpty()) {
            nodeAddressErrorAction=true
            nodeAddressErrorText="we need this"
            return false
        }
        val setHostSuccess=Helper.runWithNetwork(Helper.Action {
            try {
                nodeInfo.host=nodeAddress
                return@Action true
            } catch (ex: UnknownHostException) {
                nodeAddressErrorAction=true
                nodeAddressErrorText="cannot resolve host"
                return@Action false
            }
        })
        if (!setHostSuccess) {
            nodeAddressErrorAction=true
            nodeAddressErrorText="cannot resolve host"
            return false
        }
        nodeAddressErrorAction=false
        nodeAddressErrorText=""
        nodeInfo.rpcPort=port
        Helper.runWithNetwork {
            nodeInfo.name=nodeUserName.trim { it <= ' ' }
            true
        }
        nodeInfo.username=nodeUserName.trim { it <= ' ' }
        nodeInfo.password=nodePassword
        return true
    }

    fun asyncTestNode() {
        object : AsyncTask<Void, Void, Boolean>() {
            @Deprecated("Deprecated in Java")
            override fun onPreExecute() {
                super.onPreExecute()
                setNodeIsTested(context, true)
                nodeStatus=context.getString(R.string.node_testing, nodeInfo?.hostAddress)
            }

            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void?): Boolean {
                nodeInfo.testIsMainnet()
                val isMainnet=nodeInfo.testIsMainnet()
                setNodeIsMainnet(context, isMainnet)
                nodeInfo.testRpcService()
                return true
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(result: Boolean) {
                showTestResult()

                testProgressAction=false
                if (shutdown) {
                    if (nodeBackup == null) {
                        nodeViewModel.addNode(nodeInfo)
                    } else {
                        nodeViewModel.setNodes(nodeInfo as Collection<NodeInfo>)
                    }
                }
            }
        }.execute()
    }

    fun test() {
        if (applyChanges()) {
            asyncTestNode()
        } else {
            testProgressAction=false
        }
    }

    fun testNode() {
        testProgressAction=true
        test()

    }

    fun apply() {
        if (applyChanges()) {
            onDismiss()
            if (nodeBackup == null) { // this is a (FAB) new node
                nodeInfo.isFavourite=true
                //Need to add list value
                nodeViewModel.favouritesNodes.value!!.add(nodeInfo)
                nodeViewModel.saveNodes(nodeViewModel.favouritesNodes.value!!)
            }
            shutdown=true
            asyncTestNode()
        }
    }

    fun addNode() {
        if (!getNodeIsTested(context) && nodeStatus == "") {
            Toast.makeText(context, context.getString(R.string.make_sure_you_test_the_node_before_adding_it), Toast.LENGTH_SHORT).show()
        } else if (nodeStatus == context.getString(R.string.node_general_error)) {
            Toast.makeText(context, context.getString(R.string.unable_to_connect_test_failed), Toast.LENGTH_SHORT).show()
        } else if (nodeStatus == context.getString(R.string.add_node_success)) {
            if (getNodeIsMainnet(context)) {
                apply()
                setNodeIsMainnet(context, false)
            } else {
                Toast.makeText(context, context.getString(R.string.please_add_a_mainnet_node), Toast.LENGTH_SHORT).show()
            }
        }
    }


    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest=onDismiss,
    ) {

        OutlinedCard(
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
                elevation=CardDefaults.cardElevation(defaultElevation=4.dp)) {
            Column(verticalArrangement=Arrangement.Center, modifier=Modifier
                .fillMaxWidth()
                .padding(10.dp)) {

                Text(text= stringResource(id = R.string.node_fab_add), modifier=Modifier
                    .fillMaxWidth()
                    .padding(top=10.dp, bottom=5.dp, start=10.dp),
                        style=MaterialTheme.typography.bodyLarge.copy(
                                fontSize=16.sp,
                                fontWeight=FontWeight(700),
                                color=MaterialTheme.appColors.secondaryContentColor),
                        textAlign=TextAlign.Center)

                TextField(
                        value=nodeAddress,
                        placeholder={
                            Text(text=stringResource(R.string.node_address),
                                    style=MaterialTheme.typography.bodyMedium,
                                    color=MaterialTheme.appColors.addNodeHintColor)
                        },
                        onValueChange={
                            nodeAddress=it
                            nodeStatusSuccessAction=false
                            nodeStatusErrorAction=false
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        colors=TextFieldDefaults.colors(
                                unfocusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedIndicatorColor=Color.Transparent,
                                unfocusedIndicatorColor=Color.Transparent,
                                disabledIndicatorColor=Color.Transparent,
                                selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                                cursorColor=MaterialTheme.appColors.textColor),
                        textStyle=TextStyle(
                                color=MaterialTheme.appColors.textColor,
                                fontSize=13.sp,
                                fontWeight=FontWeight(400)),
                        modifier=Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .border(
                                1.dp,
                                MaterialTheme.appColors.textFiledBorderColor,
                                shape=RoundedCornerShape(12.dp)
                            )
                )
                if (nodeAddressErrorAction) {
                    Text(
                            text=nodeAddressErrorText,
                            modifier=Modifier
                                    .padding(start=20.dp),
                            style=MaterialTheme.typography.bodyLarge.copy(
                                    color=MaterialTheme.appColors.errorMessageColor,
                                    fontSize=13.sp,
                                    fontWeight=FontWeight(400),
                            ),
                            textAlign=TextAlign.Start
                    )
                }

                TextField(
                        value=nodePort,
                        placeholder={
                            Text(text=stringResource(R.string.node_port),
                                    style=MaterialTheme.typography.bodyMedium,
                                    color=MaterialTheme.appColors.addNodeHintColor)
                        },
                        onValueChange={
                            if(it.isDigitsOnly()) {
                                nodePort = it
                                nodeStatusSuccessAction=false
                                nodeStatusErrorAction=false
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        colors=TextFieldDefaults.colors(
                                unfocusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedIndicatorColor=Color.Transparent,
                                unfocusedIndicatorColor=Color.Transparent,
                                disabledIndicatorColor=Color.Transparent,
                                selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                                cursorColor=MaterialTheme.appColors.textColor),
                        textStyle=TextStyle(
                                color=MaterialTheme.appColors.textColor,
                                fontSize=13.sp,
                                fontWeight=FontWeight(400)),
                        modifier=Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .border(
                                1.dp,
                                MaterialTheme.appColors.textFiledBorderColor,
                                shape=RoundedCornerShape(12.dp)
                            ),
                )
                if (nodePortErrorAction) {
                    Text(
                            text=nodePortErrorText,
                            modifier=Modifier
                                    .padding(start=20.dp),
                            style=MaterialTheme.typography.bodyLarge.copy(
                                    color=MaterialTheme.appColors.errorMessageColor,
                                    fontSize=13.sp,
                                    fontWeight=FontWeight(400),
                            ),
                            textAlign=TextAlign.Start
                    )
                }


                TextField(value=nodeName, placeholder={
                    Text(text=stringResource(R.string.node_name),
                            style=MaterialTheme.typography.bodyMedium,
                            color=MaterialTheme.appColors.addNodeHintColor)
                }, onValueChange={
                    nodeName=it

                }, modifier=Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.textFiledBorderColor,
                        shape=RoundedCornerShape(12.dp)
                    ),
                        singleLine = true,
                        colors=TextFieldDefaults.colors(
                                unfocusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedIndicatorColor=Color.Transparent,
                                unfocusedIndicatorColor=Color.Transparent,
                                disabledIndicatorColor=Color.Transparent,
                                selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                                cursorColor=MaterialTheme.appColors.textColor),
                        textStyle=TextStyle(
                                color=MaterialTheme.appColors.primaryButtonColor,
                                fontSize=13.sp,
                                fontWeight=FontWeight(400))

                )

                TextField(value=nodeUserName, placeholder={
                    Text(text=stringResource(R.string.node_login),
                            style=MaterialTheme.typography.bodyMedium,
                            color=MaterialTheme.appColors.addNodeHintColor)
                }, onValueChange={
                    nodeUserName=it

                }, modifier=Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.textFiledBorderColor,
                        shape=RoundedCornerShape(12.dp)
                    ),
                        singleLine = true,
                        colors=TextFieldDefaults.colors(
                                unfocusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedIndicatorColor=Color.Transparent,
                                unfocusedIndicatorColor=Color.Transparent,
                                disabledIndicatorColor=Color.Transparent,
                                selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                                cursorColor=MaterialTheme.appColors.textColor),
                        textStyle=TextStyle(
                                color=MaterialTheme.appColors.textColor,
                                fontSize=13.sp,
                                fontWeight=FontWeight(400))

                )

                TextField(value=nodePassword, placeholder={
                    Text(text=stringResource(R.string.node_password),
                            style=MaterialTheme.typography.bodyMedium,
                            color=MaterialTheme.appColors.addNodeHintColor)
                }, onValueChange={
                    nodePassword=it

                }, modifier=Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.textFiledBorderColor,
                        shape=RoundedCornerShape(12.dp)
                    ),
                        singleLine = true,
                        colors=TextFieldDefaults.colors(
                                unfocusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedContainerColor=MaterialTheme.appColors.beldexAddressBackground,
                                focusedIndicatorColor=Color.Transparent,
                                unfocusedIndicatorColor=Color.Transparent,
                                disabledIndicatorColor=Color.Transparent,
                                selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                                cursorColor=MaterialTheme.appColors.textColor),
                        textStyle=TextStyle(
                                color=MaterialTheme.appColors.textColor,
                                fontSize=13.sp,
                                fontWeight=FontWeight(400))

                )

                Row(
                        horizontalArrangement=Arrangement.Start,
                        verticalAlignment=Alignment.CenterVertically,
                        modifier=Modifier.align(Alignment.Start)

                ) {


                    Button(
                            onClick={
                                testNode()
                                nodeStatusSuccessAction=false
                                nodeStatusErrorAction=false

                            },
                            colors=ButtonDefaults.buttonColors(
                                    containerColor=MaterialTheme.appColors.secondaryButtonColor
                            ),
                            border=BorderStroke(
                                    width=2.dp,
                                    color=MaterialTheme.appColors.primaryButtonColor
                            ),
                            modifier=Modifier
                                    .padding(horizontal=10.dp)
                    ) {
                        Text(
                                text=stringResource(id=R.string.test),
                                style=MaterialTheme.typography.titleMedium.copy(
                                        color= MaterialTheme.appColors.onMainContainerTextColor,
                                        fontSize=14.sp,
                                        fontWeight=FontWeight(400)
                                ),
                                modifier=Modifier.padding(horizontal=10.dp)
                        )
                    }
                    if(testProgressAction) {
                        CircularProgressIndicator(
                                modifier=Modifier
                                    .height(16.dp)
                                    .width(16.dp),
                                color=MaterialTheme.appColors.primaryButtonColor,
                                strokeWidth=2.dp
                        )
                    }
                    if (nodeStatusSuccessAction) {
                        Text(text=nodeStatus, modifier=Modifier,
                                style=MaterialTheme.typography.bodyLarge.copy(
                                        fontSize=12.sp,
                                        fontWeight=FontWeight(700),
                                        color=MaterialTheme.appColors.primaryButtonColor),
                                textAlign=TextAlign.Center
                        )

                        Icon(
                                painter=painterResource(id=R.drawable.ic_node_succcess),
                                contentDescription="",
                                tint=MaterialTheme.appColors.primaryButtonColor,
                                modifier=Modifier
                                        .padding(horizontal=5.dp))
                    }
                    if (nodeStatusErrorAction) {
                        Text(text=nodeStatus, modifier=Modifier,
                                style=MaterialTheme.typography.bodyLarge.copy(
                                        fontSize=12.sp,
                                        fontWeight=FontWeight(700),
                                        color=MaterialTheme.appColors.errorMessageColor),
                                textAlign=TextAlign.Center
                        )
                        Icon(
                                painter=painterResource(id=R.drawable.ic_connection_error),
                                contentDescription="",
                                tint=MaterialTheme.appColors.errorMessageColor,
                                modifier=Modifier
                                        .padding(horizontal=5.dp))
                    }


                }

                Row(
                        modifier=Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                ) {
                    Button(
                        onClick={ onDismiss() },
                        shape = RoundedCornerShape(12.dp),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.negativeGreenButton,
                            contentColor = MaterialTheme.appColors.negativeGreenButtonText
                        ),
                        modifier=Modifier
                                .weight(1f),
                        border = BorderStroke(width = 0.5.dp, color = MaterialTheme.appColors.negativeGreenButtonBorder)
                    ) {
                        Text(
                                text=stringResource(id=R.string.cancel),
                                style=MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight(400),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.appColors.negativeGreenButtonText
                                ),
                                modifier=Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier=Modifier.width(16.dp))

                    Button(
                         onClick={
                             addNode()
                         },
                         enabled = nodeStatusSuccessAction,
                         shape = RoundedCornerShape(12.dp),
                         colors=ButtonDefaults.buttonColors(
                             containerColor=MaterialTheme.appColors.primaryButtonColor,
                             disabledContainerColor = MaterialTheme.colorScheme.primary,
                             disabledContentColor = MaterialTheme.appColors.disableAddButtonContainer
                         ),
                         modifier=Modifier
                                 .weight(1f)
                    ) {
                        Text(
                                text= stringResource(id = R.string.add),
                                style=MaterialTheme.typography.bodyMedium.copy(
                                    color=if(nodeStatusSuccessAction)Color.White else MaterialTheme.appColors.disableAddButtonContent,
                                    fontWeight = FontWeight(400),
                                    fontSize = 12.sp
                                ),
                                modifier=Modifier.padding(10.dp)
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun SwitchNodePopUp(onDismiss: () -> Unit, nodeViewModel: NodeViewModel, nodeInfo: NodeInfo) {

    val context=LocalContext.current

    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest=onDismiss,
    ) {

        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground), elevation=CardDefaults.cardElevation(defaultElevation=4.dp), modifier=Modifier.fillMaxWidth()) {
            Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center, modifier=Modifier
                .fillMaxWidth()
                .padding(10.dp)) {

                Text(
                    text=stringResource(id=R.string.switch_node),
                    textAlign=TextAlign.Center,
                    style=MaterialTheme.typography.titleMedium.copy(
                        fontSize=16.sp,
                        fontWeight=FontWeight(700),
                        color=MaterialTheme.appColors.textColor),
                    modifier=Modifier.padding(top =20.dp, start = 40.dp, end = 40.dp))

                Text(
                    text=stringResource(id=R.string.switch_node_alert),
                    textAlign=TextAlign.Center,
                    style=MaterialTheme.typography.titleMedium.copy(
                            fontSize=14.sp,
                            fontWeight=FontWeight(400),
                            color=MaterialTheme.appColors.textColor),
                    modifier=Modifier.padding(top =10.dp, bottom = 20.dp, start =40.dp, end = 40.dp))

                Row(
                        modifier=Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                ) {
                    Button(
                            onClick={ onDismiss() },
                        colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.negativeGreenButton
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(width = 0.5.dp, color = MaterialTheme.appColors.negativeGreenButtonBorder),
                        modifier=Modifier
                                .weight(1f)
                    ) {
                        Text(
                            text=stringResource(id=R.string.cancel),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.appColors.negativeGreenButtonText,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp
                            ),
                            modifier=Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier=Modifier.width(16.dp))

                    Button(
                        onClick={
                            nodeInfo.isFavourite=true
                            // Need to check
                            nodeViewModel.setFavouriteNodes(nodeViewModel.favouritesNodes.value, context)
                            AsyncTask.execute {
                                nodeViewModel.setNode(nodeInfo, true, context)
                                nodeInfo.isSelecting=true
                                changeDaemon(context, true)
                            }
                            onDismiss()

                        },
                        colors=ButtonDefaults.buttonColors(
                                containerColor=MaterialTheme.appColors.primaryButtonColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier=Modifier
                                    .weight(1f)
                    ) {
                        Text(
                            text= stringResource(id = R.string.yes),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                color=Color.White,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp
                            ),
                            modifier=Modifier.padding(10.dp)
                        )
                    }
                }

            }
        }
    }

}


private class NodeDiff(oldList: MutableList<NodeInfo>, newList: List<NodeInfo?>?) : DiffCallback<NodeInfo?>(oldList as List<NodeInfo?>?, newList) {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldList[oldItemPosition] == mNewList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem=mOldList[oldItemPosition]!!
        val newItem=mNewList[newItemPosition]!!
        return oldItem.timestamp == newItem.timestamp
                && oldItem.isTested == newItem.isTested
                && oldItem.isValid == newItem.isValid
                && oldItem.responseTime == newItem.responseTime
                && oldItem.isSelected == newItem.isSelected
                && oldItem.name == newItem.name
    }
}


@Composable
private fun NodeScreenContainer(
        title: String,
        wrapInCard: Boolean=true,
        onBackClick: () -> Unit,
        actionItems: @Composable () -> Unit={},
        content: @Composable () -> Unit,
) {
    Column(
            modifier=Modifier
                    .fillMaxSize()
    ) {
        Row(
                verticalAlignment=Alignment.CenterVertically,
                modifier=Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {
            Icon(
                    painterResource(id = R.drawable.ic_back_arrow),
                    contentDescription=stringResource(R.string.back),
                    tint=MaterialTheme.appColors.editTextColor,
                    modifier=Modifier
                            .clickable {
                                onBackClick()
                            }
            )

            Spacer(modifier=Modifier.width(16.dp))

            Text(
                    text=title,
                    style=MaterialTheme.typography.titleLarge.copy(
                            color=MaterialTheme.appColors.editTextColor,
                            fontWeight=FontWeight.Bold,
                            fontSize=18.sp
                    ),
                    modifier=Modifier
                            .weight(1f)
            )

            actionItems()
        }

        Spacer(modifier=Modifier.height(24.dp))

        if (wrapInCard) {
            CardContainer(
                    modifier=Modifier
                        .fillMaxWidth()
                        .weight(1f)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}


private var favouriteNodeSet: MutableSet<NodeInfo> = HashSet()
private const val pingSelected=0
private const val prefDaemonTestNet="daemon_testnet"
private const val prefDaemonStageNet="daemon_stagenet"
private const val prefDaemonMainNet="daemon_mainnet"

private fun pingSelectedNode(context: Context, nodeViewModel: NodeViewModel) {
    nodeViewModel.asyncFindBestNode(context, pingSelected)
}


/*
@Preview(uiMode=Configuration.UI_MODE_NIGHT_NO)
@Composable
fun NodeScreenPreview() {
    NodeScreen()
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NodeScreenPreviewDark() {
    NodeScreen()
}*/
