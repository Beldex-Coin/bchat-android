package io.beldex.bchat.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Locale;


public class LocalHelper {
    private static final String PREFERRED_LOCALE_KEY = "preferred_locale";
    private static Locale SYSTEM_DEFAULT_LOCALE = Locale.getDefault();

    //Important
    public static ArrayList<Locale> getAvailableLocales(Context context) {
        ArrayList<Locale> locales = new ArrayList<>();
        // R.string.available_locales gets generated in build.gradle by enumerating values-* folders
        /*String[] availableLocales = context.getString(R.string.available_locales).split(",");

        for (String localeName : availableLocales) {
            locales.add(Locale.forLanguageTag(localeName));
        }*/

        return locales;
    }

    public static String getDisplayName(Locale locale, boolean sentenceCase) {
        String displayName = locale.getDisplayName(locale);

        if (sentenceCase) {
            displayName = toSentenceCase(displayName, locale);
        }

        return displayName;
    }

    public static Context setPreferredLocale(Context context) {
        return setLocale(context, getPreferredLanguageTag(context));
    }

    public static Context setAndSaveLocale(Context context, String langaugeTag) {
        savePreferredLangaugeTag(context, langaugeTag);
        return setLocale(context, langaugeTag);
    }

    private static Context setLocale(Context context, String languageTag) {
        Locale locale = (languageTag.isEmpty()) ? SYSTEM_DEFAULT_LOCALE : Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    public static void updateSystemDefaultLocale(Locale locale) {
        SYSTEM_DEFAULT_LOCALE = locale;
    }

    private static String toSentenceCase(String str, Locale locale) {
        if (str.isEmpty()) {
            return str;
        }

        int firstCodePointLen = str.offsetByCodePoints(0, 1);
        return str.substring(0, firstCodePointLen).toUpperCase(locale)
                + str.substring(firstCodePointLen);
    }

    public static Locale getPreferredLocale(Context context) {
        String languageTag = getPreferredLanguageTag(context);
        return languageTag.isEmpty() ? SYSTEM_DEFAULT_LOCALE : Locale.forLanguageTag(languageTag);
    }

    public static String getPreferredLanguageTag(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERRED_LOCALE_KEY, "");
    }

    @SuppressLint("ApplySharedPref")
    private static void savePreferredLangaugeTag(Context context, String locale) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREFERRED_LOCALE_KEY, locale).commit();
    }
}

