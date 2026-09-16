package com.beldex.libbchat.utilities.dynamiclanguage;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Updates a context with an alternative language.
 */
public final class DynamicLanguageContextWrapper {

  public static Context updateContext(Context context, String language) {
    Locale locale = LocaleParser.findBestMatchingLocaleForLanguage(language);

    Locale.setDefault(locale);

    Configuration configuration =
            new Configuration(context.getResources().getConfiguration());

      configuration.setLocales(new LocaleList(locale));
      configuration.setLayoutDirection(locale);

    return context.createConfigurationContext(configuration);
  }
}