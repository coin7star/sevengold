package com.sevengold.signalapp.ui.common

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Mencegah screenshot & screen recording SELAMA composable ini ada di layar (misal
 * halaman sinyal Premium yang isinya konten berbayar), supaya lebih susah dibagikan
 * ulang ke orang yang belum bayar lewat screenshot/recording.
 *
 * FLAG_SECURE otomatis dilepas lagi begitu composable ini hilang dari layar (misal user
 * pindah ke layar lain), jadi tidak mengganggu screenshot di bagian app yang lain.
 *
 * Catatan: ini bukan proteksi 100% (foto pakai kamera dari HP lain tetap bisa), tapi
 * cukup untuk mencegah cara paling gampang (screenshot/screen record bawaan Android).
 */
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
