-dontwarn org.webrtc.NetworkMonitorAutoDetect
-dontwarn android.net.Network
-keep class org.webrtc.** { *; }

# Some dependencies bundle copies of android.net.* framework classes (e.g. LinkProperties).
# R8 then treats them as app classes and renames them (android.net.LinkProperties -> android.net.b),
# so WebRTC's NetworkMonitorAutoDetect calls ConnectivityManager.getLinkProperties(Network) with the
# renamed return type and crashes with NoSuchMethodError on the ConnectivityThread when a call starts.
# Never rename or strip anything in the android.* framework packages.
-keep class android.net.** { *; }
-keep class android.** { *; }
-dontwarn android.**
