package io.beldex.bchat.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RestoreHeight {
    static final int DIFFICULTY_TARGET = 30; // seconds

    static private RestoreHeight Singleton = null;

    static public RestoreHeight getInstance() {
        if (Singleton == null) {
            synchronized (RestoreHeight.class) {
                if (Singleton == null) {
                    Singleton = new RestoreHeight();
                }
            }
        }
        return Singleton;
    }

    private Map<String, Long> blockheight = new HashMap<>();

    RestoreHeight() {

        blockheight.put("2019-03-01", 21164L);
        blockheight.put("2019-04-01", 42675L);
        blockheight.put("2019-05-01", 64918L);
        blockheight.put("2019-06-01", 348926L);
        blockheight.put("2019-07-01", 108687L);
        blockheight.put("2019-08-01", 130935L);
        blockheight.put("2019-09-01", 152452L);
        blockheight.put("2019-10-01", 174680L);
        blockheight.put("2019-11-01", 196906L);
        blockheight.put("2019-12-01", 217017L);
        blockheight.put("2020-01-01", 239353L);
        blockheight.put("2020-02-01", 260946L);
        blockheight.put("2020-03-01", 283214L);
        blockheight.put("2020-04-01", 304758L);
        blockheight.put("2020-05-01", 326679L);
        blockheight.put("2020-06-01", 348926L);
        blockheight.put("2020-07-01", 370533L);
        blockheight.put("2020-08-01", 392807L);
        blockheight.put("2020-09-01", 414270L);
        blockheight.put("2020-10-01", 436562L);
        blockheight.put("2020-11-01", 458817L);
        blockheight.put("2020-12-01", 479654L);
        blockheight.put("2021-01-01", 501870L);
        blockheight.put("2021-02-01", 523356L);
        blockheight.put("2021-03-01", 545569L);
        blockheight.put("2021-04-01", 567123L);
        blockheight.put("2021-05-01", 589402L);
        blockheight.put("2021-06-01", 611687L);
        blockheight.put("2021-07-01", 633161L);
        blockheight.put("2021-08-01", 655438L);
        blockheight.put("2021-09-01", 677038L);
        blockheight.put("2021-10-01", 699358L);
        blockheight.put("2021-11-01", 721678L);
        blockheight.put("2021-12-01", 741838L);
        blockheight.put("2022-01-01", 788501L);
        blockheight.put("2022-02-01", 877781L);
        blockheight.put("2022-03-01", 958421L);
        blockheight.put("2022-04-01", 1006790L);
        blockheight.put("2022-05-01", 1093190L);
        blockheight.put("2022-06-01", 1199750L);
        blockheight.put("2022-07-01", 1291910L);
        blockheight.put("2022-08-01", 1361030L);
        blockheight.put("2022-09-01", 1456070L);
        blockheight.put("2022-10-01", 1574150L);
        blockheight.put("2022-11-01", 1674950L);
        blockheight.put("2022-12-01", 1764230L);
        blockheight.put("2023-01-01", 1850630L);
        blockheight.put("2023-02-01", 1942950L);
        blockheight.put("2023-03-01", 2022950L);
        blockheight.put("2023-04-01", 2112950L);
        blockheight.put("2023-05-01", 2199950L);
        blockheight.put("2023-06-01", 2289269L);
        blockheight.put("2023-07-01", 2363143L);
        blockheight.put("2023-08-01", 2420443L);
        blockheight.put("2023-09-01", 2503900L);
        blockheight.put("2023-10-01", 2585550L);
        blockheight.put("2023-11-01", 2696980L);
        blockheight.put("2023-12-01", 2816300L);
        blockheight.put("2024-01-01", 2894560L);
        blockheight.put("2024-02-01", 2986700L);
        blockheight.put("2024-03-01", 3049909L);
        blockheight.put("2024-04-01", 3130730L);
        blockheight.put("2024-05-01", 3187670L);
        blockheight.put("2024-06-01", 3317020L);
        blockheight.put("2024-07-01", 3429750L);
        blockheight.put("2024-08-01", 3479700L);
        blockheight.put("2024-09-01", 3536850L);
        blockheight.put("2024-10-01", 3668050L);
        blockheight.put("2024-11-01", 3784050L);
        blockheight.put("2024-12-01", 3870400L);
        blockheight.put("2025-01-01", 3959700L);
        blockheight.put("2025-02-01", 4048980L);
    }

    public long getHeight(String date) {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        parser.setLenient(false);
        try {
            return getHeight(parser.parse(date));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public long getHeight(final Date date) {
        Log.d("Beldex","getHeight in offline data value "+ date);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.DST_OFFSET, 0);
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, -4); // give it some leeway
        if (cal.get(Calendar.YEAR) < 2019)
            return 0;
        if ((cal.get(Calendar.YEAR) == 2019) && (cal.get(Calendar.MONTH) <= 2))
            // before march 2019
            return 0;

        Calendar query = (Calendar) cal.clone();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        String queryDate = formatter.format(date);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        long prevTime = cal.getTimeInMillis();
        String prevDate = formatter.format(prevTime);
        // lookup blockheight at first of the month
        Long prevBc = blockheight.get(prevDate);
        Log.d("Beldex","Value of restoreHeight in offline prevDate value " +prevBc);
        if (prevBc == null) {
            // if too recent, go back in time and find latest one we have
            while (prevBc == null) {
                cal.add(Calendar.MONTH, -1);
                if (cal.get(Calendar.YEAR) < 2019) {
                    throw new IllegalStateException("endless loop looking for blockheight");
                }
                prevTime = cal.getTimeInMillis();
                prevDate = formatter.format(prevTime);
                prevBc = blockheight.get(prevDate);
            }
        }
        long height = prevBc;
        // now we have a blockheight & a date ON or BEFORE the restore date requested
        if (queryDate.equals(prevDate)) return height;
        // see if we have a blockheight after this date
        cal.add(Calendar.MONTH, 1);
        long nextTime = cal.getTimeInMillis();
        String nextDate = formatter.format(nextTime);
        Long nextBc = blockheight.get(nextDate);
        Log.d("Beldex","Value of restoreHeight in offline nextBc value " +nextBc);
        if (nextBc != null) { // we have a range - interpolate the blockheight we are looking for
            Log.d("Beldex","Value of restoreHeight in offline nextBc value if " +nextBc);
            long diff = nextBc - prevBc;
            long diffDays = TimeUnit.DAYS.convert(nextTime - prevTime, TimeUnit.MILLISECONDS);
            long days = TimeUnit.DAYS.convert(query.getTimeInMillis() - prevTime,
                    TimeUnit.MILLISECONDS);
            height = Math.round(prevBc + diff * (1.0 * days / diffDays));
            Log.d("Beldex","Value of restoreHeight in offline nextBc value if 1 " +height);
        } else {
            long days = TimeUnit.DAYS.convert(query.getTimeInMillis() - prevTime,
                    TimeUnit.MILLISECONDS);
            Log.d("Beldex","Value of restoreHeight in offline days value else 1 " +days);
            Log.d("Beldex","Value of restoreHeight in offline prevBc value else 1 " +prevBc);
            height = Math.round(prevBc + 1.0 * days * (24f * 60 * 60 / DIFFICULTY_TARGET));
            Log.d("Beldex","Value of restoreHeight in offline nextBc value else 1 " +height);
        }
        return height;
    }
}

