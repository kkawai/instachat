# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ~/android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/kkawai/tools/adt-bundle-mac-x86_64-20140702/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes SourceFile,LineNumberTable
-keep class com.kenai.** { *; }
-dontwarn com.kenai.**
-keep class com.novell.** { *; }
-keep class de.** { *; }
-keep class org.apache.** { *; }
-keep class org.jivesoftware.** { *; }
-keep class org.xbill.** { *; }
-keep class com.instachat.android.data.model.** { *; }
-keep class com.instachat.android.app.bans.BannedUser** { *; }
-keep class com.google.firebase.codelab.friendlychat.model.** { *; }
-keep class com.bumptech.** { *; }
-keep class com.nineoldandroids.** { *; }
-keep class com.crashlytics.** { *; }
-keep class com.github.** { *; }
-keep class com.ath.** { *; }
-keep class pub.devrel.** { *; }
-keep class com.firebaseui.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.leocardz.** { *; }
-keep class cn.pedant.** { *; }
-keep class com.pnikosis.** { *; }
-keep class com.brandongogetap.stickyheaders.** { *; }
-keep class com.cocosw.** { *; }
-keep class com.sackcentury.shinebutton.** { *; }
-keep class hanks.xyz.** { *; }
-keep class me.himanshusoni.chatmessageview.** { *; }
-keep class com.github.silvestrpredko.** { *; }
-keep class jp.wasabeef.** { *; }
-keep class jp.co.cyberagent.android.gpuimage.** { *; }

# below was necessary for making proguard work with flurry jar
-keep class com.flurry.** { *; }
-dontwarn com.flurry.**

-keep class **.R { }

-keep class **.R$* { }

-keepattributes *Annotation*

-keep class org.apache.** {*;}

-dontwarn org.apache.commons.**

-keep class com.amazon.** {*;}
-keep class com.amazonaws.** {*;}

-dontwarn com.amazonaws.**
-dontwarn org.apache.http.annotation.**

-keep class com.fasterxml.** {*;}

-dontwarn org.apache.http.annotation.**
-dontwarn org.w3c.**

-keep class com.google.** {*;}
-keep class com.android.** {*;}
-dontwarn com.android.**

-keep class com.instachat.android.util.IhLocalBroadcastManager

-dontwarn com.amazon.**


-keep interface com.google.** { *;}
-dontwarn com.google.**

-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-keepattributes *Annotation*,Signature
-keep class * extends com.google.api.client.json.GenericJson {
*;
}
-keep class com.google.api.services.drive.** {
*;
}


#Google Play Services
#https://developer.android.com/google/play-services/setup.html#Proguard
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keep class android.support.** { *; }

-keep class com.instachat.android.app.activity.group.GroupChatActivity
-keepclassmembers class com.initech.PrivateChatActivity {
    <methods>;
}
-dontwarn com.instachat.android.app.activity.group.GroupChatActivity
-dontwarn com.instachat.android.app.activity.pm.PrivateChatActivity

-dontwarn okio.**
-dontwarn javax.annotation.**

-keepattributes Signature
-keepattributes *Annotation*

-dontwarn com.smaato.**
-dontwarn android.net.**
-dontwarn retrofit2.**

# UNITY ADS
# Keep filenames and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Keep JavascriptInterface for WebView bridge
-keepattributes JavascriptInterface

# Sometimes keepattributes is not enough to keep annotations
-keep class android.webkit.JavascriptInterface {
   *;
}

# Keep all classes in Unity Ads package
-keep class com.unity3d.ads.** {
   *;
}

# Keep all classes in Unity Services package
-keep class com.unity3d.services.** {
   *;
}

-dontwarn com.google.ar.core.**
#END, UNITY ADS