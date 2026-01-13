package io.beldex.bchat.util.daterangepicker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.CalendarView.OnDateChangeListener;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.beldex.bchat.R;

public class DateRangePicker extends Dialog implements OnClickListener, OnTabSelectedListener {
    final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private final DateRangePicker.OnCalenderClickListener onCalenderClickListener;
    private final Context context;
    private ViewFlipper viewFlipper;
    private CalendarView startDateCalendarView;
    private CalendarView endDateCalendarView;
    private TextView startDate;
    private TextView endDate;
    private TextView btnNegative;
    private TextView btnPositive;
    private long selectedFromDate;
    private long selectedToDate = 0L;
    private final Calendar startDateCal = Calendar.getInstance();
    private final Calendar endDateCal = Calendar.getInstance();
    private TabLayout tabLayout;
    private String startDateTitle = "start date";
    private String endDateTitle = "end date";
    private String startDateError = "Please select start date";
    private String endDateError = "Please select end date";

    public DateRangePicker(@NonNull Context context, DateRangePicker.OnCalenderClickListener onCalenderClickListener) {
        super(context);
        this.context = context;
        this.onCalenderClickListener = onCalenderClickListener;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(1);
        this.setContentView(R.layout.date_range_picker);
        this.initView();
    }

    private void initView() {
        this.tabLayout = (TabLayout)this.findViewById(R.id.drp_tabLayout);
        this.viewFlipper = (ViewFlipper)this.findViewById(R.id.drp_viewFlipper);
        this.startDateCalendarView = (CalendarView)this.findViewById(R.id.drp_calStartDate);
        this.endDateCalendarView = (CalendarView)this.findViewById(R.id.drp_calEndDate);
        this.startDate = (TextView)this.findViewById(R.id.drp_tvStartDate);
        this.endDate = (TextView)this.findViewById(R.id.drp_tvEndDate);
        this.btnNegative = (TextView)this.findViewById(R.id.drp_btnNegative);
        this.btnPositive = (TextView)this.findViewById(R.id.drp_btnPositive);
        this.startDateCalendarView.setMaxDate(System.currentTimeMillis());
        this.endDateCalendarView.setMaxDate(System.currentTimeMillis());
        this.startDateCalendarView.setOnDateChangeListener(new OnDateChangeListener() {
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                DateRangePicker.this.startDateCal.set(year, month, dayOfMonth);
                DateRangePicker.this.selectedFromDate = DateRangePicker.this.startDateCal.getTimeInMillis();
                DateRangePicker.this.startDate.setText(DateRangePicker.this.dateFormatter.format(DateRangePicker.this.startDateCal.getTime()));
                DateRangePicker.this.tabLayout.getTabAt(1).select();
            }
        });
        this.endDateCalendarView.setOnDateChangeListener(new OnDateChangeListener() {
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                DateRangePicker.this.endDateCal.set(year, month, dayOfMonth);
                DateRangePicker.this.selectedToDate = DateRangePicker.this.endDateCal.getTimeInMillis();
                DateRangePicker.this.endDate.setText(DateRangePicker.this.dateFormatter.format(DateRangePicker.this.endDateCal.getTime()));
            }
        });
        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.startDateTitle), true);
        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.endDateTitle));
        this.btnPositive.setOnClickListener(this);
        this.btnNegative.setOnClickListener(this);
        this.tabLayout.addOnTabSelectedListener(this);
    }

    public void onClick(View view) {
        if (view == this.btnPositive) {
            if (TextUtils.isEmpty(this.startDate.getText().toString())) {
                Snackbar.make(this.startDate, this.startDateError, BaseTransientBottomBar.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(this.endDate.getText().toString())) {
                Snackbar.make(this.endDate, this.endDateError, BaseTransientBottomBar.LENGTH_SHORT).show();
            } else {
                this.onCalenderClickListener.onDateSelected(this.selectedFromDate, this.selectedToDate);
                this.dismiss();
            }
        } else if (view == this.btnNegative) {
            this.dismiss();
        }

    }

    public void onTabSelected(Tab tab) {
        if (tab.getPosition() == 0) {
            this.viewFlipper.showPrevious();
        } else {
            this.showToDateCalender();
        }

    }

    private void showToDateCalender() {
        this.endDateCalendarView.setMinDate(0L);
        this.endDateCalendarView.setMinDate(this.selectedFromDate);
        if (this.selectedToDate != 0L) {
            this.endDateCalendarView.setDate(this.selectedToDate);
        }

        this.viewFlipper.showNext();
        if (!TextUtils.isEmpty(this.endDate.getText()) && this.endDateCal.before(this.startDateCal)) {
            this.endDate.setText(this.startDate.getText().toString());
        }

    }

    public void onTabUnselected(Tab tab) {
    }

    public void onTabReselected(Tab tab) {
    }

    public void setBtnPositiveText(String text) {
        this.btnPositive.setText(text);
    }

    public void setBtnNegativeText(String text) {
        this.btnNegative.setText(text);
    }

    public SimpleDateFormat getDateFormatter() {
        return this.dateFormatter;
    }

    public String getStartDateTitle() {
        return this.startDateTitle;
    }

    public void setStartDateTitle(String startDateTitle) {
        this.startDateTitle = startDateTitle;
    }

    public CalendarView getStartDateCalendarView() {
        return this.startDateCalendarView;
    }

    public CalendarView getEndDateCalendarView() {
        return this.endDateCalendarView;
    }

    public TextView getStartDate() {
        return this.startDate;
    }

    public TextView getEndDate() {
        return this.endDate;
    }

    public TextView getBtnNegative() {
        return this.btnNegative;
    }

    public TextView getBtnPositive() {
        return this.btnPositive;
    }

    public TabLayout getTabLayout() {
        return this.tabLayout;
    }

    public String getEndDateTitle() {
        return this.endDateTitle;
    }

    public void setEndDateTitle(String endDateTitle) {
        this.endDateTitle = endDateTitle;
    }

    public String getStartDateError() {
        return this.startDateError;
    }

    public void setStartDateError(String startDateError) {
        this.startDateError = startDateError;
    }

    public String getEndDateError() {
        return this.endDateError;
    }

    public void setEndDateError(String endDateError) {
        this.endDateError = endDateError;
    }

    public interface OnCalenderClickListener {
        void onDateSelected(Long var1, Long var2);
    }
}
