package io.github.fabiann1809.reader.ui.settings

import androidx.annotation.StringRes
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiProviderKind

/** How to get a key from a provider: the page that hands them out (if there is one) and the steps. */
class ProviderGuide(@StringRes val title: Int, val url: String?, @StringRes val steps: List<Int>)

fun AiProviderKind.guide(): ProviderGuide = when (this) {
    AiProviderKind.GEMINI -> ProviderGuide(
        R.string.settings_guide_title,
        "https://aistudio.google.com/apikey",
        listOf(R.string.settings_guide_step2, R.string.settings_guide_step3, R.string.settings_guide_step4),
    )
    AiProviderKind.OPENAI -> ProviderGuide(
        R.string.settings_guide_title_paid,
        "https://platform.openai.com/api-keys",
        listOf(R.string.settings_guide_openai_step2, R.string.settings_guide_openai_step3, R.string.settings_guide_step4),
    )
    AiProviderKind.ANTHROPIC -> ProviderGuide(
        R.string.settings_guide_title_paid,
        "https://console.anthropic.com/settings/keys",
        listOf(R.string.settings_guide_anthropic_step2, R.string.settings_guide_anthropic_step3, R.string.settings_guide_step4),
    )
    AiProviderKind.COMPATIBLE -> ProviderGuide(
        R.string.settings_guide_title_compatible,
        null,
        listOf(R.string.settings_guide_compatible_step1, R.string.settings_guide_compatible_step2, R.string.settings_guide_step4),
    )
}
