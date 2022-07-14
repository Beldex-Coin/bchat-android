-dontoptimize
-keepattributes SourceFile,LineNumberTable
-keep class org.whispersystems.** { *; }
-keep class com.thoughtcrimes.securesms.** { *; }
-keep class com.beldex.** { *; }
-keepclassmembers class ** {
    public void onEvent*(**);
}

