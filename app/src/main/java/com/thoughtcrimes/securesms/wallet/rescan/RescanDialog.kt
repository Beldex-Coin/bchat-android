package com.thoughtcrimes.securesms.wallet.rescan

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.ArrayMap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.preferences.ClearAllDataDialog
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.WalletActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.RescanDialogBinding
import timber.log.Timber
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class RescanDialog(val context: HomeActivity, private val daemonBlockChainHeight: Long): DialogFragment() {

    private lateinit var binding: RescanDialogBinding

    var cal = Calendar.getInstance()
    private var restoreFromDateHeight = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private var dates = ArrayMap<String,Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL,R.style.FullScreenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RescanDialogBinding.inflate(layoutInflater,container,false)
        binding.toolbar.setButton(1)
        binding.toolbar.setTitle(resources.getString(R.string.activity_rescan_page_title))

        dates["2019-03"] = 21164
        dates["2019-04"] = 42675
        dates["2019-05"] = 64918
        dates["2019-06"] = 87175
        dates["2019-07"] = 108687
        dates["2019-08"] = 130935
        dates["2019-09"] = 152452
        dates["2019-10"] = 174680
        dates["2019-11"] = 196906
        dates["2019-12"] = 217017
        dates["2020-01"] = 239353
        dates["2020-02"] = 260946
        dates["2020-03"] = 283214
        dates["2020-04"] = 304758
        dates["2020-05"] = 326679
        dates["2020-06"] = 348926
        dates["2020-07"] = 370533
        dates["2020-08"] = 392807
        dates["2020-09"] = 414270
        dates["2020-10"] = 436562
        dates["2020-11"] = 458817
        dates["2020-12"] = 479654
        dates["2021-01"] = 501870
        dates["2021-02"] = 523356
        dates["2021-03"] = 545569
        dates["2021-04"] = 567123
        dates["2021-05"] = 589402
        dates["2021-06"] = 611687
        dates["2021-07"] = 633161
        dates["2021-08"] = 655438
        dates["2021-09"] = 677038
        dates["2021-10"] = 699358
        dates["2021-11"] = 721678
        dates["2021-12"] = 741838
        dates["2022-01"] = 788501

        dates["2022-02"] = 877781
        dates["2022-03"] = 958421
        dates["2022-04"] = 1006790
        dates["2022-05"] = 1093190
        dates["2022-06"] = 1199750
        dates["2022-07"] = 1291910
        dates["2022-08"] = 1361030
        dates["2022-09"] = 1456070
        dates["2022-10"] = 1674950

        // create an OnDateSetListener
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }


        with(binding){
            toolbar.setOnButtonListener { type ->
                when (type) {
                    1 -> {
                        binding.restoreSeedWalletRestoreDate.text = ""
                        binding.restoreSeedWalletRestoreHeight.setText("")
                        dismiss()
                    }
                    else -> Timber.e("Button " + type + "pressed - how can this be?")
                }
            }
            dialogCurrentBlockHeight.text=daemonBlockChainHeight.toString()

            binding.restoreSeedWalletRestoreHeight.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    if (binding.restoreSeedWalletRestoreHeight.text.toString().length == 9) {
                        Toast.makeText(
                            context,
                            R.string.enter_a_valid_height,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
            //SteveJosephh21
            restoreSeedWalletRestoreDate.setOnClickListener {
                restoreSeedWalletRestoreDate.inputType = InputType.TYPE_NULL;
                val datePickerDialog = DatePickerDialog(context,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                datePickerDialog.show()
            }

            rescanButton.setOnClickListener {
                if(CheckOnline.isOnline(context)) {
                    val restoreHeight = binding.restoreSeedWalletRestoreHeight.text.toString()
                    val restoreFromDate = binding.restoreSeedWalletRestoreDate.text.toString()
                    //SteveJosephh21
                    when {
                        restoreHeight.isNotEmpty() -> {
                            val restoreHeightBig = BigInteger(restoreHeight)
                            if(restoreHeightBig.toLong()>=0 && restoreHeightBig.toLong()<daemonBlockChainHeight) {
                                binding.restoreFromHeightErrorMessage.text=""
                                binding.restoreFromHeightErrorMessage.visibility=View.GONE
                                binding.restoreSeedWalletRestoreDate.text = ""
                                context.onWalletRescan(restoreHeight.toLong())
                                //_recoveryWallet(displayName,password,getSeed, restoreHeight.toLong())
                                dismiss()
                            }else{
                                binding.restoreFromHeightErrorMessage.text=getString(R.string.restore_height_error_message)
                                binding.restoreFromHeightErrorMessage.visibility=View.VISIBLE
                            }
                        }
                        restoreFromDate.isNotEmpty() -> {
                            binding.restoreSeedWalletRestoreHeight.setText("")
                            Log.d("Beldex", "Restore Height 1 ${restoreFromDateHeight.toLong()}")
                            context.onWalletRescan(restoreFromDateHeight.toLong())
                            //_recoveryWallet(displayName, password, getSeed, restoreFromDateHeight.toLong())
                            dismiss()
                        }
                        else -> {
                            Toast.makeText(
                                context,
                                getString(R.string.activity_restore_from_height_missing_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }else{
                    Toast.makeText(context,getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                }
            }

            restoreHeightInfoIcon.setOnClickListener {
                RestoreHeightInfoDialog().show(context.supportFragmentManager, "Restore Height Info Dialog")
            }
        }
        return binding.root
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.restoreSeedWalletRestoreDate.text = sdf.format(cal.time)

        if (cal.time != null) {
            restoreFromDateHeight = getHeightByDate(cal.time,sdf)
        }
    }

    private fun getHeightByDate(date: Date, sdf: SimpleDateFormat): Int {
        val sdfDate = sdf.parse(sdf.format(date))

        val monthFormat = "MM"
        val monthSdfFormat = SimpleDateFormat(monthFormat, Locale.US)
        val monthVal = monthSdfFormat.format(date).toInt()
        val month = if (monthVal < 10) "0${monthVal}" else "$monthVal"

        val yearFormat = "yyyy"
        val yearSdfFormat = SimpleDateFormat(yearFormat, Locale.US)
        val yearVal = yearSdfFormat.format(date).toInt()

        val raw = "${yearVal}-$month"
        val firstDate = dateFormat.parse(dates.keys.first())

        Log.d("Beldex","Restore Height -->$raw")

        var height = dates[raw]?:0

        if (height != null) {
            if (height <= 0 && sdfDate.after(firstDate)) {
                height = dates.values.last()
            }
        }
        Log.d("Beldex","Restore Height --> $height")

        return height
    }

}