package io.beldex.bchat.util

import android.view.View
import androidx.annotation.DrawableRes
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import com.thoughtcrimes.securesms.conversation.v2.input_bar.InputBarButton
import com.thoughtcrimes.securesms.home.NewConversationButtonSetView

class NewConversationButtonDrawableMatcher(@DrawableRes private val expectedId: Int): TypeSafeMatcher<View>() {

    companion object {
        @JvmStatic fun newConversationButtonWithDrawable(@DrawableRes expectedId: Int) = NewConversationButtonDrawableMatcher(expectedId)
    }

    override fun describeTo(description: Description?) {
        description?.appendText("with drawable on button with resource id: $expectedId")
    }

    override fun matchesSafely(item: View): Boolean {
        if (item !is NewConversationButtonSetView.Button) return false

        return item.getIconID() == expectedId
    }
}

class InputBarButtonDrawableMatcher(@DrawableRes private val expectedId: Int): TypeSafeMatcher<View>() {

    companion object {
        @JvmStatic fun inputButtonWithDrawable(@DrawableRes expectedId: Int) = InputBarButtonDrawableMatcher(expectedId)
    }

    override fun describeTo(description: Description?) {
        description?.appendText("with drawable on button with resource id: $expectedId")
    }

    override fun matchesSafely(item: View): Boolean {
        if (item !is InputBarButton) return false

        return item.getIconID() == expectedId
    }
}