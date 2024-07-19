package com.thoughtcrimes.securesms.wallet.rescan

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.DialogInterface
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.rescan.RescanScreen
import io.beldex.bchat.R

class RescanDialog(val contextHomeActivity: HomeActivity, private val daemonBlockChainHeight: Long): DialogFragment() {
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
        dates["2022-10"] = 1575070

        dates["2022-11"] = 1674950
        dates["2022-12"] = 1764950
        dates["2023-01"] = 1853950
        dates["2023-02"] = 1942950
        dates["2023-03"] = 2022950
        dates["2023-04"] = 2112950
        dates["2023-05"] = 2199950
        dates["2023-06"] = 2289269
        dates["2023-07"] = 2363143
        dates["2023-08"] = 2420443
        dates["2023-09"] = 2503900
        dates["2023-10"] = 2585550
        dates["2023-11"] = 2696980
        dates["2023-12"] = 2816300
        dates["2024-01"] = 2894560
        dates["2024-02"] = 2986700
        dates["2024-03"] = 3049909
        dates["2024-04"] = 3130730
        dates["2024-05"] = 3187670
        dates["2024-06"] = 3317020
        dates["2024-07"] = 3429750

        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme (
                    //darkTheme = false
                ){
                    RescanScreenContainer(title = stringResource(R.string.menu_rescan),
                        onBackClick = {
                            keyboardDismiss(requireView())
                            dismiss()
                        }){
                        RescanScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            daemonBlockChainHeight,
                            dates,
                            onDismissDialog = {restoreFromHeight->
                                contextHomeActivity.onWalletRescan(restoreFromHeight)
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun keyboardDismiss(v: View) {
        val inputMethodManager : InputMethodManager? = contextHomeActivity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager!!.hideSoftInputFromWindow(v.applicationWindowToken, 0)
    }

    override fun onDismiss(dialog: DialogInterface) {
        keyboardDismiss(requireView())
        super.onDismiss(dialog)
    }

}

@Composable
private fun RescanScreenContainer(
    title: String,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    actionItems: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primary)
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
private fun CardContainer(
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