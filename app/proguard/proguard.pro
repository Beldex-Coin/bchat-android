-dontoptimize
-keepattributes SourceFile,LineNumberTable
-keep class org.whispersystems.** { *; }
-keep class io.beldex.bchat.** { *; }
-keep class com.beldex.** { *; }
-keepclassmembers class ** {
    public void onEvent*(**);
}

