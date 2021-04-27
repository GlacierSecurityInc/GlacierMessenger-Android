-dontobfuscate

-keep class com.glaciersecurity.glaciermessenger.**

-keep class org.whispersystems.**

-keep class com.kyleduo.switchbutton.Configuration

-keep class com.soundcloud.android.crop.**

-keep class com.google.android.gms.**

#-keep class org.openintents.openpgp.*

-dontwarn org.bouncycastle.mail.**
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.cert.dane.**
-dontwarn rocks.xmpp.addr.**
-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector
-dontwarn com.google.errorprone.annotations.**

# Class names are needed in reflection
-keepnames class com.amazonaws.**
-keepnames class com.amazon.**
# Request handlers defined in request.handlers
-keep class com.amazonaws.services.**.*Handler
# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**
# Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.apache.http.**
# The SDK has several references of Apache HTTP client
-dontwarn com.amazonaws.mobile.**
-dontwarn okhttp3.**
-dontwarn com.amazonaws.http.**
-dontwarn com.amplifyframework.datastore.**
-dontwarn com.amazonaws.metrics.**
# Amplify plugin
-dontwarn com.amazonaws.mobileconnectors.**
-dontwarn com.apollographql.apollo.**
-dontwarn org.codehaus.mojo.**
-keep class tvi.webrtc.** { *; }
-keep class com.twilio.video.** { *; }
-keep class com.twilio.common.** { *; }
-keepattributes InnerClasses
