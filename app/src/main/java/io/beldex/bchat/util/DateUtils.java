/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.beldex.bchat.util;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.format.DateFormat;

import com.beldex.libsignal.utilities.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.beldex.bchat.R;

/**
 * Utility methods to help display dates in a nice, easily readable way.
 */
public class DateUtils extends android.text.format.DateUtils {

  @SuppressWarnings("unused")
  private static final String TAG = DateUtils.class.getSimpleName();
  private static final SimpleDateFormat DAY_PRECISION_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
  private static final SimpleDateFormat HOUR_PRECISION_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHH");

  private static boolean isWithin(final long millis, final long span, final TimeUnit unit) {
    return System.currentTimeMillis() - millis <= unit.toMillis(span);
  }

  private static boolean isYesterday(final long when) {
    return DateUtils.isToday(when + TimeUnit.DAYS.toMillis(1));
  }

  private static int convertDelta(final long millis, TimeUnit to) {
    return (int) to.convert(System.currentTimeMillis() - millis, TimeUnit.MILLISECONDS);
  }

  public static String getFormattedDateTime(long time, String template, Locale locale) {
    final String localizedPattern = getLocalizedPattern(template, locale);
    return new SimpleDateFormat(localizedPattern, locale).format(new Date(time));
  }
  
  private static boolean isExplicit24HourFormat(Context c) {
    String value = android.provider.Settings.System.getString(
            c.getContentResolver(),
            android.provider.Settings.System.TIME_12_24
    );
    return "24".equals(value);
  }

  public static String getHourFormat(Context c) {
    return isExplicit24HourFormat(c) ? "Hm" : "hm";
  }

  public static String getDisplayFormattedTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return c.getString(R.string.DateUtils_just_now);
    } else if (isToday(timestamp)) {
      return getFormattedDateTime(timestamp, getHourFormat(c), locale);
    } else if (isWithin(timestamp, 6, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "E" + getHourFormat(c), locale);
    } else if (isWithin(timestamp, 365, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "MMMd" + getHourFormat(c), locale);
    } else {
      return getFormattedDateTime(timestamp, "MMMd" + getHourFormat(c) + "y", locale);
    }
  }

  public static String getTimeStamp(final Context c, final Locale locale, final long timestamp) {
    return getFormattedDateTime(timestamp, getHourFormat(c), locale);
  }

  public static String getCoversationDisplayFormattedTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return c.getString(R.string.DateUtils_just_now);
    } else if (isToday(timestamp)) {
      return c.getString(R.string.DateUtils_today);
    } else if (isWithin(timestamp, 6, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "EEEE", locale);
    } else if (isWithin(timestamp, 365, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "MMMd", locale);
    } else {
      return getFormattedDateTime(timestamp, "MMMdy", locale);
    }
  }

  public static SimpleDateFormat getDetailedDateFormatter(Context context, Locale locale) {
    String skeleton;

    if (isExplicit24HourFormat(context)) {
      skeleton = "MMMdyHmsz";
    } else {
      skeleton = "MMMdyhmsza";
    }

    String dateFormatPattern = getLocalizedPattern(skeleton, locale);

    return new SimpleDateFormat(dateFormatPattern, locale);
  }

  public static String getRelativeDate(@NonNull Context context,
                                       @NonNull Locale locale,
                                       long timestamp)
  {
    if (isToday(timestamp)) {
      return context.getString(R.string.DateUtils_today);
    } else if (isYesterday(timestamp)) {
      return context.getString(R.string.DateUtils_yesterday);
    } else {
      return getFormattedDateTime(timestamp, "EMMMdy", locale);
    }
  }

  public static boolean isSameDay(long t1, long t2) {
    return DAY_PRECISION_DATE_FORMAT.format(new Date(t1)).equals(DAY_PRECISION_DATE_FORMAT.format(new Date(t2)));
  }

  public static boolean isSameHour(long t1, long t2) {
    return HOUR_PRECISION_DATE_FORMAT.format(new Date(t1)).equals(HOUR_PRECISION_DATE_FORMAT.format(new Date(t2)));
  }

  private static String getLocalizedPattern(String template, Locale locale) {
    return DateFormat.getBestDateTimePattern(locale, template);
  }

  /**
   * e.g. 2020-09-04T19:17:51Z
   * https://www.iso.org/iso-8601-date-and-time-format.html
   *
   * @return The timestamp if able to be parsed, otherwise -1.
   */
  @SuppressLint("ObsoleteSdkInt")
  public static long parseIso8601(@Nullable String date) {
    SimpleDateFormat format;
    format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());

    if (date.isEmpty()) {
      return -1;
    }

    try {
      return format.parse(date).getTime();
    } catch (ParseException e) {
      Log.w(TAG, "Failed to parse date.", e);
      return -1;
    }
  }

  // region Deprecated
  public static String getBriefRelativeTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return c.getString(R.string.DateUtils_just_now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = convertDelta(timestamp, TimeUnit.MINUTES);
      return c.getResources().getString(R.string.DateUtils_minutes_ago, mins);
    } else if (isWithin(timestamp, 1, TimeUnit.DAYS)) {
      int hours = convertDelta(timestamp, TimeUnit.HOURS);
      return c.getResources().getQuantityString(R.plurals.hours_ago, hours, hours);
    } else if (isWithin(timestamp, 6, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "E", locale);
    } else if (isWithin(timestamp, 365, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "MMMd", locale);
    } else {
      return getFormattedDateTime(timestamp, "MMMdy", locale);
    }
  }

  public static String getExtendedRelativeTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return c.getString(R.string.DateUtils_just_now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = (int)TimeUnit.MINUTES.convert(System.currentTimeMillis() - timestamp, TimeUnit.MILLISECONDS);
      return c.getResources().getString(R.string.DateUtils_minutes_ago, mins);
    } else {
      StringBuilder skeleton = new StringBuilder();
      if      (isWithin(timestamp,   6, TimeUnit.DAYS)) skeleton.append("E");
      else if (isWithin(timestamp, 365, TimeUnit.DAYS)) skeleton.append("MMMd");
      else                                              skeleton.append("MMMdy");

      skeleton.append(getHourFormat(c));

      return getFormattedDateTime(timestamp, skeleton.toString(), locale);
    }
  }
  // endregion
}