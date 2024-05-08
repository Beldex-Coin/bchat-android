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
import java.util.Locale

@Composable
fun FilterTransactionByDatePopUp(
    onDismiss: () -> Unit,
    context: Context,
    incomingTransactionIsChecked: Boolean,
    outgoingTransactionIsChecked: Boolean,
    viewModels: WalletViewModels,
    emptyList: MutableList<TransactionInfo>,
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
                                .fillMaxWidth().clickable{
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
                                .fillMaxWidth().clickable{
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
                            Toast.makeText(
                                context,
                                context.getString(R.string.filter_applied),
                                Toast.LENGTH_LONG
                            ).show()
                            if(incomingTransactionIsChecked && outgoingTransactionIsChecked){
                                viewModels.adapterTransactionInfoItems.value?.let {list->
                                    filterTransactionsByDate(getDaysBetweenDates(Date(selectedFromDate),Date(selectedToDate)), list, viewModels,onDismiss = {
                                        onDismiss()
                                    })
                                }
                            }else if(incomingTransactionIsChecked){
                                viewModels.adapterTransactionInfoItems.value?.let {list->
                                    filterTempList(TransactionInfo.Direction.Direction_In,
                                        list
                                    )
                                }?.let {
                                    filterTransactionsByDate(
                                        getDaysBetweenDates(Date(selectedFromDate),Date(selectedToDate)),
                                        it, viewModels,onDismiss = {
                                            onDismiss()
                                        }
                                    )
                                }
                            }else if(outgoingTransactionIsChecked){
                                viewModels.adapterTransactionInfoItems.value?.let {list->
                                    filterTempList(TransactionInfo.Direction.Direction_Out,
                                        list
                                    )
                                }?.let {
                                    filterTransactionsByDate(
                                        getDaysBetweenDates(Date(selectedFromDate),Date(selectedToDate)),
                                        it, viewModels,onDismiss = {
                                            onDismiss()
                                        }
                                    )
                                }
                            }else{
                                filterTransactionsByDate(
                                    getDaysBetweenDates(Date(selectedFromDate),Date(selectedToDate)),
                                    emptyList, viewModels,onDismiss = {
                                        onDismiss()
                                    }
                                )
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

private fun filterTempList(text: TransactionInfo.Direction, arrayList: MutableList<TransactionInfo>):MutableList<TransactionInfo> {
    val temp: MutableList<TransactionInfo> = ArrayList()
    for (d in arrayList) {
        if (d.direction == text) {
            temp.add(d)
        }
    }
    return temp
}

private fun filterTransactionsByDate(
    dates: List<Date>,
    arrayList: MutableList<TransactionInfo>,
    viewModels: WalletViewModels,
    onDismiss: () -> Unit
) {
    val temp: MutableList<TransactionInfo> = ArrayList()
    val DATETIME_FORMATTER = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    for (datesItem in dates) {
        for (d in arrayList) {
            if (DATETIME_FORMATTER.format(d.timestamp) == DATETIME_FORMATTER.format(datesItem)) {
                temp.add(d)
            }
        }
    }
    callIfTransactionListEmpty(temp.size, viewModels)
    //update recyclerview
    viewModels.updateTransactionInfoItems(temp)
    onDismiss()
}

private fun callIfTransactionListEmpty(size: Int, viewModels: WalletViewModels) {
    if (size > 0) {
        viewModels.setTransactionListContainerIsVisible(true)
    } else {
        viewModels.setFilterTransactionIconIsClickable(true)
        viewModels.setTransactionListContainerIsVisible(false)
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

    DatePickerDialog(onDismissRequest = { }, dismissButton = {
        Button(
            onClick = {
                onDismiss()
            }
        ){
            Text("Cancel")
        }
    }, confirmButton = {

        Button(
            onClick = {
                selectedDateValue(selectedFromDate,selectedDate)
                onDismiss()
            }
        ){
            Text("Ok")
        }
    }) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null,
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
            onClick = {
                onDismiss()
            }
        ){
            Text("Cancel")
        }
    }, confirmButton = {

        Button(
            onClick = {
                selectedDateValue(selectedToDate, selectedDate)
                onDismiss()
            }
        ){
            Text("Ok")
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
                containerColor = Color(0xff1C1C26),
                weekdayContentColor = Color(0xff9595Ac),
                todayContentColor = Color(0xff00BD40),
                selectedDayContainerColor = Color(0xff00BD40),
                disabledDayContentColor = Color(0xff000000)
            )
        )
    }
}

private fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy")
    return formatter.format(Date(millis))
}
