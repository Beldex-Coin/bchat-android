package com.thoughtcrimes.securesms.backup

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.beldex.bchat.R
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.backup.FullBackupImporter.DatabaseDowngradeException
import com.thoughtcrimes.securesms.crypto.AttachmentSecretProvider
import com.thoughtcrimes.securesms.database.DatabaseFactory
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.notifications.NotificationChannels
import com.thoughtcrimes.securesms.util.BackupUtil
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import com.thoughtcrimes.securesms.util.show

class BackupRestoreActivity : BaseActionBarActivity() {

    companion object {
        private const val TAG = "BackupRestoreActivity"
    }

    private val viewModel by viewModels<BackupRestoreViewModel>()

    private val fileSelectionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && result.data!!.data != null) {
            viewModel.backupFile.value = result.data!!.data!!
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("")

//        val viewBinding = DataBindingUtil.setContentView<ActivityBackupRestoreBinding>(this, R.layout.activity_backup_restore)
//        viewBinding.lifecycleOwner = this
//        viewBinding.viewModel = viewModel

//        viewBinding.restoreButton.setOnClickListener { viewModel.tryRestoreBackup() }

//        viewBinding.buttonSelectFile.setOnClickListener {
//            fileSelectionResultLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//                //FIXME On some old APIs (tested on 21 & 23) the mime type doesn't filter properly
//                // and the backup files are unavailable for selection.
////                type = BackupUtil.BACKUP_FILE_MIME_TYPE
//                type = "*/*"
//            })
//        }

//        viewBinding.backupCode.addTextChangedListener { text -> viewModel.backupPassphrase.value = text.toString() }

        // Focus passphrase text edit when backup file is selected.
//        viewModel.backupFile.observe(this, { backupFile ->
//            if (backupFile != null) viewBinding.backupCode.post {
//                viewBinding.backupCode.requestFocus()
//                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
//                        .showSoftInput(viewBinding.backupCode, InputMethodManager.SHOW_IMPLICIT)
//            }
//        })

        // React to backup import result.
        viewModel.backupImportResult.observe(this) { result ->
            if (result != null) when (result) {
                BackupRestoreViewModel.BackupRestoreResult.SUCCESS -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    this.show(intent)
                }
                BackupRestoreViewModel.BackupRestoreResult.FAILURE_VERSION_DOWNGRADE ->
                    Toast.makeText(this, R.string.RegistrationActivity_backup_failure_downgrade, Toast.LENGTH_LONG).show()
                BackupRestoreViewModel.BackupRestoreResult.FAILURE_UNKNOWN ->
                    Toast.makeText(this, R.string.RegistrationActivity_incorrect_backup_passphrase, Toast.LENGTH_LONG).show()
            }
        }

        //region Legal info views
        val termsExplanation = SpannableStringBuilder("By using this service, you agree to our Terms of Service and Privacy Policy")
        termsExplanation.setSpan(StyleSpan(Typeface.BOLD), 40, 56, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        termsExplanation.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                openURL("https://www.beldex.io/")
            }
        }, 40, 56, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        termsExplanation.setSpan(StyleSpan(Typeface.BOLD), 61, 75, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        termsExplanation.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                openURL("https://www.beldex.io/")
            }
        }, 61, 75, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        viewBinding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
//        viewBinding.termsTextView.text = termsExplanation
        //endregion
    }

    private fun openURL(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
        }
    }
}

class BackupRestoreViewModel(application: Application): AndroidViewModel(application) {

    companion object {
        private const val TAG = "BackupRestoreViewModel"

        @JvmStatic
        fun uriToFileName(view: View, fileUri: Uri?): String? {
            fileUri ?: return null

            view.context.contentResolver.query(fileUri, null, null, null, null).use {
                val nameIndex = it!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                return it.getString(nameIndex)
            }
        }

        @JvmStatic
        fun validateData(fileUri: Uri?, passphrase: String?): Boolean {
            return fileUri != null &&
                    !Strings.isEmptyOrWhitespace(passphrase) &&
                    passphrase!!.length == BackupUtil.BACKUP_PASSPHRASE_LENGTH
        }
    }

    val backupFile = MutableLiveData<Uri>(null)
    val backupPassphrase = MutableLiveData<String>(null)

    val processingBackupFile = MutableLiveData<Boolean>(false)
    val backupImportResult = MutableLiveData<BackupRestoreResult>(null)

    fun tryRestoreBackup() = viewModelScope.launch {
        if (processingBackupFile.value == true) return@launch
        if (backupImportResult.value == BackupRestoreResult.SUCCESS) return@launch
        if (!validateData(backupFile.value, backupPassphrase.value)) return@launch

        val context = getApplication<Application>()
        val backupFile = backupFile.value!!
        val passphrase = backupPassphrase.value!!

        val result: BackupRestoreResult

        processingBackupFile.value = true

        withContext(Dispatchers.IO) {
            result = try {
                val database = DatabaseComponent.get(context).openHelper().readableDatabase
                FullBackupImporter.importFromUri(
                        context,
                        AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret(),
                        database,
                        backupFile,
                        passphrase
                )
                DatabaseFactory.upgradeRestored(context, database)
                NotificationChannels.restoreContactNotificationChannels(context)
                TextSecurePreferences.setRestorationTime(context, System.currentTimeMillis())
                TextSecurePreferences.setHasViewedSeed(context, true)
                TextSecurePreferences.setHasSeenWelcomeScreen(context, true)

                BackupRestoreResult.SUCCESS
            } catch (e: DatabaseDowngradeException) {
                Log.w(TAG, "Failed due to the backup being from a newer version of Signal.", e)
                BackupRestoreResult.FAILURE_VERSION_DOWNGRADE
            } catch (e: Exception) {
                Log.w(TAG, e)
                BackupRestoreResult.FAILURE_UNKNOWN
            }
        }

        processingBackupFile.value = false

        backupImportResult.value = result
    }

    enum class BackupRestoreResult {
        SUCCESS, FAILURE_VERSION_DOWNGRADE, FAILURE_UNKNOWN
    }
}