# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.vcloud.motor.** {*; }
-keep class com.vcloud.jnihook.** {*; }
-keep class mirror.** {*; }
-keep class android.** {*; }
-keep class com.android.** {*; }

-keep class com.vcloud.reflexao.** {*; }
-keep @com.vcloud.reflexao.annotation.BClass class * {*;}
-keep @com.vcloud.reflexao.annotation.BClassName class * {*;}
-keep @com.vcloud.reflexao.annotation.BClassNameNotProcess class * {*;}
-keepclasseswithmembernames class * {
    @com.vcloud.reflexao.annotation.BField.* <methods>;
    @com.vcloud.reflexao.annotation.BFieldNotProcess.* <methods>;
    @com.vcloud.reflexao.annotation.BFieldSetNotProcess.* <methods>;
    @com.vcloud.reflexao.annotation.BFieldCheckNotProcess.* <methods>;
    @com.vcloud.reflexao.annotation.BMethod.* <methods>;
    @com.vcloud.reflexao.annotation.BStaticField.* <methods>;
    @com.vcloud.reflexao.annotation.BStaticMethod.* <methods>;
    @com.vcloud.reflexao.annotation.BMethodCheckNotProcess.* <methods>;
    @com.vcloud.reflexao.annotation.BConstructor.* <methods>;
    @com.vcloud.reflexao.annotation.BConstructorNotProcess.* <methods>;
}