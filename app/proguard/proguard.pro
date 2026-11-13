-dontoptimize
-keepattributes SourceFile,LineNumberTable
-keep class org.whispersystems.** { *; }
-keep class io.beldex.bchat.** { *; }
-keep class com.beldex.** { *; }
-keepclassmembers class ** {
    public void onEvent*(**);
}

# ---------------------------------------------------------------------------------------------
# Release (R8) rules — added for the Play Console "DEX code optimization / Obfuscation" check.
# App and library modules (io.beldex.bchat, com.beldex.libbchat, com.beldex.libsignal) stay
# un-renamed above; third-party libraries are shrunk and obfuscated.
# ---------------------------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-renamesourcefileattribute SourceFile

# JNI: native methods and the classes they use must keep their names (wallet / crypto libs).
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Protobuf (generated messages are reflected on by the runtime).
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * extends com.google.protobuf.GeneratedMessageV3 { *; }
-dontwarn com.google.protobuf.**

# Jackson / Gson / kotlinx.serialization models.
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.fasterxml.jackson.** { *; }
-keep class com.google.gson.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    static ** $serializer;
}
-keep,includedescriptorclasses class **$$serializer { *; }

# Crypto, database and networking libraries loaded by reflection or JNI.
-keep class com.goterl.** { *; }
-keep class com.sun.jna.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.signal.aesgcmprovider.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class net.zetetic.** { *; }
-keep class org.xbill.DNS.** { *; }
-keep class org.jitsi.** { *; }
-keep class org.apache.http.** { *; }
-keep class nl.komponents.kovenant.** { *; }
-keep class com.android.mms.** { *; }
-keep class com.google.android.mms.** { *; }
-keep class com.klinker.** { *; }
-keep class ezvcard.** { *; }
-keep class com.opencsv.** { *; }
-keep class org.webrtc.** { *; }

# Glide generated module.
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# EventBus subscribers.
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Parcelable / Serializable / enum contracts.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Optional / compile-only dependencies referenced by libraries.
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn sun.misc.**
-dontwarn lombok.**
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.log4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.jetbrains.annotations.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.sun.jna.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.xbill.DNS.**
-dontwarn org.jitsi.**
-dontwarn ezvcard.**
-dontwarn com.opencsv.**

# Kryo (libsession serialises queued jobs with it) and its Objenesis helper use reflection / Unsafe.
-keep class com.esotericsoftware.** { *; }
-keep class org.objenesis.** { *; }
-dontwarn com.esotericsoftware.**
-dontwarn org.objenesis.**
-dontwarn sun.nio.ch.**
