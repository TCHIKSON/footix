package com.footix.tv.ui.common

import androidx.annotation.StringRes
import com.footix.tv.R
import com.footix.tv.core.AppError

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.NETWORK -> R.string.error_network
    AppError.TIMEOUT -> R.string.error_timeout
    AppError.SERVER -> R.string.error_server
    AppError.NO_SOURCE -> R.string.error_no_source
    AppError.UNKNOWN -> R.string.error_unknown
}
