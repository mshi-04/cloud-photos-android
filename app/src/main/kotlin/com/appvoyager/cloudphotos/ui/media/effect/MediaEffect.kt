package com.appvoyager.cloudphotos.ui.media.effect

import androidx.annotation.StringRes

sealed class MediaEffect {

    data class ShowSnackbar(@param:StringRes val messageResId: Int) : MediaEffect()

}
