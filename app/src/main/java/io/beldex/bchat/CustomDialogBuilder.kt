package io.beldex.bchat

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.util.toPx
import io.beldex.bchat.R


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class DialogDsl

@DialogDsl
class CustomDialogBuilder(val context: Context) {

    private val dp20 = toPx(20, context.resources)
    private val dp40 = toPx(40, context.resources)

    private val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.profile_backgroundColor)

    private var dialog: AlertDialog? = null
    private fun dismiss() = dialog?.dismiss()

    private val topView = LinearLayout(context).apply { orientation = HORIZONTAL
    gravity = Gravity.END}
        .also(dialogBuilder::setCustomTitle)
    private val contentView = LinearLayout(context).apply { orientation = VERTICAL }
    private val buttonLayout = LinearLayout(context)

    private val root = LinearLayout(context).apply { orientation = VERTICAL }
        .also(dialogBuilder::setView)
        .apply {
            addView(contentView)
            addView(buttonLayout)
        }

    fun title(@StringRes id: Int) = title(context.getString(id))

    fun title(text: CharSequence?) = title(text?.toString())
    fun title(text: String?) {
        text(text, R.style.TextAppearance_AppCompat_Title) {  }
    }

    fun text(@StringRes id: Int, style: Int = 0) = text(context.getString(id), style)
    fun text(text: CharSequence?, @StyleRes style: Int = 0) {
        text(text, style) {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                .apply {  }
        }
    }

    fun icon( @StyleRes style: Int) {
        icon(style) {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.END
                setPadding(20,40,40,20) }
        }
    }

    private fun text(text: CharSequence?, @StyleRes style: Int, modify: TextView.() -> Unit) {
        text ?: return
        TextView(context, null, 0, style)
            .apply {
                setText(text)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                modify()
            }.let(contentView::addView)
    }

    private fun icon(@StyleRes style: Int, modify: ImageView.() -> Unit) {
        ImageView(context, null, 0, style)
                .apply {
                    setImageResource(R.drawable.ic_close)
                    modify()
                    setOnClickListener {
                        dismiss()
                    }
                }.let(topView::addView)

    }

    fun view(view: View) = contentView.addView(view)

    fun view(@LayoutRes layout: Int): View = LayoutInflater.from(context).inflate(layout, contentView)

    fun iconAttribute(@AttrRes icon: Int): AlertDialog.Builder = dialogBuilder.setIconAttribute(icon)

    fun singleChoiceItems(
        options: Collection<String>,
        currentSelected: Int = 0,
        onSelect: (Int) -> Unit
    ) = singleChoiceItems(options.toTypedArray(), currentSelected, onSelect)

    private fun singleChoiceItems(
        options: Array<String>,
        currentSelected: Int = 0,
        onSelect: (Int) -> Unit
    ): AlertDialog.Builder = dialogBuilder.setSingleChoiceItems(
        options,
        currentSelected
    ) { dialog, it -> onSelect(it); dialog.dismiss() }

    fun items(
        options: Array<String>,
        onSelect: (Int) -> Unit
    ): AlertDialog.Builder = dialogBuilder.setItems(
        options,
    ) { dialog, it -> onSelect(it); dialog.dismiss() }

    fun destructiveButton(
        @StringRes text: Int,
        listener: () -> Unit = {}
    ) = button(
        text,
        R.style.Widget_Bchat_Button_Dialog_DestructiveText,
        listener
    )

    fun okButton(listener: (() -> Unit) = {}) = button(android.R.string.ok, listener = listener)
    fun cancelButton(listener: (() -> Unit) = {}) = button(android.R.string.cancel, listener = listener)

    fun button(
        @StringRes text: Int,
        @StyleRes style: Int = R.style.Bchat_Profile_Change_dialog_Save,
        listener: (() -> Unit) = {}
    ) = Button(context, null, 0, style).apply {
        setText(text)
        layoutParams = LinearLayout.LayoutParams(150, 150, 1f)
            .apply {  setMargins(toPx(20, resources)) }
        setOnClickListener {
            listener.invoke()
            dismiss()
        }
    }.let(buttonLayout::addView)

    fun removeButton(
            @StringRes text: Int,
            @StyleRes style: Int = R.style.Bchat_Profile_Change_dialog_Remove,
            listener: (() -> Unit) = {}
    ) = Button(context, null, 0, style).apply {
        setText(text)
        layoutParams = LinearLayout.LayoutParams(150, 150, 1f)
                .apply { setMargins(toPx(20, resources)) }
        isEnabled = if (TextSecurePreferences.getProfileAvatarId(context) == 0) {
            setTextColor(context.getColor(R.color.disable_button_text_color))
            false
        } else {
            setTextColor(context.getColor(R.color.text))
            true
        }
        setOnClickListener {
            listener.invoke()
            dismiss()
        }
    }.let(buttonLayout::addView)

    fun create(): AlertDialog = dialogBuilder.create().also { dialog = it }
    fun show(): AlertDialog = dialogBuilder.show().also { dialog = it }
}

fun Context.showCustomDialog(build: CustomDialogBuilder.() -> Unit): AlertDialog =
    CustomDialogBuilder(this).apply { build() }.show()

fun Fragment.showCustomDialog(build: CustomDialogBuilder.() -> Unit): AlertDialog =
    CustomDialogBuilder(requireContext()).apply { build() }.show()
fun Fragment.createCustomDialog(build: CustomDialogBuilder.() -> Unit): AlertDialog =
    CustomDialogBuilder(requireContext()).apply { build() }.create()