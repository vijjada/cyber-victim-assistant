package com.cybervictim.assistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Official App Color Palette
 *
 * | Colour          | HEX code  | Usage                                     |
 * |-----------------|-----------|-------------------------------------------|
 * | Deep Navy Blue  | #082B5B   | Primary buttons, headers, navigation       |
 * | Royal Blue      | #145DA0   | Active tabs, links, progress indicators   |
 * | Ivory White     | #FAF9F6   | Main background                           |
 * | White           | #FFFFFF   | Forms, cards, input fields                |
 * | Ash Grey        | #E5E7EB   | Borders, dividers, inactive elements      |
 * | Saffron Gold    | #F4A62A   | Important actions and highlights          |
 * | Soft Mint       | #DDF3E8   | Success messages and confirmation cards   |
 * | Charcoal        | #263238   | Main text                                 |
 */
object AppColors {
    val DeepNavyBlue = Color(0xFF082B5B) // Primary buttons, headers, navigation
    val RoyalBlue = Color(0xFF145DA0)    // Active tabs, links, progress indicators
    val IvoryWhite = Color(0xFFFAF9F6)   // Main background
    val White = Color(0xFFFFFFFF)        // Forms, cards, input fields
    val AshGrey = Color(0xFFE5E7EB)      // Borders, dividers, inactive elements
    val SaffronGold = Color(0xFFF4A62A)  // Important actions and highlights
    val SoftMint = Color(0xFFDDF3E8)     // Success messages and confirmation cards
    val Charcoal = Color(0xFF263238)     // Main text
    val SubtitleCharcoal = Color(0xFF546E7A) // Secondary / subtitle text
    val MintDarkText = Color(0xFF166534) // For readable text on Soft Mint
    val SaffronDarkText = Color(0xFF92400E) // For readable text on Saffron
}

val AppColorScheme = lightColorScheme(
    primary = AppColors.DeepNavyBlue,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.SoftMint,
    onPrimaryContainer = AppColors.DeepNavyBlue,
    secondary = AppColors.RoyalBlue,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.SoftMint,
    onSecondaryContainer = AppColors.RoyalBlue,
    background = AppColors.IvoryWhite,
    onBackground = AppColors.Charcoal,
    surface = AppColors.White,
    onSurface = AppColors.Charcoal,
    outline = AppColors.AshGrey,
    outlineVariant = AppColors.AshGrey,
    tertiary = AppColors.SaffronGold,
    onTertiary = AppColors.Charcoal
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
