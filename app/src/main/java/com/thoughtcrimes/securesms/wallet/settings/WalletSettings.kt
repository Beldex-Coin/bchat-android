package com.thoughtcrimes.securesms.wallet.settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import com.thoughtcrimes.securesms.wallet.SecureActivity
import com.thoughtcrimes.securesms.wallet.addressbook.AddressBookActivity
import com.thoughtcrimes.securesms.wallet.node.NodeListActivity
import com.thoughtcrimes.securesms.wallet.node.activity.NodeActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import io.beldex.bchat.databinding.ActivityWalletSettingsBinding

class WalletSettings : BaseActionBarActivity() {
    lateinit var binding:ActivityWalletSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Wallet Settings",false)
        binding = ActivityWalletSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding){
            currentNodeCardView.setOnClickListener{
                val intent = Intent(this@WalletSettings, NodeActivity::class.java)
                push(intent)
            }
            addressBookLayout.setOnClickListener {
                val intent = Intent(this@WalletSettings, AddressBookActivity::class.java)
                push(intent)
               /* intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK)
                push(intent)*/
            }
            changePinLayout.setOnClickListener {
                val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
                lockManager.enableAppLock(this@WalletSettings, CustomPinActivity::class.java)
                val intent = Intent(this@WalletSettings, CustomPinActivity::class.java)
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.CHANGE_PIN)
                push(intent)
            }
        }
    }
}