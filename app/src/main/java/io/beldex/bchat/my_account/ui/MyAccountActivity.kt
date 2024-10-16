package io.beldex.bchat.my_account.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.copper.flow.observeQuery
import com.beldex.libbchat.avatars.AvatarHelper
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfileKeyUtil
import com.beldex.libbchat.utilities.ProfilePictureUtilities
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.contacts.blocked.BlockedContactsViewModel
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.database.DatabaseContentProviders
import io.beldex.bchat.messagerequests.MessageRequestsViewModel
import io.beldex.bchat.my_account.ui.dialogs.BNSNameVerifySuccessDialog
import io.beldex.bchat.my_account.ui.dialogs.ClearDataDialog
import io.beldex.bchat.my_account.ui.dialogs.CopyContentDialog
import io.beldex.bchat.my_account.ui.dialogs.LinkYourBNSDialog
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.preferences.ChatSettingsActivity
import io.beldex.bchat.util.FileProviderUtil
import io.beldex.bchat.util.QRCodeUtilities
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.copyToClipBoard
import io.beldex.bchat.util.toPx
import io.beldex.bchat.wallet.jetpackcomposeUI.StatWalletInfo
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.avatar.AvatarSelection
import io.beldex.bchat.compose_utils.checkAndRequestPermissions
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.my_account.ui.dialogs.ProfilePicturePopup
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.profiles.ProfileMediaConstraints
import io.beldex.bchat.util.BitmapDecodingException
import io.beldex.bchat.util.BitmapUtil
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.wallet.CheckOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.successUi
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Date
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class MyAccountActivity : ComponentActivity() {

    private var destination = MyAccountScreens.SettingsScreen.route
    @Inject
    lateinit var groupDb: GroupDatabase
    private var tempFile: File? = null
    val TAG = "MyAccountActivity"

    val viewModel: MyAccountViewModel by viewModels()


    private val onAvatarCropped = registerForActivityResult(CropImageContract()) { result ->
        when {
            result.isSuccessful -> {
                Log.i(TAG, result.getUriFilePath(this).toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val profilePictureToBeUploaded =
                            BitmapUtil.createScaledBytes(
                                this@MyAccountActivity,
                                result.getUriFilePath(this@MyAccountActivity).toString(),
                                ProfileMediaConstraints()
                            ).bitmap
                        launch(Dispatchers.Main) {
                            TextSecurePreferences.setIsLocalProfile(this@MyAccountActivity,false)
                            updateProfile(true,profilePictureToBeUploaded)
                        }
                    } catch (e: BitmapDecodingException) {
                        Log.e(TAG, e)
                    }
                }
            }
            result is CropImage.CancelledResult -> {
                Log.i(TAG, "Cropping image was cancelled by the user")
            }
            else -> {
                Log.e(TAG, "Cropping image failed")
            }
        }
    }
    private val onPickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
        val inputFile: Uri? = result.data?.data ?: tempFile?.let(Uri::fromFile)
        cropImage(inputFile, outputFile)
    }

    private val avatarSelection = AvatarSelection(this, onAvatarCropped, onPickImage)

    private fun cropImage(inputFile: Uri?, outputFile: Uri?){
        avatarSelection.circularCropImage(
            inputFile = inputFile,
            outputFile = outputFile,
        )

    }

    private fun updateProfile(isUpdatingProfilePicture: Boolean, profilePicture: ByteArray? = null,
                              displayName: String? = null) {
        val promises = mutableListOf<Promise<*, Exception>>()
        if (displayName != null) {
            TextSecurePreferences.setProfileName(this, displayName)
            viewModel.refreshProfileName()
        }
        val encodedProfileKey = ProfileKeyUtil.generateEncodedProfileKey(this)
        if (isUpdatingProfilePicture) {
            if (profilePicture != null) {
                promises.add(ProfilePictureUtilities.upload(profilePicture, encodedProfileKey, this))
            } else {
                TextSecurePreferences.setLastProfilePictureUpload(this, System.currentTimeMillis())
                TextSecurePreferences.setProfilePictureURL(this, null)
            }
        }
        val compoundPromise = all(promises)
        compoundPromise.successUi { // Do this on the UI thread so that it happens before the alwaysUi clause below
            if (isUpdatingProfilePicture) {
                AvatarHelper.setAvatar(
                    this,
                    Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)!!),
                    profilePicture
                )
                TextSecurePreferences.setProfileAvatarId(this,profilePicture?.let { SecureRandom().nextInt() } ?: 0)
                TextSecurePreferences.setLastProfilePictureUpload(this, Date().time)
                ProfileKeyUtil.setEncodedProfileKey(this, encodedProfileKey)
            }
            if (profilePicture != null || displayName != null) {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this)
            }
        }
        compoundPromise.alwaysUi {
            if (isUpdatingProfilePicture) {
                viewModel.updateProfile(true)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination =
            intent?.getStringExtra(extraStartDestination) ?: MyAccountScreens.SettingsScreen.route
        setContent {
            BChatTheme(
                darkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        val navController = rememberNavController()
                        MyAccountNavHost(
                            navController = navController,
                            startDestination = destination,
                            groupDatabase = groupDb,
                            startAvatarSelection = {
                                startAvatarSelection()
                            },
                            modifier = Modifier
                                .padding(it)
                        )
                    }
                }
            }
        }
    }

    private fun startAvatarSelection() {
        if (CheckOnline.isOnline(this)) {
            Permissions.with(this)
                .request(Manifest.permission.CAMERA)
                .onAnyResult {
                    tempFile=avatarSelection.startAvatarSelection(false, true)
                }
                .execute()
        } else {
            Toast.makeText(
                this,
                getString(R.string.please_check_your_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val extraStartDestination = "io.beldex.EXTRA_START_DESTINATION"
    }
}

@Composable
fun MyAccountNavHost(
    navController: NavHostController,
    startDestination: String,
    groupDatabase : GroupDatabase,
    startAvatarSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val startActivity: (Intent) -> Unit = {
        context.startActivity(it)
    }
    val archiveChatViewModel: ArchiveChatViewModel = hiltViewModel()
    val viewModel: MyAccountViewModel = hiltViewModel()
    val requiredPermission = arrayOf(Manifest.permission.CAMERA)
    var showPermissionDialog by remember {
        mutableStateOf(false)
    }
    var isProfileChanged by remember {
        mutableStateOf(false)
    }
    viewModel.isProfileChanged.observe(lifecycleOwner) { changed ->
        isProfileChanged = changed
    }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.all { it.value }) {

        } else {
            showPermissionDialog = true
        }
    }

     fun updateProfile(isUpdatingProfilePicture: Boolean, profilePicture: ByteArray? = null,
                              displayName: String? = null, context : Context) {
         /* binding.loader.isVisible = true
        lifecycleScope.launch {

            delay(3000)
            binding.loader.isVisible = false
        }*/
        val promises = mutableListOf<Promise<*, Exception>>()
        if (displayName != null) {
            TextSecurePreferences.setProfileName(context, displayName)
            viewModel.refreshProfileName()
        }
        val encodedProfileKey = ProfileKeyUtil.generateEncodedProfileKey(context)
        if (isUpdatingProfilePicture) {
            if (profilePicture != null) {
                promises.add(ProfilePictureUtilities.upload(profilePicture, encodedProfileKey, context))
            } else {
                TextSecurePreferences.setLastProfilePictureUpload(context, System.currentTimeMillis())
                TextSecurePreferences.setProfilePictureURL(context, null)
            }
        }
        val compoundPromise = all(promises)
        compoundPromise.successUi { // Do this on the UI thread so that it happens before the alwaysUi clause below
            if (isUpdatingProfilePicture) {
                AvatarHelper.setAvatar(
                    context,
                    Address.fromSerialized(TextSecurePreferences.getLocalNumber(context)!!),
                    profilePicture
                )
                TextSecurePreferences.setProfileAvatarId(context,profilePicture?.let { SecureRandom().nextInt() } ?: 0)
                TextSecurePreferences.setLastProfilePictureUpload(context, Date().time)
                ProfileKeyUtil.setEncodedProfileKey(context, encodedProfileKey)
            }
            if (profilePicture != null || displayName != null) {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(context)
            }
        }
    }

     fun saveDisplayName(displayName: String, context : Context): Boolean {
         val namePattern = Pattern.compile("[A-Za-z0-9\\s]+")
        if (displayName.isEmpty()) {
            Toast.makeText(
                context,
                R.string.activity_settings_display_name_missing_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (displayName.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            Toast.makeText(
                context,
                R.string.activity_settings_display_name_too_long_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (!displayName.matches(namePattern.toRegex())) {
            Toast.makeText(
                context,
                R.string.display_name_validation,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val checkGalleryProfile = TextSecurePreferences.getIsLocalProfile(context)
        updateProfile(checkGalleryProfile,null, displayName = displayName, context)
         //viewModel.refreshProfileName()
        return true
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
//        composable(
//            route = MyAccountScreens.MyAccountScreen.route
//        ) {
//            val uiState by viewModel.uiState.collectAsState()
//            MyAccountScreenContainer(
//                title = stringResource(R.string.my_account),
//                onBackClick = {
//                    (context as ComponentActivity).finish()
//                }
//            ) {
//                MyAccountScreen(
//                    uiState = uiState
//                )
//            }
//        }

        composable(
            route = MyAccountScreens.SettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.account_settings),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                val state by viewModel.uiState.collectAsState()
                val scrollState = rememberScrollState()
                val isDarkMode = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
                val beldexAddress by remember {
                    mutableStateOf(
                        IdentityKeyUtil.retrieve(
                            context,
                            IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF
                        )
                    )
                }
                val copyToClipBoard: (String, String) -> Unit = { label, content ->
                    context.copyToClipBoard(label, content)
                }
                var showClearDataDialog by remember {
                    mutableStateOf(false)
                }
                var showBeldexAddressDialog by remember {
                    mutableStateOf(false)
                }
                var showQRDialog by remember {
                    mutableStateOf(false)
                }
                var showBChatIdDialog by remember {
                    mutableStateOf(false)
                }
                var shareButtonLastClickTime by remember {
                    mutableLongStateOf(0)
                }
                var showLinkYourBnsDialog by remember {
                    mutableStateOf(false)
                }
                var isBnsHolder by remember {
                    mutableStateOf(TextSecurePreferences.getIsBNSHolder(context))
                }
                var showBnsNameVerifySuccessDialog by remember{
                    mutableStateOf(false)
                }
                var showEditNameTextField by remember {
                    mutableStateOf(false)
                }
                var showNameOnly by remember {
                    mutableStateOf(true)
                }
                var saveEditName by remember {
                    mutableStateOf(state.profileName)
                }

                var showPictureDialog by remember {
                    mutableStateOf(false)
                }

                if (showClearDataDialog) {
                    ClearDataDialog {
                        showClearDataDialog = false
                    }
                }

                if (showBeldexAddressDialog) {
                    CopyContentDialog(
                        title = stringResource(id = R.string.beldex_address),
                        data = beldexAddress,
                        onCopy = {
                            copyToClipBoard("Beldex Address", beldexAddress)
                            showBeldexAddressDialog = false
                        },
                        onDismissRequest = {
                            showBeldexAddressDialog = false
                        })
                }

                if (showBChatIdDialog) {
                    CopyContentDialog(
                        title = stringResource(id = R.string.chatid),
                        data = state.publicKey,
                        onCopy = {
                            copyToClipBoard("BChat ID", state.publicKey)
                            showBChatIdDialog = false
                        },
                        onDismissRequest = {
                            showBChatIdDialog = false
                        })
                }

                if (showQRDialog) {
                    ShowQRDialog(
                        title = stringResource(id = R.string.scan_qr_code),
                        uiState = state,
                        onShare = {
                            showQRDialog = false
                            if (SystemClock.elapsedRealtime() - shareButtonLastClickTime >= 1000) {
                                shareButtonLastClickTime = SystemClock.elapsedRealtime()
                                val directory =
                                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                val fileName = "${state.publicKey}.png"
                                val file = File(directory, fileName)
                                file.createNewFile()
                                val fos = FileOutputStream(file)
                                val size = toPx(280, context.resources)
                                val qrCode = QRCodeUtilities.encode(
                                    state.publicKey,
                                    size,
                                    isInverted = false,
                                    hasTransparentBackground = false
                                )
                                qrCode.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                fos.flush()
                                fos.close()
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.putExtra(
                                    Intent.EXTRA_STREAM,
                                    FileProviderUtil.getUriFor(context, file)
                                )
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.type = "image/png"
                                startActivity(
                                    Intent.createChooser(
                                        intent,
                                        context.resources.getString(R.string.fragment_view_my_qr_code_share_title)
                                    )
                                )
                            }
                        },
                        onDismissRequest = {
                            showQRDialog = false
                        }
                    )
                }

                if(showLinkYourBnsDialog){
                    LinkYourBNSDialog(state, onDismissRequest = {
                        showLinkYourBnsDialog = false
                        if(it){
                            isBnsHolder = TextSecurePreferences.getIsBNSHolder(context)
                            showBnsNameVerifySuccessDialog = true
                        }
                    })
                }

                if(showBnsNameVerifySuccessDialog){
                    BNSNameVerifySuccessDialog {
                        showBnsNameVerifySuccessDialog = false
                    }
                }
                fun checkForPermission() {
                    // Ask for an optional camera permission.
                    if (CheckOnline.isOnline(context)) {
                        checkAndRequestPermissions(
                            context = context,
                            permissions = requiredPermission,
                            launcher = launcher,
                            onGranted = {
                                startAvatarSelection()
                            }
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.please_check_your_internet_connection),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                if (showPictureDialog) {
                    ProfilePicturePopup(
                        publicKey = state.publicKey,
                        displayName = state.profileName ?: "",
                        onDismissRequest = {
                            showPictureDialog = false
                        },
                        closePopUP = {
                            showPictureDialog = false
                        },
                        removePicture = {
                            showPictureDialog = false
                        },
                        uploadPicture = {
                            showPictureDialog = false
                            checkForPermission()
                            //startAvatarSelection
                        }
                    )
                }

                fun Modifier.innerShadow(
                    color: Color = Color.Black,
                    cornersRadius: Dp = 0.dp,
                    spread: Dp = 0.dp,
                    blur: Dp = 0.dp,
                    offsetY: Dp = 0.dp,
                    offsetX: Dp = 0.dp
                ) = drawWithContent {

                    drawContent()

                    val rect = Rect(Offset.Zero, size)
                    val paint = Paint()

                    drawIntoCanvas {

                        paint.color = color
                        paint.isAntiAlias = true
                        it.saveLayer(rect, paint)
                        it.drawRoundRect(
                            left = rect.left,
                            top = rect.top,
                            right = rect.right,
                            bottom = rect.bottom,
                            cornersRadius.toPx(),
                            cornersRadius.toPx(),
                            paint
                        )
                        val frameworkPaint = paint.asFrameworkPaint()
                        frameworkPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                        if (blur.toPx() > 0) {
                            frameworkPaint.maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        val left = if (offsetX > 0.dp) {
                            rect.left + offsetX.toPx()
                        } else {
                            rect.left
                        }
                        val top = if (offsetY > 0.dp) {
                            rect.top + offsetY.toPx()
                        } else {
                            rect.top
                        }
                        val right = if (offsetX < 0.dp) {
                            rect.right + offsetX.toPx()
                        } else {
                            rect.right
                        }
                        val bottom = if (offsetY < 0.dp) {
                            rect.bottom + offsetY.toPx()
                        } else {
                            rect.bottom
                        }
                        paint.color = Color.Black
                        it.drawRoundRect(
                            left = left + spread.toPx() / 2,
                            top = top + spread.toPx() / 2,
                            right = right - spread.toPx() / 2,
                            bottom = bottom - spread.toPx() / 2,
                            cornersRadius.toPx(),
                            cornersRadius.toPx(),
                            paint
                        )
                        frameworkPaint.xfermode = null
                        frameworkPaint.maskFilter = null
                    }
                }

                fun callAboutBns(){
                    navController.navigate(MyAccountScreens.AboutBNSScreen.route)
                }

                Column(
                    modifier =Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier =Modifier
                                .padding(start=24.dp, top=16.dp, end=0.dp, bottom=16.dp)
                                .background(
                                    color=if (showEditNameTextField) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.listItemBackground,
                                    shape=RoundedCornerShape(16.dp)
                                )
                                .align(Alignment.TopEnd)
                                .clickable {
                                    if (!showEditNameTextField) {
                                        showNameOnly=false
                                        showEditNameTextField=true
                                    } else {
                                        showNameOnly=true
                                        showEditNameTextField=false
                                        saveEditName?.let { it1 -> saveDisplayName(it1, context) }
                                    }
                                }
                            ,
                            contentAlignment = Alignment.Center

                        ){
                            Text(
                                text = if(showEditNameTextField) stringResource(id=R.string.menu_done_button) else stringResource(id=R.string.edit_title),
                                style = BChatTypography.bodySmall.copy(
                                    color = if(showEditNameTextField) Color.White  else MaterialTheme.appColors.primaryButtonColor,
                                    fontWeight = FontWeight(600),
                                    fontSize = 12.sp,
                                ),
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)

                            )
                        }

                        Box(
                            modifier = if(!isBnsHolder.isNullOrEmpty()) Modifier
                                .fillMaxWidth()
                                .padding(top=50.dp, bottom=10.dp)
                                .paint(
                                    painterResource(id=if (isDarkMode) R.drawable.ic_bns_card_dark else R.drawable.ic_bns_card_light),
                                    contentScale=ContentScale.FillBounds
                                )
                                .innerShadow(
                                    color=if (isDarkMode) MaterialTheme.appColors.primaryButtonColor else Color(
                                        0x8000BD40
                                    ), blur=if (isDarkMode) 20.dp else 10.dp, cornersRadius=16.dp
                                ) else Modifier
                                .fillMaxWidth()
                                .padding(top=50.dp, bottom=10.dp)
                                .background(
                                    color=MaterialTheme.appColors.listItemBackground,
                                    shape=RoundedCornerShape(16.dp)
                                )
                        ) {
                            ProfileCard(
                                isBnsHolder = isBnsHolder,
                                uiState = state,
                                beldexAddress = beldexAddress,
                                modifier =Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top=40.dp
                                    ),
                                onShowDialog = {
                                    when (it) {
                                        0 ->
                                            showBeldexAddressDialog = true

                                        1 ->
                                            showBChatIdDialog = true

                                        else ->
                                            showQRDialog = true
                                    }
                                },
                                showEditNameTextField,
                                showNameOnly,
                                saveDisplayName ={
                                    saveEditName = it
                                }
                            )
                        }
                        Box(
                            contentAlignment=Alignment.CenterEnd,
                            modifier=Modifier.align(alignment=Alignment.TopCenter)
                        ) {
                            if(isProfileChanged) {
                                ProfilePictureComponent(
                                    publicKey = state.publicKey,
                                    displayName = state.profileName ?: state.publicKey,
                                    containerSize = ProfilePictureMode.LargePicture.size,
                                    pictureMode = ProfilePictureMode.LargePicture,
                                    modifier = Modifier.align(alignment = Alignment.TopCenter)
                                )
                                isProfileChanged = false
                            }else{
                                ProfilePictureComponent(
                                    publicKey = state.publicKey,
                                    displayName = state.profileName ?: state.publicKey,
                                    containerSize = ProfilePictureMode.LargePicture.size,
                                    pictureMode = ProfilePictureMode.LargePicture,
                                    modifier = Modifier.align(alignment = Alignment.TopCenter)
                                )
                            }
                            if (showEditNameTextField) {
                                Box(
                                    contentAlignment=Alignment.Center,
                                    modifier=Modifier
                                        .padding(start=150.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            color=MaterialTheme.appColors.backgroundColor
                                        )
                                        .clickable {
                                            showPictureDialog=true

                                        }
                                ) {
                                    Icon(
                                        painterResource(id=R.drawable.ic_camera_edit),
                                        contentDescription="",
                                        tint=MaterialTheme.appColors.editTextColor,
                                        modifier=Modifier
                                            .size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if(isBnsHolder.isNullOrEmpty()) {
                        PrimaryButton(
                            onClick = {
                                showLinkYourBnsDialog = true
                            },
                            modifier =Modifier
                                .fillMaxWidth()
                                .padding(bottom=10.dp),
                            shape = RoundedCornerShape(12.dp),
                            disabledContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.bns_transaction),
                                    contentDescription = ""
                                )
                                Text(
                                    text = stringResource(R.string.link_your_bns),
                                    style = BChatTypography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight(600),
                                    ),
                                    modifier = Modifier
                                        .padding(start = 5.dp)
                                )
                            }
                        }
                        Row(
                            modifier =Modifier
                                .fillMaxWidth()
                                .padding(bottom=10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.read_more_about_bns),
                                style = BChatTypography.titleSmall.copy(
                                    color = MaterialTheme.appColors.secondaryTextColor,
                                    fontWeight = FontWeight(400),
                                    fontSize = 12.sp
                                ),
                                modifier =Modifier
                                    .padding(end=5.dp)
                                    .clickable(onClick={
                                        callAboutBns()
                                    })
                            )
                            Icon(
                                painterResource(id = R.drawable.ic_info_outline_dark),
                                contentDescription = "Read more about BNS",
                                tint = MaterialTheme.appColors.secondaryTextColor,
                                modifier =Modifier
                                    .size(12.dp)
                                    .clickable(onClick={
                                        callAboutBns()
                                    })
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.appColors.listItemBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsScreen(
                            navigate = {
                                when (it) {
                                    SettingItem.Hops -> {
//                                Intent(context, PathActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                        viewModel.getPathNodes()
                                        navController.navigate(MyAccountScreens.HopsScreen.route)
                                    }

                                    SettingItem.AppLock -> {
//                                Intent(context, AppLockDetailsActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                        navController.navigate(MyAccountScreens.AppLockScreen.route)
                                    }

                                    SettingItem.ChatSettings -> {
                                        Intent(
                                            context,
                                            ChatSettingsActivity::class.java
                                        ).also { intent ->
                                            startActivity(intent)
                                        }
//                                navController.navigate(MyAccountScreens.ChatSettingsScreen.route)
                                    }

                                    SettingItem.BlockedContacts -> {
//                                Intent(context, BlockedContactsActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                        navController.navigate(MyAccountScreens.BlockedContactScreen.route)
                                    }

                                    SettingItem.ClearData -> {
                                        showClearDataDialog = true
                                    }

                                    SettingItem.Feedback -> {
                                        val intent = Intent(Intent.ACTION_SENDTO)
                                        intent.data =
                                            Uri.parse("mailto:") // only email apps should handle this
                                        intent.putExtra(
                                            Intent.EXTRA_EMAIL,
                                            arrayOf("feedback@beldex.io")
                                        )
                                        intent.putExtra(Intent.EXTRA_SUBJECT, "")
                                        startActivity(intent)
                                    }

                                    SettingItem.FAQ -> {
                                        try {
                                            val url = "https://bchat.beldex.io/faq"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Can't open URL",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                    SettingItem.ChangeLog -> {
//                                Intent(context, ChangeLogActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                        navController.navigate(MyAccountScreens.ChangeLogScreen.route)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        composable(
            route = MyAccountScreens.HopsScreen.route
        ) {
            val nodes by viewModel.pathState.collectAsState()
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_path_title),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                HopsScreen(
                    nodes = nodes
                )
            }
        }

        composable(
            route = MyAccountScreens.ChangeLogScreen.route
        ) {
            val changeLogViewModel: ChangeLogViewModel = hiltViewModel()
            val changeLogs by changeLogViewModel.changeLogs.collectAsState()

            MyAccountScreenContainer(
                title = stringResource(id = R.string.changelog),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                ChangeLogScreen(
                    changeLogs = changeLogs
                )
            }
        }

        composable(
            route = MyAccountScreens.AppLockScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_settings_app_lock_button_title),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                AppLockScreen()
            }
        }

        composable(
            route = MyAccountScreens.ChatSettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.preferences_chats__chats),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                ChatSettingsScreen(
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.BlockedContactScreen.route
        ) {
            val contactViewModel: BlockedContactsViewModel = hiltViewModel()
            var contacts by remember {
                mutableStateOf(emptyList<Recipient>())
            }
            val uiState by contactViewModel.uiState.collectAsState()
            val owner = LocalLifecycleOwner.current
            LaunchedEffect(key1 = Unit) {
                contactViewModel.subscribe(context).observe(owner) { newState ->
                    contacts = newState.blockedContacts
                }
            }
            MyAccountScreenContainer(
                title = stringResource(id = R.string.blocked_contacts),
                onBackClick = {
                    navController.navigateUp()
                },
                actionItems = {
                    if (contacts.isNotEmpty()) {
                        Icon(
                            painter = if (uiState.multiSelectedActivated)
                                painterResource(id = R.drawable.ic_selected_all)
                            else
                                painterResource(id = R.drawable.ic_unselected_all),
                            contentDescription = "",
                            tint = MaterialTheme.appColors.iconTint,
                            modifier = Modifier
                                .clickable {
                                    contactViewModel.onEvent(BlockedContactEvents.MultiSelectClicked)
                                }
                        )
                    }
                }
            ) {
                if (contacts.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_blocked_contact),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    BlockedContactScreen(
                        blockedList = contacts,
                        selectedList = uiState.selectedList,
                        multiSelectActivated = uiState.multiSelectedActivated,
                        unBlockSingleContact = {
                            contactViewModel.onEvent(BlockedContactEvents.UnblockSingleContact(it))
                        },
                        unBlockMultipleContacts = {
                            contactViewModel.onEvent(BlockedContactEvents.UnblockMultipleContact)
                        },
                        addRemoveContactToList = { contact, add ->
                            contactViewModel.onEvent(
                                BlockedContactEvents.AddContactToUnBlockList(
                                    contact,
                                    add
                                )
                            )
                        },
                        modifier =Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        composable(
            route = MyAccountScreens.AboutScreen.route
        ) {
            val aboutViewModel: ContentViewModel = hiltViewModel()
            val content by aboutViewModel.content.collectAsState()
            MyAccountScreenContainer(
                title = stringResource(R.string.about),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                ContentScreen(
                    content = content,
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.RecoverySeedScreen.route
        ) {
            var markedAsSafe by remember {
                mutableStateOf(false)
            }
            val resultLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_OK) {
                        markedAsSafe = true
                    } else {
                        markedAsSafe = false
                        Toast.makeText(context, "Failed to authenticate", Toast.LENGTH_SHORT).show()
                    }
                }
            val verifyPin: () -> Unit = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "onboarding://manage_pin?finish=true&action=${PinCodeAction.VerifyPinCode.action}".toUri()
                )
                resultLauncher.launch(intent)
            }
            val seed by lazy {
                try {
                    var hexEncodedSeed =
                        IdentityKeyUtil.retrieve(context, IdentityKeyUtil.BELDEX_SEED)
                    if (hexEncodedSeed == null) {
                        hexEncodedSeed =
                            IdentityKeyUtil.getIdentityKeyPair(context).hexEncodedPrivateKey // Legacy account
                    }
                    val loadFileContents: (String) -> String = { fileName ->
                        MnemonicUtilities.loadFileContents(context, fileName)
                    }
                    MnemonicCodec(loadFileContents).encode(
                        hexEncodedSeed,
                        MnemonicCodec.Language.Configuration.english
                    )
                } catch (e: Exception) {
                    ""
                }
            }
            MyAccountScreenContainer(
                title = stringResource(R.string.recovery_seed),
                wrapInCard = markedAsSafe,
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                RecoverySeedScreen(
                    seed = seed,
                    markedAsSafe = markedAsSafe,
                    verifyPin = verifyPin,
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(route = MyAccountScreens.StartWalletInfoScreen.route) {
            MyAccountScreenContainer(title = stringResource(id = R.string.wallets), onBackClick = {
                (context as ComponentActivity).finish()
            }) {
                StatWalletInfo(
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

            }

        }

        composable(
            route = MyAccountScreens.MessageRequestsScreen.route
        ) {
            val requestViewModel: MessageRequestsViewModel = hiltViewModel()
            val uiState by requestViewModel.uiState.collectAsState()

            LaunchedEffect(key1 = Unit) {
                launch(Dispatchers.IO) {
                    context.contentResolver
                        .observeQuery(DatabaseContentProviders.ConversationList.CONTENT_URI)
                        .onEach { requestViewModel.refreshRequests() }
                        .collect()
                }
            }
            MyAccountScreenContainer(
                title = stringResource(R.string.activity_message_requests_title),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                MessageRequestsScreen(
                    requestsList = uiState.messageRequests,
                    onEvent = requestViewModel::onEvent,
                    onRequestClick = {
                        val returnIntent = Intent()
                        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, it.threadId)
                        (context as Activity).run {
                            setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            finish()
                        }
                    },
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.AboutBNSScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(R.string.about_bns),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                AboutBNSScreen(
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.ArchiveChatScreen.route
        ) {
            val archiveViewModel: ArchiveChatViewModel = hiltViewModel()
            val uiState by archiveViewModel.uiState.collectAsState()

            LaunchedEffect(key1 = Unit) {
                launch(Dispatchers.IO) {
                    context.contentResolver
                        .observeQuery(DatabaseContentProviders.ConversationList.CONTENT_URI)
                        .onEach { archiveViewModel.refreshContacts() }
                        .collect()
                }
            }
            ArchiveChatScreenContainer(
                title = stringResource(R.string.archive_chat),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                ArchiveChatScreen(
                    requestsList = uiState.archiveChats,
                    onRequestClick = {
                        val returnIntent = Intent()
                        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, it.threadId)
                        (context as Activity).run {
                            setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            finish()
                        }
                    },
                    archiveChatViewModel = archiveChatViewModel,
                    groupDatabase = groupDatabase,
                    modifier =Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileCard(
    isBnsHolder : String?,
    uiState: MyAccountViewModel.UIState,
    beldexAddress: String,
    modifier: Modifier = Modifier,
    onShowDialog: (status: Int) -> Unit,
    onShowEditName: Boolean,
    onShowNameOnly: Boolean,
    saveDisplayName: (String) -> Unit
) {
    val context = LocalContext.current
    val copyToClipBoard: (String, String) -> Unit = { label, content ->
        context.copyToClipBoard(label, content)
    }
    var textFieldValueState by remember {
        mutableStateOf("${uiState.profileName}")
    }
    val savedName by remember {
        mutableStateOf(saveDisplayName)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        if (onShowNameOnly) {
            Text(
                text=uiState.profileName ?: "",
                style=MaterialTheme.typography.titleMedium.copy(
                    color=MaterialTheme.appColors.titleTextColor,
                    fontWeight=FontWeight(700),
                    fontSize=18.sp,
                    lineHeight=24.51.sp
                ),
                textAlign=TextAlign.Center,
                modifier=Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            )
        }
        if (onShowEditName) {
           TextField(
               value= textFieldValueState,
               onValueChange={
                   textFieldValueState=it
                   savedName(it)
               },
               maxLines = 1,
               singleLine = true,
               textStyle = MaterialTheme.typography.bodyMedium.copy(
                   fontSize = 16.sp,
                   fontWeight = FontWeight(400),
                   textAlign = TextAlign.Center
               ),
               colors = TextFieldDefaults.colors(
                   focusedIndicatorColor = MaterialTheme.appColors.textColor,
                   unfocusedIndicatorColor = MaterialTheme.appColors.textColor,
                   selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                   cursorColor = colorResource(id = R.color.button_green)
           )
           )
        }
        if(!isBnsHolder.isNullOrEmpty()){
            Row(
                modifier =Modifier
                    .fillMaxWidth()
                    .padding(top=5.dp, bottom=15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BNS Verified",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.appColors.primaryButtonColor,
                        fontWeight = FontWeight(700),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(end = 5.dp)
                )
                Image(painter = painterResource(id = R.drawable.ic_bns_verified), contentDescription = "Bns verified", modifier = Modifier.size(14.dp))
            }
        }else {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProfileCardKeyContainer(
                isBnsHolder = isBnsHolder,
                title = stringResource(id = R.string.beldex_address),
                image = R.drawable.ic_beldex_logo,
                onCopy = {
                    copyToClipBoard("Beldex Address", beldexAddress)
                },
                isBeldex = true,
                onShowDialog = {
                    onShowDialog(0)
                }
            )

            Spacer(modifier = Modifier.padding(start = 3.dp))

            ProfileCardKeyContainer(
                isBnsHolder = isBnsHolder,
                title = stringResource(id = R.string.chatid),
                image = R.drawable.ic_bchat_logo,
                onCopy = {
                    copyToClipBoard("BChat ID", uiState.publicKey)
                },
                onShowDialog = {
                    onShowDialog(1)
                }
            )
            Spacer(modifier = Modifier.padding(start = 3.dp))

            ProfileCardKeyContainer(
                isBnsHolder = isBnsHolder,
                title = stringResource(id = R.string.show_qr),
                image = R.drawable.ic_show_qr,
                onCopy = {
                },
                showCopyIcon = false,
                onShowDialog = {
                    onShowDialog(2)
                }
            )
        }
    }
}

@Composable
fun ProfileCardKeyContainer(
    isBnsHolder:String?,
    title: String,
    image: Int,
    onCopy: () -> Unit,
    showCopyIcon: Boolean = true,
    isBeldex: Boolean = false,
    onShowDialog: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = if(!isBnsHolder.isNullOrEmpty()) 2.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if(!isBnsHolder.isNullOrEmpty()) MaterialTheme.appColors.profileAddressCardBackground else MaterialTheme.appColors.backgroundColor
        ),
    ) {
        Box(
            modifier =Modifier
                .padding(5.dp)
                .clickable {
                    onShowDialog()
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(
                    top = 13.dp,
                    bottom = 5.dp,
                    start = if (isBeldex) 5.dp else 15.dp,
                    end = if (isBeldex) 5.dp else 15.dp
                )
            ) {
                Image(
                    painter = painterResource(id = image), contentDescription = "",
                    modifier = Modifier
                        .size(25.dp),
                    colorFilter = if (!showCopyIcon) ColorFilter.tint(MaterialTheme.appColors.editTextColor) else null
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier =Modifier
                        .padding(top=8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            if (showCopyIcon) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.editTextHint,
                    modifier =Modifier
                        .size(16.dp)
                        .align(alignment=Alignment.TopEnd)
                        .clickable {
                            onCopy()
                        }
                )
            }
        }
    }
}

@Composable
private fun MyAccountScreenContainer(
    title: String,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    actionItems: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.appColors.editTextColor,
                modifier = Modifier
                    .clickable {
                        onBackClick()
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier
                    .weight(1f)
            )

            actionItems()
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (wrapInCard) {
            CardContainer(
                modifier =Modifier
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

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.backgroundColor
        ),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
private fun ArchiveChatScreenContainer(
    title: String,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    actionItems: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(
                painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.appColors.editTextColor,
                modifier = Modifier
                    .clickable {
                        onBackClick()
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier
                    .weight(1f)
            )
            Icon(
                painterResource(id = R.drawable.ic_unarchive_chats),
                contentDescription = stringResource(R.string.un_archive_chat),
                tint = MaterialTheme.appColors.editTextColor,
                modifier = Modifier
                    .clickable {
                        onBackClick()
                    }
            )

            actionItems()
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (wrapInCard) {
            CardContainer(
                modifier =Modifier
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