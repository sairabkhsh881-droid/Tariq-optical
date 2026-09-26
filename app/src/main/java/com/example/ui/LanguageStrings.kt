package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalIsUrdu = compositionLocalOf { false }

@Composable
fun tr(en: String, ur: String): String {
    return if (LocalIsUrdu.current) ur else en
}

fun trText(isUrdu: Boolean, en: String, ur: String): String {
    return if (isUrdu) ur else en
}
