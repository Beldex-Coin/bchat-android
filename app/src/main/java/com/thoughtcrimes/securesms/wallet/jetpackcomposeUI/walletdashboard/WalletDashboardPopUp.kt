package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.walletdashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.WalletViewModels
import io.beldex.bchat.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

@Composable
fun FilterTransactionByDatePopUp(
    onDismiss: () -> Unit,
    selectedDates: (listOfDates :List<Date>,arrayList: MutableList<TransactionInfo>,) -> Unit,
    context: Context,
    viewModels: WalletViewModels,
) {
    var fromDateStr by remember {
        mutableStateOf("")
    }
    var toDateStr by remember {
        mutableStateOf("")
    }

    var selectedFromDate by  remember {
        mutableLongStateOf(0)
    }

    var selectedToDate by remember {
        mutableLongStateOf(0)
    }

    var showFromDatePicker by remember {
        mutableStateOf(false)
    }

    var showToDatePicker by remember {
        mutableStateOf(false)
    }

    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier) {
            OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Text(text = stringResource(id = R.string.select_date_range), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(800), color = MaterialTheme.appColors.selectDateRangeText), textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))
                    OutlinedCard(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground),modifier = Modifier
                            .padding(top = 10.dp)
                    ) {
                        TextField(
                            value = fromDateStr,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showFromDatePicker = !showFromDatePicker
                                },
                            placeholder = {
                                Text(
                                    text = "From Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.appColors.editTextHint,modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            onValueChange = {

                            },
                            colors = TextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.appColors.textColor
                            ),
                            trailingIcon = {
                                Icon(Icons.Filled.Circle, "From Date", tint = MaterialTheme.appColors.primaryButtonColor, modifier = Modifier
                                    .size(8.dp))
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CalendarToday, "From Date", tint = MaterialTheme.appColors.primaryButtonColor, modifier = Modifier
                                    .size(17.dp))
                            },
                        )
                    }
                    OutlinedCard(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), modifier = Modifier.padding(top = 10.dp)
                    ) {
                        TextField(
                            value = toDateStr,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showToDatePicker = !showToDatePicker
                                },
                            placeholder = {
                                Text(
                                    text = "To Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.appColors.editTextHint,modifier = Modifier
                                        .fillMaxWidth(),
                                )
                            },
                            onValueChange = {

                            },
                            colors = TextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.appColors.textColor
                            ),
                            trailingIcon = {
                                Icon(Icons.Filled.Circle, "To Date", tint = MaterialTheme.appColors.secondaryColor, modifier = Modifier
                                    .size(8.dp))
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CalendarToday, "To Date", tint = MaterialTheme.appColors.secondaryColor, modifier = Modifier
                                    .size(17.dp))
                            },
                        )
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)) {
                        Button(onClick = {
                            onDismiss()
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.cancelButtonColor), modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.cancel), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(onClick = {
                            if(fromDateStr.isEmpty()){
                                Toast.makeText(context, context.getString(R.string.alert_from_date),Toast.LENGTH_SHORT).show()
                            }else if(toDateStr.isEmpty()){
                                Toast.makeText(context, context.getString(R.string.alert_to_date),Toast.LENGTH_SHORT).show()
                            }else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.filter_applied),
                                    Toast.LENGTH_LONG
                                ).show()
                                viewModels.transactionInfoItems.value?.let { list ->
                                    selectedDates(
                                        getDaysBetweenDates(
                                            Date(selectedFromDate),
                                            Date(selectedToDate)
                                        ), list
                                    )
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.ok), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    if(showFromDatePicker){
        FromDatePickerView(selectedDateValue = { date,dateStr->
            selectedFromDate = date
            fromDateStr = dateStr
            val dates = selectedToDate - selectedFromDate
            if(selectedToDate != 0.toLong() && dates<0){
                selectedToDate = selectedFromDate
                toDateStr = convertMillisToDate(selectedToDate)
            }
        },onDismiss = {
            showFromDatePicker = false
        })
    }

    if(showToDatePicker){
        ToDatePickerView(selectedFromDate, selectedDateValue = {date,dateStr->
            selectedToDate = date
            toDateStr = dateStr
        },onDismiss = {
            showToDatePicker = false
        })
    }
}

private fun getDaysBetweenDates(startDate: Date, endDate: Date): List<Date> {
    val dates: MutableList<Date> = ArrayList()
    if(startDate == endDate){
        val calendar: Calendar = GregorianCalendar()
        calendar.time = startDate
        dates.add(calendar.time)
        return dates
    }else {
        val calendar: Calendar = GregorianCalendar()
        calendar.time = startDate
        while (calendar.time.before(endDate)) {
            val result = calendar.time
            dates.add(result)
            calendar.add(Calendar.DATE, 1)
        }
        val calendarEndDate: Calendar = GregorianCalendar()
        calendarEndDate.time = endDate
        dates.add(calendarEndDate.time)
        return dates
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FromDatePickerView(
    selectedDateValue: (selectedFromDate:Long,selectedFromDateStr:String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(System.currentTimeMillis())

    var selectedFromDate by remember {
        mutableLongStateOf(0)
    }

    val selectedDate = datePickerState.selectedDateMillis?.let {
        selectedFromDate = it
        convertMillisToDate(it)
    }?: ""

    DatePickerDialog(
        onDismissRequest = {},
        dismissButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.appColors.cancelButtonColor,
                    contentColor = MaterialTheme.appColors.cancelButtonTextColor,
                ),
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.appColors.primaryButtonColor,
                    contentColor = MaterialTheme.appColors.textColor,
                ),
                onClick = {
                    selectedDateValue(selectedFromDate, selectedDate)
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null,
            dateValidator = {
                it < System.currentTimeMillis()
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.appColors.cardBackground,
                weekdayContentColor = Color(0xff9595Ac),
                todayContentColor = MaterialTheme.appColors.primaryButtonColor,
                selectedDayContainerColor = MaterialTheme.appColors.primaryButtonColor,
                disabledDayContentColor = MaterialTheme.appColors.disableDateColor
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDatePickerView(
    selectedFromDate: Long,
    selectedDateValue: (selectedToDate:Long,selectedToDateStr:String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(System.currentTimeMillis())

    var selectedToDate by remember {
        mutableLongStateOf(0)
    }

    val selectedDate = datePickerState.selectedDateMillis?.let {
        selectedToDate = it
        convertMillisToDate(it)
    } ?: ""

    DatePickerDialog(onDismissRequest = {onDismiss()}, dismissButton = {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.appColors.cancelButtonColor,
                contentColor = MaterialTheme.appColors.cancelButtonTextColor,
            ),
            onClick = {
                onDismiss()
            }
        ){
            Text(stringResource(id = R.string.cancel))
        }
    }, confirmButton = {

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.appColors.primaryButtonColor,
                contentColor = MaterialTheme.appColors.textColor,
            ),
            onClick = {
                selectedDateValue(selectedToDate, selectedDate)
                onDismiss()
            }
        ){
            Text(stringResource(id = R.string.ok))
        }
    }) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null,
            dateValidator = {
                it>=selectedFromDate && it<System.currentTimeMillis()
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.appColors.cardBackground,
                weekdayContentColor = Color(0xff9595Ac),
                todayContentColor = MaterialTheme.appColors.primaryButtonColor,
                selectedDayContainerColor = MaterialTheme.appColors.primaryButtonColor,
                disabledDayContentColor = MaterialTheme.appColors.disableDateColor
            )
        )
    }
}

private fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy")
    return formatter.format(Date(millis))
}
