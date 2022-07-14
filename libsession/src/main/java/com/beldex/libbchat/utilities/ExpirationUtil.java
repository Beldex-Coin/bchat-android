package com.beldex.libbchat.utilities;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import com.beldex.libbchat.R;

public class ExpirationUtil {

  public static String getExpirationDisplayValue(Context context, int expirationTime) {
    if (expirationTime <= 0) {
      return context.getString(R.string.expiration_off);
    } else if (expirationTime < TimeUnit.MINUTES.toSeconds(1)) {
      return context.getResources().getQuantityString(R.plurals.expiration_seconds, expirationTime, expirationTime);
    } else if (expirationTime < TimeUnit.HOURS.toSeconds(1)) {
      int minutes = expirationTime / (int)TimeUnit.MINUTES.toSeconds(1);
      return context.getResources().getQuantityString(R.plurals.expiration_minutes, minutes, minutes);
    } else if (expirationTime < TimeUnit.DAYS.toSeconds(1)) {
      int hours = expirationTime / (int)TimeUnit.HOURS.toSeconds(1);
      return context.getResources().getQuantityString(R.plurals.expiration_hours, hours, hours);
    } else if (expirationTime < TimeUnit.DAYS.toSeconds(7)) {
      int days = expirationTime / (int)TimeUnit.DAYS.toSeconds(1);
      return context.getResources().getQuantityString(R.plurals.expiration_days, days, days);
    } else {
      int weeks = expirationTime / (int)TimeUnit.DAYS.toSeconds(7);
      return context.getResources().getQuantityString(R.plurals.expiration_weeks, weeks, weeks);
    }
  }

  public static String getExpirationAbbreviatedDisplayValue(Context context, int expirationTime) {
    if (expirationTime < TimeUnit.MINUTES.toSeconds(1)) {
      return context.getResources().getString(R.string.expiration_seconds_abbreviated, expirationTime);
    } else if (expirationTime < TimeUnit.HOURS.toSeconds(1)) {
      int minutes = expirationTime / (int)TimeUnit.MINUTES.toSeconds(1);
      return context.getResources().getString(R.string.expiration_minutes_abbreviated, minutes);
    } else if (expirationTime < TimeUnit.DAYS.toSeconds(1)) {
      int hours = expirationTime / (int)TimeUnit.HOURS.toSeconds(1);
      return context.getResources().getString(R.string.expiration_hours_abbreviated, hours);
    } else if (expirationTime < TimeUnit.DAYS.toSeconds(7)) {
      int days = expirationTime / (int)TimeUnit.DAYS.toSeconds(1);
      return context.getResources().getString(R.string.expiration_days_abbreviated, days);
    } else {
      int weeks = expirationTime / (int)TimeUnit.DAYS.toSeconds(7);
      return context.getResources().getString(R.string.expiration_weeks_abbreviated, weeks);
    }
  }

  public static String getTimeOutDisplayValue(Context context, String expirationTime) {
    if (expirationTime.equals("None")) {
      return context.getResources().getString(R.string.none);
    } else if (expirationTime.equals("30 seconds")) {
      return context.getResources().getString(R.string.thirty_seconds);
    }
    else if (expirationTime.equals("1 Minute")) {
      return context.getResources().getString(R.string.one_minute);
    }
    else if (expirationTime.equals("2 Minutes")) {
      return context.getResources().getString(R.string.two_minutes);
    }
    else if (expirationTime.equals("5 Minutes")) {
      return context.getResources().getString(R.string.five_minutes);
    }
    else if (expirationTime.equals("15 Minutes")) {
      return context.getResources().getString(R.string.fifteen_minutes);
    }else {
      return context.getResources().getString(R.string.thirty_minutes);
    }
  }

}
