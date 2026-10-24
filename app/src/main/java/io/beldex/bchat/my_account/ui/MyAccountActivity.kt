package io.beldex.bchat.my_account.ui

import io.beldex.bchat.util.BChatToast
import android.Manifest
import io.beldex.bchat.compose_utils.bchatBackground
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.copper.flow.observeQuery
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
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
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.avatar.AvatarSelection
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.NotchCorner
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.checkAndRequestPermissions
import io.beldex.bchat.compose_utils.singleCornerCutShape
import io.beldex.bchat.contacts.blocked.BlockedContactsViewModel
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.database.DatabaseContentProviders
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.home.ChooseLanguage
import io.beldex.bchat.messagerequests.MessageRequestsViewModel
import io.beldex.bchat.my_account.ui.dialogs.BNSNameVerifySuccessDialog
import io.beldex.bchat.my_account.ui.dialogs.ClearDataDialog
import io.beldex.bchat.my_account.ui.dialogs.CopyContentDialog
import io.beldex.bchat.my_account.ui.dialogs.LinkYourBNSDialog
import io.beldex.bchat.my_account.ui.dialogs.ProfilePicturePopup
import io.beldex.bchat.onboarding.ui.OnboardingTopBar
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.profiles.ProfileMediaConstraints
import io.beldex.bchat.util.BitmapDecodingException
import io.beldex.bchat.util.BitmapUtil
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.FileProviderUtil
import io.beldex.bchat.util.QRCodeUtilities
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.copyToClipBoard
import io.beldex.bchat.util.getColorWithID
import io.beldex.bchat.util.toPx
import io.beldex.bchat.util.unicodeNamePattern
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
import javax.inject.Inject

private val ACCOUNT_AVATAR_SIZE = 86.dp

@AndroidEntryPoint
class MyAccountActivity : BaseComponentActivity() {

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

    private fun removeAvatar() {
        val latestName = TextSecurePreferences.getProfileName(this)
        TextSecurePreferences.setIsLocalProfile(this,true)
        updateProfile(true, null,displayName = latestName, removeAvatar = true)
    }

    private fun updateProfile(isUpdatingProfilePicture: Boolean, profilePicture: ByteArray? = null,
                              displayName: String? = null, removeAvatar : Boolean = false) {
        viewModel.updateLoaderStatus(true)
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
                if(removeAvatar) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        viewModel.updateLoaderStatus(false)
                    }, 3000)
                } else {
                    viewModel.updateLoaderStatus(false)
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        destination =
            intent?.getStringExtra(extraStartDestination) ?: MyAccountScreens.SettingsScreen.route
        setContent {
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view = LocalView.current
            val window = (view.context as Activity).window
            val statusBarColor = if (isDarkTheme) Color.Black else Color.White
            val navBarColor = if (isDarkTheme) Color.Black else Color.White
            SideEffect {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    window.statusBarColor = statusBarColor.toArgb()
                    window.navigationBarColor = navBarColor.toArgb()
                }
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
                WindowInsetsControllerCompat(window, view).isAppearanceLightNavigationBars = !isDarkTheme
            }
            BChatTheme(
                darkTheme = isDarkTheme
            ) {
                val context = LocalContext.current
                val activity = (context as? Activity)
                Surface(color = Color.Transparent) {
                    Scaffold(
                        modifier = Modifier.bchatBackground(MaterialTheme.appColors.backgroundColor),
                        contentWindowInsets = WindowInsets.safeDrawing,
                        containerColor = Color.Transparent
                    ) { padding ->

                        val navController = rememberNavController()
                        MyAccountNavHost(
                            navController = navController,
                            startDestination = destination,
                            groupDatabase = groupDb,
                            startAvatarSelection = {
                                startAvatarSelection()
                            },
                            removeAvatar = {
                                removeAvatar()
                            },
                            modifier = Modifier.padding(padding)
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
            BChatToast.error(
                this,
                getString(R.string.please_check_your_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val extraStartDestination = "io.beldex.EXTRA_START_DESTINATION"

        fun intentForRoute(context: Context, route: String): Intent =
            Intent(context, MyAccountActivity::class.java).putExtra(extraStartDestination, route)
    }
}

@Composable
fun MyAccountNavHost(
    navController: NavHostController,
    startDestination: String,
    groupDatabase : GroupDatabase,
    startAvatarSelection: () -> Unit,
    removeAvatar: () -> Unit,
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
        viewModel.updateLoaderStatus(true)
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
            viewModel.updateLoaderStatus(false)
        }
    }

    fun saveDisplayName(displayName: String, context : Context): Boolean {
        if (displayName.isEmpty()) {
            BChatToast.error(
                context,
                R.string.activity_display_name_display_name_missing_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (displayName.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            BChatToast.error(
                context,
                R.string.activity_display_name_display_name_too_long_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (!displayName.matches(unicodeNamePattern.toRegex())) {
            BChatToast.makeText(
                context,
                R.string.display_name_validation,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val checkGalleryProfile=TextSecurePreferences.getIsLocalProfile(context)
        updateProfile(checkGalleryProfile, null, displayName=displayName, context)
        viewModel.updateProfile(true)
        return true
    }

    var showLoader by remember {
        mutableStateOf(false)
    }

    viewModel.showLoader.observe(lifecycleOwner) { status ->
        showLoader = status
    }

    if(showLoader){
        LoaderAnimationPopUp()
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
                    (context as BaseComponentActivity).finish()
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
                            viewModel.updateProfile(true)
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
                        BChatToast.error(
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
                            removeAvatar()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .verticalScroll(scrollState),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isBnsHolder.isNullOrEmpty()) Modifier
                                else Modifier
                            )
                    ) {
                        Box(
                            modifier = if(!isBnsHolder.isNullOrEmpty()) Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp, bottom = 10.dp)
                                .paint(
                                    painterResource(id = if (isDarkMode) R.drawable.ic_bns_card_dark else R.drawable.ic_bns_card_light),
                                    contentScale = ContentScale.FillBounds
                                )
                                .innerShadow(
                                    color = if (isDarkMode) MaterialTheme.appColors.primaryButtonColor else Color(
                                        0x8000BD40
                                    ),
                                    blur = if (isDarkMode) 20.dp else 10.dp,
                                    cornersRadius = 16.dp
                                ) else Modifier
                                .fillMaxWidth()
                                .padding(top = 0.dp, bottom = 1.dp)
                        ) {
                            ProfileCard(
                                isBnsHolder = isBnsHolder,
                                uiState = state,
                                beldexAddress = beldexAddress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = if (!isBnsHolder.isNullOrEmpty()) 40.dp
                                        else ACCOUNT_AVATAR_SIZE + 1.dp
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
                                    saveEditName = it.trim()
                                }
                            )
                        }
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .align(alignment = Alignment.TopCenter)
                                .padding(top = if (!isBnsHolder.isNullOrEmpty()) 0.dp else 12.dp)
                                .size(ACCOUNT_AVATAR_SIZE)
                                .border(1.dp, Color(0xFF000000), io.beldex.bchat.compose_utils.NotchedAvatarShape)
                        ) {
                            if(isProfileChanged) {
                                ProfilePictureComponent(
                                    publicKey = state.publicKey,
                                    displayName = state.profileName ?: state.publicKey,
                                    containerSize = ACCOUNT_AVATAR_SIZE,
                                    pictureMode = ProfilePictureMode.LargePicture,
                                    modifier = Modifier.fillMaxSize()
                                )
                                isProfileChanged = false
                            }else{
                                ProfilePictureComponent(
                                    publicKey = state.publicKey,
                                    displayName = state.profileName ?: state.publicKey,
                                    containerSize = ACCOUNT_AVATAR_SIZE,
                                    pictureMode = ProfilePictureMode.LargePicture,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (showEditNameTextField) {
                                Box(
                                    contentAlignment=Alignment.Center,
                                    modifier= Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 4.dp, bottom = 4.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            color = MaterialTheme.appColors.backgroundColor
                                        )
                                        .clickable {
                                            showPictureDialog = true

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
                        Box(
                            modifier = Modifier
                                .padding(start = 24.dp, top = 16.dp, end = 0.dp, bottom = 16.dp)
                                .widthIn(max = 120.dp)
                                .background(
                                    color = if (showEditNameTextField) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
                                )
                                .align(Alignment.TopEnd)
                                .clickable {
                                    if (!showEditNameTextField) {
                                        showNameOnly = false
                                        showEditNameTextField = true
                                    } else {
                                        val isSaved = saveEditName?.let {
                                            saveDisplayName(it.trim(), context)
                                        } ?: false

                                        if (isSaved) {
                                            showNameOnly = true
                                            showEditNameTextField = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center

                        ){
                            Text(
                                text = if(showEditNameTextField) stringResource(id=R.string.menu_done_button) else stringResource(id=R.string.edit_title),
                                style = BChatTypography.bodySmall.copy(
                                    color = if(showEditNameTextField) Color(0xFF0B0B0B) else MaterialTheme.appColors.editTextColor,
                                    fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                                    fontWeight = FontWeight(600),
                                    fontSize = 12.sp,
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)

                            )
                        }

                    }

                    if(isBnsHolder.isNullOrEmpty()) {
                        val bnsGreen = if (isDarkMode) Color(0xFF00BD40) else Color(0xFF078720)
                        val bnsGradientStart = if (isDarkMode) Color(0xFF003F00) else Color(0xFFD4EAD9)
                        val bnsGradientEnd = if (isDarkMode) Color(0xFF0B150B) else Color(0xFFFFFFFF)
                        val bnsTextColor = if (isDarkMode) Color(0xFFEBEBEB) else Color(0xFF222222)
                        val bnsShape = singleCornerCutShape(
                            BChatDimens.NotchMedium,
                            NotchCorner.TopRight
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 12.dp)
                                .height(47.dp)
                                .clip(bnsShape)
                                .drawBehind {
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(bnsGradientStart, bnsGradientEnd),
                                            center = Offset(size.width, 0f),
                                            radius = size.width
                                        )
                                    )
                                }
                                .border(1.dp, bnsGreen, bnsShape)
                                .clickable {
                                    showLinkYourBnsDialog = true
                                }
                                .padding(start = 20.dp, end = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.bns_transaction),
                                        contentDescription = "",
                                        modifier = Modifier.size(width = 14.dp, height = 16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.link_your_bns),
                                        style = BChatTypography.titleSmall.copy(
                                            color = bnsTextColor,
                                            fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                                            fontWeight = FontWeight(600),
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
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
                                modifier = Modifier
                                    .padding(end = 5.dp)
                                    .clickable(onClick = {
                                        callAboutBns()
                                    })
                            )
                            Icon(
                                painterResource(id = R.drawable.ic_info_outline_dark),
                                contentDescription = "Read more about BNS",
                                tint = MaterialTheme.appColors.secondaryTextColor,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable(onClick = {
                                        callAboutBns()
                                    })
                            )
                        }
                    }

                    Card(
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
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
                                        navController.navigate(MyAccountScreens.ChatSettingsScreen.route)
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
                                            BChatToast.error(
                                                context,
                                                context.getString(R.string.cannot_open_url),
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
            val context = LocalContext.current
            val hopCount = TextSecurePreferences.getOnionRequestPathCount(context)
            val hopsTitleResId = when (hopCount) {
                0 -> R.string.activity_path_title_direct
                1 -> R.string.activity_path_title_one_hop
                else -> R.string.activity_path_title_three_hops
            }
            MyAccountScreenContainer(
                title = dimTitleSuffix(stringResource(id = hopsTitleResId)),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                HopsScreen(
                    nodes = nodes,
                    hopCount = hopCount,
                    isDirect = hopCount == 0
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.PrivacySettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_settings_title),
                onBackClick = {
                    if (!navController.navigateUp()) {
                        (context as BaseComponentActivity).finish()
                    }
                }
            ) {
                PrivacySettingsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.NotificationSettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_settings_notifications_button_title),
                onBackClick = {
                    if (!navController.navigateUp()) {
                        (context as BaseComponentActivity).finish()
                    }
                }
            ) {
                NotificationSettingsScreen(
                    modifier = Modifier
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
                            tint = MaterialTheme.appColors.secondaryContentColor,
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = (-60).dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_no_blocked_contacts),
                                contentDescription = null,
                                modifier = Modifier.size(134.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.no_blocked_contact),
                                color = Color(0xFF737373),
                                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        composable(
            route = MyAccountScreens.AboutScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(R.string.about),
                onBackClick = {
                    (context as BaseComponentActivity).finish()
                }
            ) {
                ContentScreen(
                    modifier = Modifier
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
                        BChatToast.error(context, context.getString(R.string.failed_to_authenticate), Toast.LENGTH_SHORT).show()
                    }
                }
            val verifyPin: () -> Unit = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "onboarding://manage_pin?finish=true&action=${PinCodeAction.VerifyPinCode.action}".toUri()
                )
                intent.setPackage(context.packageName)
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
                title = stringResource(R.string.activity_settings_recovery_phrase_button_title),
                wrapInCard = markedAsSafe,
                onBackClick = {
                    (context as BaseComponentActivity).finish()
                }
            ) {
                RecoverySeedScreen(
                    seed = seed,
                    markedAsSafe = markedAsSafe,
                    verifyPin = verifyPin,
                    modifier = Modifier
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
                    (context as BaseComponentActivity).finish()
                }
            ) {
                MessageRequestsScreen(
                    requestsList = uiState.messageRequests,
                    onEvent = requestViewModel::onEvent,
                    onRequestClick = {
                        val returnIntent = Intent()
                        returnIntent.putExtra(ConversationActivityV2.THREAD_ID, it.threadId)
                        (context as Activity).run {
                            setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            finish()
                        }
                    },
                    modifier = Modifier
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
                    modifier = Modifier
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
                    (context as BaseComponentActivity).finish()
                }
            ) {
                ArchiveChatScreen(
                    requestsList = uiState.archiveChats,
                    keepArchiveChat = TextSecurePreferences.getKeepArchiveChat(context),
                    onRequestClick = {
                        val returnIntent = Intent()
                        returnIntent.putExtra(ConversationActivityV2.THREAD_ID, it.threadId)
                        (context as Activity).run {
                            setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            finish()
                        }
                    },
                    onTabChatSetting = {},
                    archiveChatViewModel = archiveChatViewModel,
                    groupDatabase = groupDatabase,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
        composable(
            route = MyAccountScreens.LanguageChooseScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(R.string.choose_language),
                onBackClick = {
                    (context as BaseComponentActivity).finish()
                }) {
                ChooseLanguage(
                    viewModel,
                    onBack = {
                        (context as BaseComponentActivity).finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedPreloader(modifier: Modifier = Modifier) {
    val preloaderLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.load_animation
        )
    )

    val preloaderProgress by animateLottieCompositionAsState(
        preloaderLottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )


    LottieAnimation(
        composition = preloaderLottieComposition,
        progress = preloaderProgress,
        modifier = modifier
    )
}

@Composable
fun LoaderAnimationPopUp() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedPreloader(modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center))
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
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        if (onShowNameOnly) {
            Text(
                text=uiState.profileName ?: "",
                style=MaterialTheme.typography.titleMedium.copy(
                    color=MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight=FontWeight(600),
                    fontSize=18.sp,
                    lineHeight=24.51.sp
                ),
                textAlign=TextAlign.Center,
                modifier= Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            )
        }
        if (onShowEditName) {
            // Figma input: #111111 fill, #333333 hairline, Roboto Mono 14.
            androidx.compose.foundation.text.BasicTextField(
                value = textFieldValueState,
                onValueChange = {
                    textFieldValueState = it
                    savedName(it)
                },
                maxLines = 1,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.appColors.textSelectionColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .border(1.dp, Color(0xFF333333))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
        if(!isBnsHolder.isNullOrEmpty()){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.bns_verified),
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
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
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
                },
                modifier = Modifier.weight(1f)
            )

            ProfileCardKeyContainer(
                isBnsHolder = isBnsHolder,
                title = stringResource(id = R.string.chatid),
                image = R.drawable.ic_bchat_logo,
                onCopy = {
                    copyToClipBoard("BChat ID", uiState.publicKey)
                },
                onShowDialog = {
                    onShowDialog(1)
                },
                modifier = Modifier.weight(1f)
            )

            ProfileCardKeyContainer(
                isBnsHolder = isBnsHolder,
                title = stringResource(id = R.string.show_qr),
                image = R.drawable.ic_show_qr,
                onCopy = {
                },
                showCopyIcon = false,
                onShowDialog = {
                    onShowDialog(2)
                },
                modifier = Modifier.weight(1f)
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
    onShowDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        // Figma 7546:13728 "BG Corner Cut": only the bottom-left corner is cut,
        // the other three edges stay square.
        shape = io.beldex.bchat.compose_utils.singleCornerCutShape(
            io.beldex.bchat.compose_utils.BChatDimens.NotchAccountCard,
            io.beldex.bchat.compose_utils.NotchCorner.BottomLeft
        ),
        border = BorderStroke(1.dp, Color(0xFF444444)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .clickable {
                    onShowDialog()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 13.dp,
                        bottom = 5.dp
                    )
            ) {
                Image(
                    painter = painterResource(id = image), contentDescription = "",
                    modifier = Modifier
                        // Figma: 28dp identity icons (Beldex logo, BChat ID, QR).
                        .size(28.dp)
                        .align(Alignment.CenterHorizontally),
                    colorFilter = if (!showCopyIcon) ColorFilter.tint(MaterialTheme.appColors.editTextColor) else null
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight(600)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            if (showCopyIcon) {
                // Figma: a small 10dp copy glyph in the card's top-right corner (above the notch cut).
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy_card),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.userDetailsConfirmBackground,
                    modifier = Modifier
                        .align(alignment = Alignment.TopEnd)
                        .padding(top = 4.dp, end = 6.dp)
                        .size(10.dp)
                        .clickable {
                            onCopy()
                        }
                )
            }
        }
    }
}

@Composable
private fun dimTitleSuffix(title: String): AnnotatedString {
    val bracketIndex = title.indexOf('(')
    if (bracketIndex <= 0) return AnnotatedString(title)

    val suffixColor = MaterialTheme.appColors.restoreDescColor
    return buildAnnotatedString {
        append(title.substring(0, bracketIndex))
        withStyle(SpanStyle(color = suffixColor)) { append(title.substring(bracketIndex)) }
    }
}

@Composable
fun MyAccountScreenContainer(
    title: String,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    actionItems: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    MyAccountScreenContainer(
        title = AnnotatedString(title),
        wrapInCard = wrapInCard,
        onBackClick = onBackClick,
        actionItems = actionItems,
        content = content
    )
}

@Composable
fun MyAccountScreenContainer(
    title: AnnotatedString,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    actionItems: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        OnboardingTopBar(
            title = title,
            onBackClick = onBackClick,
            actions = { actionItems() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (wrapInCard) {
            CardContainer(
                modifier = Modifier
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
    // Revamp_2026: settings content sits directly on the screen background (no rounded panel).
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}

@Composable
fun ArchiveChatScreenContainer(
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
        OnboardingTopBar(
            title = title,
            onBackClick = onBackClick,
            actions = { actionItems() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (wrapInCard) {
            CardContainer(
                modifier = Modifier
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