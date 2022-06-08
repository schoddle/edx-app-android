package org.edx.mobile.util

import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import org.edx.mobile.R
import org.edx.mobile.core.IEdxEnvironment
import org.edx.mobile.exception.ErrorMessage
import org.edx.mobile.module.analytics.Analytics
import org.edx.mobile.module.analytics.InAppPurchasesAnalytics
import org.edx.mobile.view.dialog.AlertDialogFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppPurchasesUtils @Inject constructor(
    var environment: IEdxEnvironment,
    var iapAnalytics: InAppPurchasesAnalytics,
) {

    fun showUpgradeErrorDialog(
        context: Fragment,
        @StringRes errorResId: Int = R.string.general_error_message,
        errorCode: Int? = null,
        errorMessage: String? = null,
        errorType: Int? = null,
        listener: DialogInterface.OnClickListener? = null
    ) {
        // To restrict showing error dialog on an unattached fragment
        if (!context.isAdded) return

        val feedbackErrorMessage: String = TextUtils.getFormattedErrorMessage(
            errorCode,
            errorType,
            errorMessage
        ).toString()

        when (errorType) {
            ErrorMessage.PAYMENT_SDK_CODE -> iapAnalytics.trackIAPEvent(
                eventName = Analytics.Events.IAP_PAYMENT_ERROR,
                errorMsg = feedbackErrorMessage
            )
            ErrorMessage.PRICE_CODE -> iapAnalytics.trackIAPEvent(
                eventName = Analytics.Events.IAP_PRICE_LOAD_ERROR,
                errorMsg = feedbackErrorMessage
            )
            else -> iapAnalytics.trackIAPEvent(
                eventName = Analytics.Events.IAP_COURSE_UPGRADE_ERROR,
                errorMsg = feedbackErrorMessage
            )
        }

        AlertDialogFragment.newInstance(
            context.getString(R.string.title_upgrade_error),
            context.getString(errorResId),
            context.getString(if (listener != null) R.string.try_again else R.string.label_close),
            { dialog, which ->
                listener?.onClick(dialog, which).also {
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_RELOAD_PRICE
                    )
                } ?: run {
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_CLOSE
                    )
                }
            },
            context.getString(if (listener != null) R.string.label_cancel else R.string.label_get_help),
            { dialog, which ->
                listener?.onClick(dialog, which).also {
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_CLOSE
                    )
                    if (context is DialogFragment) context.dismiss()
                } ?: run {
                    environment.router?.showFeedbackScreen(
                        context.requireActivity(),
                        context.getString(R.string.email_subject_upgrade_error),
                        feedbackErrorMessage
                    )
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_GET_HELP
                    )
                }
            }, false
        ).show(context.childFragmentManager, null)
    }

    fun showPostUpgradeErrorDialog(
        context: Fragment,
        errorCode: Int? = null,
        errorMessage: String? = null,
        errorType: Int? = null,
        retryListener: DialogInterface.OnClickListener? = null,
        cancelListener: DialogInterface.OnClickListener? = null
    ) {

        val feedbackErrorMessage: String = TextUtils.getFormattedErrorMessage(
            errorCode,
            errorType,
            errorMessage
        ).toString()

        iapAnalytics.trackIAPEvent(
            eventName = Analytics.Events.IAP_COURSE_UPGRADE_ERROR,
            errorMsg = feedbackErrorMessage
        )
        AlertDialogFragment.newInstance(
            context.getString(R.string.title_upgrade_error),
            context.getString(R.string.error_course_not_fullfilled),
            context.getString(R.string.label_refresh_to_retry),
            { dialog, which ->
                retryListener?.onClick(dialog, which).also {
                    iapAnalytics.initRefreshContentTime()
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_REFRESH
                    )
                }
            },
            context.getString(R.string.label_get_help),
            { dialog, which ->
                cancelListener?.onClick(dialog, which).also {
                    environment.router?.showFeedbackScreen(
                        context.requireActivity(),
                        context.getString(R.string.email_subject_upgrade_error),
                        feedbackErrorMessage
                    )
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_GET_HELP
                    )
                }
            },
            context.getString(R.string.label_cancel),
            { dialog, which ->
                cancelListener?.onClick(dialog, which).also {
                    iapAnalytics.trackIAPEvent(
                        eventName = Analytics.Events.IAP_ERROR_ALERT_ACTION,
                        errorMsg = feedbackErrorMessage,
                        actionTaken = Analytics.Values.ACTION_CLOSE
                    )
                }
            }, false
        ).show(context.childFragmentManager, null)
    }
}
