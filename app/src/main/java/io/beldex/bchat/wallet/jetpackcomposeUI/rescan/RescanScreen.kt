package io.beldex.bchat.wallet.jetpackcomposeUI.rescan

import android.util.ArrayMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.R
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RescanScreen(
    modifier: Modifier = Modifier,
    daemonBlockChainHeight: Long,
    dates: ArrayMap<String, Int>,
    onDismissDialog: (restoreFromBlockHeight:Long) -> Unit
) {
    val context = LocalContext.current

    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    var restoreFromDateHeight by remember {
        mutableIntStateOf(0)
    }

    var restoreFromBlockHeight by remember {
        mutableStateOf("")
    }

    var restoreFromDateIsVisible by remember {
        mutableStateOf(false)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var showErrorMessage by remember {
        mutableStateOf(false)
    }

    var restoreFromDateStr by remember {
        mutableStateOf("")
    }

    if(showDatePicker) {
        MyDatePickerDialog(
            onDateSelected = { dateStr,restoreHeight->
                restoreFromDateStr = dateStr
                restoreFromDateHeight = restoreHeight
            },
            onDismiss = { showDatePicker = false },
            dates,
            dateFormat
        )
    }

    Column(
        modifier = modifier.padding(top = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(30.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.appColors.textFieldUnfocusedColor,
                    shape = RoundedCornerShape(30.dp)
                )
                .background(
                    color = MaterialTheme.appColors.currentBlockHeightContainerBackground
                )
                .padding(15.dp)
        ) {
            Text(
                context.getString(R.string.current_blockheight),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.appColors.secondaryContentColor,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Text(
                " $daemonBlockChainHeight", style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.primaryButtonColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                ), modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (restoreFromDateIsVisible) "Enter the date at which you created the wallet" else "Enter the blockheight at which you created the wallet",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.transactionTypeTitle,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start
                )
            )

            if (restoreFromDateIsVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground),
                    modifier = Modifier.padding(top = 15.dp, bottom = 20.dp)
                ) {
                    TextField(
                        value = restoreFromDateStr,
                        placeholder = {
                            Text(
                                text = "Restore from Date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.appColors.editTextHint,
                            )
                        },
                        onValueChange = {
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.appColors.editTextBackground,
                            focusedContainerColor = MaterialTheme.appColors.editTextBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                            cursorColor = MaterialTheme.appColors.secondaryColor
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                "Calendar",
                                tint = MaterialTheme.appColors.primaryButtonColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        onClick = {
                                            showDatePicker = !showDatePicker
                                        }
                                    )
                            )
                        },
                        readOnly = true,
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground),
                    modifier = Modifier.padding(top = 15.dp, bottom = if(showErrorMessage) 5.dp else 20.dp)
                ) {
                    TextField(
                        value = restoreFromBlockHeight,
                        placeholder = {
                            Text(
                                text = "Restore from BlockHeight",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.appColors.editTextHint,
                            )
                        },
                        onValueChange = {
                            if(it.isDigitsOnly()) {
                                if (it.trim().length == 9) {
                                    Toast.makeText(
                                        context,
                                        R.string.enter_a_valid_height,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    restoreFromBlockHeight = it
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.appColors.editTextBackground,
                            focusedContainerColor = MaterialTheme.appColors.editTextBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                            cursorColor = MaterialTheme.appColors.secondaryColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            }
            if(showErrorMessage) {
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.Red,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                    ),modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            PrimaryButton(
                onClick = {
                    if(CheckOnline.isOnline(context)) {
                        val restoreFromHeight = restoreFromBlockHeight.trim()
                        when {
                            restoreFromHeight.isNotEmpty() -> {
                                val restoreHeightBig = BigInteger(restoreFromHeight.trim())
                                if(restoreHeightBig.toLong() in 0 until daemonBlockChainHeight) {
                                    errorMessage = ""
                                    showErrorMessage = false
                                    restoreFromDateStr = ""
                                    onDismissDialog(restoreFromHeight.toLong())
                                }else{
                                    errorMessage = context.getString(R.string.restore_height_error_message)
                                    showErrorMessage = true
                                }
                            }
                            restoreFromDateStr.isNotEmpty() -> {
                                restoreFromBlockHeight = ""
                                onDismissDialog(restoreFromDateHeight.toLong())
                            }
                            else -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.activity_restore_from_height_missing_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }else{
                        Toast.makeText(context,context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                    }

                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.menu_rescan),
                    style = BChatTypography.bodyLarge.copy(
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Column(modifier = Modifier
            .weight(0.3f)
            .padding(top = 10.dp)) {
            OutlinedButton(
                onClick = {
                    restoreFromDateIsVisible = !restoreFromDateIsVisible
                    showErrorMessage = false
                    errorMessage = ""
                    if(restoreFromDateIsVisible){
                        restoreFromBlockHeight = ""
                    }else{
                        restoreFromDateStr = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.appColors.disabledButtonContainerColor)
            ) {
                Row {
                    Text(
                        text = if (restoreFromDateIsVisible) "I Know the Blockheight" else "I Know the Date",
                        style = BChatTypography.bodyLarge.copy(
                            color = MaterialTheme.appColors.secondaryContentColor,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .weight(1f)
                    )
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = "",
                        tint = MaterialTheme.appColors.primaryButtonColor,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    onDateSelected: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    dates: ArrayMap<String, Int>,
    dateFormat: SimpleDateFormat
) {

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        initialDisplayedMonthMillis = System.currentTimeMillis(),
        initialDisplayMode = DisplayMode.Picker
    )

    var restoreFromDateHeight by remember {
        mutableIntStateOf(0)
    }

    val restoreFromDate = datePickerState.selectedDateMillis?.let {
        convertMillisToDate(it,dateFormat, updateRestoreHeight = { restoreHeight ->
            restoreFromDateHeight = restoreHeight
        },dates)
    } ?: ""

    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = {
                onDateSelected(restoreFromDate,restoreFromDateHeight)
                onDismiss()
            }

            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text(text = "Cancel")
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.appColors.currentBlockHeightContainerBackground,
            weekdayContentColor = Color.Blue
        )
    ) {
        DatePicker(
            state = datePickerState,
            title = null,
            headline = null,
            showModeToggle = false,
            dateValidator = {
                it<System.currentTimeMillis()
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xff1C1C26),
                weekdayContentColor = Color(0xff9595AC),
                todayContentColor = Color(0xff00BD40),
                selectedDayContainerColor = Color(0xff00BD40),
                disabledDayContentColor = Color(0xff9595A0)
            )
        )
    }
}

private fun convertMillisToDate(
    millis: Long,
    dateFormat: SimpleDateFormat,
    updateRestoreHeight: (restoreHeight: Int) -> Unit,
    dates: ArrayMap<String, Int>
): String {
    val date = Date(millis)
    val myFormat = "yyyy-MM-dd"
    val sdf = SimpleDateFormat(myFormat, Locale.US)


    if (date != null) {
        updateRestoreHeight(getHeightByDate(date,sdf,dateFormat,dates))
    }
    return sdf.format(date)
}

private fun getHeightByDate(
    date: Date,
    sdf: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    dates: ArrayMap<String, Int>
): Int {
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

    var height = dates[raw]?:0

    if (height != null) {
        if (sdfDate != null) {
            if (height <= 0 && sdfDate.after(firstDate)) {
                height = dates.values.last()
            }
        }
    }

    return height
}
