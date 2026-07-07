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

# PROTEÇÃO CONTRA A EXCLUSÃO DA TELA INICIAL
-keep class com.example.MainActivity { *; }
-keep class br.com.yson.controle.de.obras.MainActivity { *; }
-keep public class * extends android.app.Activity

# PROTEÇÃO DAS ENTIDADES DO BANCO DE DADOS (ENTITIES.KT)
-keep class com.example.data.model.** { *; }
-keep class br.com.yson.controle.de.obras.data.model.** { *; }

# PROTEÇÃO DAS INTERFACES DE CONSULTA (APPDAO.KT)
-keep class com.example.data.dao.** { *; }
-keep class br.com.yson.controle.de.obras.data.dao.** { *; }

# REGRAS OBRIGATÓRIAS PARA O ROOM DATABASE
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
