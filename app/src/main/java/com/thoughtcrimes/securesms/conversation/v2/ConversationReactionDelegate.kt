package com.thoughtcrimes.securesms.conversation.v2

import android.app.Activity
import android.graphics.PointF
import android.view.MotionEvent
import com.beldex.libbchat.utilities.Stub
import com.thoughtcrimes.securesms.conversation.v2.ConversationReactionOverlay
import com.thoughtcrimes.securesms.database.model.MessageRecord

/**
 * Delegate class that mimics the ConversationReactionOverlay public API
 *
 * This allows us to properly stub out the ConversationReactionOverlay View class while still
 * respecting listeners and other positional information that can be set BEFORE we want to actually
 * resolve the view.
 */
internal class ConversationReactionDelegate(private val overlayStub: Stub<ConversationReactionOverlay>) {
    private val lastSeenDownPoint = PointF()
    private var onReactionSelectedListener: ConversationReactionOverlay.OnReactionSelectedListener? = null
    private var onActionSelectedListener: ConversationReactionOverlay.OnActionSelectedListener? = null
    private var onHideListener: ConversationReactionOverlay.OnHideListener? = null
    val isShowing: Boolean
        get() = overlayStub.resolved() && overlayStub.get().isShowing

    fun show(
        activity: Activity,
        messageRecord: MessageRecord,
        selectedConversationModel: SelectedConversationModel,
        blindedPublicKey: String?
    ) {
        resolveOverlay().show(
            activity,
            messageRecord,
            lastSeenDownPoint,
            selectedConversationModel,
            blindedPublicKey
        )
    }

    fun hide() {
        overlayStub.get().hide()
    }

    fun hideForReactWithAny() {
        overlayStub.get().hideForReactWithAny()
    }

    fun setOnReactionSelectedListener(onReactionSelectedListener: ConversationReactionOverlay.OnReactionSelectedListener) {
        this.onReactionSelectedListener = onReactionSelectedListener
        if (overlayStub.resolved()) {
            overlayStub.get().setOnReactionSelectedListener(onReactionSelectedListener)
        }
    }

    fun setOnActionSelectedListener(onActionSelectedListener: ConversationReactionOverlay.OnActionSelectedListener) {
        this.onActionSelectedListener = onActionSelectedListener
        if (overlayStub.resolved()) {
            overlayStub.get().setOnActionSelectedListener(onActionSelectedListener)
        }
    }

    fun setOnHideListener(onHideListener: ConversationReactionOverlay.OnHideListener) {
        this.onHideListener = onHideListener
        if (overlayStub.resolved()) {
            overlayStub.get().setOnHideListener(onHideListener)
        }
    }

    val messageRecord: MessageRecord
        get() {
            check(overlayStub.resolved()) { "Cannot call getMessageRecord right now." }
            return overlayStub.get().messageRecord
        }

    fun applyTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (!overlayStub.resolved() || !overlayStub.get().isShowing) {
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                lastSeenDownPoint[motionEvent.x] = motionEvent.y
            }
            false
        } else {
            overlayStub.get().applyTouchEvent(motionEvent)
        }
    }

    private fun resolveOverlay(): ConversationReactionOverlay {
        val overlay = overlayStub.get()
        overlay.requestFitSystemWindows()
        overlay.setOnHideListener(onHideListener)
        overlay.setOnActionSelectedListener(onActionSelectedListener)
        overlay.setOnReactionSelectedListener(onReactionSelectedListener)
        return overlay
    }
}