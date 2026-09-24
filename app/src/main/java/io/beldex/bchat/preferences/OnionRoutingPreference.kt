package io.beldex.bchat.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import io.beldex.bchat.R

/**
 * A settings row for the "Onion Routing" hop-count option. Instead of a toggle or a chevron this
 * row shows the currently selected value ("Off" / "1 hop" / "3 hops") on the right hand side while
 * the summary line below the title holds the fixed description from the XML.
 */
class OnionRoutingPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    private val entries: Array<String> =
        context.resources.getStringArray(R.array.preferences__onion_routing_hops_entries)
    private val values: Array<String> =
        context.resources.getStringArray(R.array.preferences__onion_routing_hops_values)

    private var selectedValueText: String = ""

    init {
        setWidgetLayoutResource(R.layout.preference_widget_value)
    }

    fun setHopCount(hopCount: Int) {
        val valueIndex = values.indexOf(hopCount.toString())
        val text =
            if (valueIndex in entries.indices) entries[valueIndex] else ""
        if (selectedValueText == text) { return }
        selectedValueText = text
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.findViewById<TextView>(R.id.preference_value_text)?.text = selectedValueText
    }
}