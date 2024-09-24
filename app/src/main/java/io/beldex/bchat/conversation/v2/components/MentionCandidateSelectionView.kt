package io.beldex.bchat.conversation.v2.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import com.beldex.libbchat.messaging.mentions.Mention
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.mms.GlideRequests
import io.beldex.bchat.util.toPx

class MentionCandidateSelectionView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : ListView(context, attrs, defStyleAttr) {
    private var mentionCandidates = listOf<Mention>()
        set(newValue) { field = newValue; mentionCandidateSelectionViewAdapter.mentionCandidates = newValue }
    var glide: GlideRequests? = null
        set(newValue) { field = newValue; mentionCandidateSelectionViewAdapter.glide = newValue }
    var openGroupServer: String? = null
        set(newValue) { field = newValue; mentionCandidateSelectionViewAdapter.openGroupServer = openGroupServer }
    var openGroupRoom: String? = null
        set(newValue) { field = newValue; mentionCandidateSelectionViewAdapter.openGroupRoom = openGroupRoom }
    var onMentionCandidateSelected: ((Mention) -> Unit)? = null

    private val mentionCandidateSelectionViewAdapter by lazy { Adapter(context) }

    private class Adapter(private val context: Context) : BaseAdapter() {
        var mentionCandidates = listOf<Mention>()
            set(newValue) { field = newValue; notifyDataSetChanged() }
        var glide: GlideRequests? = null
        var openGroupServer: String? = null
        var openGroupRoom: String? = null

        override fun getCount(): Int {
            return mentionCandidates.count()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Mention {
            return mentionCandidates[position]
        }

        override fun getView(position: Int, cellToBeReused: View?, parent: ViewGroup): View {
            val cell = cellToBeReused as MentionCandidateView? ?: MentionCandidateView(context)
            val mentionCandidate = getItem(position)
            cell.glide = glide
            cell.mentionCandidate = mentionCandidate
            cell.openGroupServer = openGroupServer
            cell.openGroupRoom = openGroupRoom
            return cell
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    init {
        clipToOutline = true
        adapter = mentionCandidateSelectionViewAdapter
        mentionCandidateSelectionViewAdapter.mentionCandidates = mentionCandidates
        setOnItemClickListener { _, _, position, _ ->
            onMentionCandidateSelected?.invoke(mentionCandidates[position])
        }
    }

    fun show(mentionCandidates: List<Mention>, threadID: Long) {
        val openGroup = DatabaseComponent.get(context).beldexThreadDatabase().getOpenGroupChat(threadID)
        if (openGroup != null) {
            openGroupServer = openGroup.server
            openGroupRoom = openGroup.room
        }
        this.mentionCandidates = mentionCandidates
        val layoutParams = this.layoutParams as ViewGroup.LayoutParams
        layoutParams.height = toPx(Math.min(mentionCandidates.count(), 4) * 44, resources)
        this.layoutParams = layoutParams
    }

    fun hide() {
        val layoutParams = this.layoutParams as ViewGroup.LayoutParams
        layoutParams.height = 0
        this.layoutParams = layoutParams
    }
}