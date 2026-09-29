package io.github.fabiann1809.reader.ui.components

import androidx.annotation.StringRes
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiError

/** User-facing message for an AI failure. Anything that is not an [AiError] gets a generic message. */
@StringRes
fun aiErrorMessageRes(error: Throwable): Int = when (error) {
    is AiError.MissingApiKey -> R.string.ai_error_missing_key
    is AiError.InvalidApiKey -> R.string.ai_error_invalid_key
    is AiError.QuotaExhausted -> R.string.ai_error_quota_exhausted
    is AiError.RateLimited -> R.string.ai_error_rate_limited
    is AiError.NoInternet -> R.string.ai_error_no_internet
    is AiError.Timeout -> R.string.ai_error_timeout
    is AiError.ServiceUnavailable -> R.string.ai_error_unavailable
    is AiError.ContentBlocked -> R.string.ai_error_blocked
    else -> R.string.ai_error_unknown
}
