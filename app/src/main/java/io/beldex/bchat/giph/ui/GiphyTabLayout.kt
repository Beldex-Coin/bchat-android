package io.beldex.bchat.giph.ui

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.tabs.TabLayout
import io.beldex.bchat.R

/**
 * Figma "Send Gif" bottom tabs (7609:4028): each tab is a corner-cut tile with a 16dp icon next to
 * its "GIF" / "Stickers" label (label style comes from TextAppearance.Bchat.GiphyTab).
 *
 * GiphyActivity builds the tabs with setupWithViewPager(), which recreates them from the page
 * titles and drops any XML-declared icons, so the icon is attached here as each tab is added
 * (position 0 = GIFs, position 1 = Stickers).
 */
class GiphyTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.tabStyle
) : TabLayout(context, attrs, defStyleAttr) {

    override fun addTab(tab: Tab, position: Int, setSelected: Boolean) {
        tab.setIcon(if (position == 0) R.drawable.ic_giphy_tab_gif else R.drawable.ic_giphy_tab_sticker)
        super.addTab(tab, position, setSelected)
    }
}
