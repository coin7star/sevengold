# Add project specific ProGuard rules here.

# Perlu dipertahankan supaya Firestore tetap bisa baca generic type (List<T>, Map<K,V>, dst)
# dan supaya crash log (nanti kalau Crashlytics dipasang) tetap kebaca jelas.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,SourceFile,LineNumberTable

# Data model Firestore: Firestore pakai reflection (constructor kosong + getter/setter/field)
# buat ubah dokumen jadi objek Kotlin. Kalau nama field/constructor ini di-obfuscate atau
# fieldnya dihapus karena "kelihatan" tidak dipakai, mapping-nya bakal gagal diam-diam atau app
# crash. WAJIB dipertahankan utuh.
-keep class com.sevengold.signalapp.data.model.** { *; }
-keepclassmembers class com.sevengold.signalapp.data.model.** { *; }

# Firebase (Auth, Firestore, Messaging)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Sign-In (Credential Manager + fallback klasik)
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-dontwarn com.google.android.gms.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
