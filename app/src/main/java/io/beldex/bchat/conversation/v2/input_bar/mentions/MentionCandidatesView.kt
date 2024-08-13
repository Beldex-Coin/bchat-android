package io.beldex.bchat.conversation.v2.input_bar.mentions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import dagger.hilt.android.AndroidEntryPoint
import com.beldex.libbchat.messaging.mentions.Mention
import io.beldex.bchat.database.BeldexThreadDatabase
import io.beldex.bchat.mms.GlideRequests
import io.beldex.bchat.util.toPx
import javax.inject.Inject

@AndroidEntryPoint
class MentionCandidatesView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : ListView(context, attrs, defStyleAttr) {
    private var candidates = listOf<Mention>()
        set(newValue) { field = newValue; snAdapter.candidates = newValue }
    var glide: GlideRequests? = null
        set(newValue) { field = newValue; snAdapter.glide = newValue }
    var openGroupServer: String? = null
        set(newValue) { field = newValue; snAdapter.openGroupServer = openGroupServer }
    var openGroupRoom: String? = null
        set(newValue) { field = newValue; snAdapter.openGroupRoom = openGroupRoom }
    var onCandidateSelected: ((Mention) -> Unit)? = null

    @Inject lateinit var threadDb: BeldexThreadDatabase

    private val snAdapter by lazy { Adapter(context) }

    private class Adapter(private val context: Context) : BaseAdapter() {
        var candidates = listOf<Mention>()
            set(newValue) { field = newValue; notifyDataSetChanged() }
        var glide: GlideRequests? = null
        var openGroupServer: String? = null
        var openGroupRoom: String? = null

        override fun getCount(): Int { return candidates.count() }
        override fun getItemId(position: Int): Long { return position.toLong() }
        override fun getItem(position: Int): Mention { return candidates[position] }

        override fun getView(position: Int, cellToBeReused: View?, parent: ViewGroup): View {
            val cell = cellToBeReused as MentionCandidateView? ?: MentionCandidateView(context)
            val mentionCandidate = getItem(position)
            cell.glide = glide
            cell.candidate = mentionCandidate
            cell.openGroupServer = openGroupServer
            cell.openGroupRoom = openGroupRoom
            return cell
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    init {
        clipToOutline = true
        adapter = snAdapter
        snAdapter.candidates = candidates
        setOnItemClickListener { _, _, position, _ ->
            onCandidateSelected?.invoke(candidates[position])
        }
    }

    fun show(candidates: List<Mention>, threadID: Long) {
        val openGroup = threadDb.getOpenGroupChat(threadID)
        if (openGroup != null) {
            openGroupServer = openGroup.server
            openGroupRoom = openGroup.room
        }
        setMentionCandidates(candidates)
    }

    fun setMentionCandidates(candidates: List<Mention>) {
        this.candidates = candidates
        val layoutParams = this.layoutParams as ViewGroup.LayoutParams
        layoutParams.height = toPx(Math.min(candidates.count(), 4) * 44, resources)
        this.layoutParams = layoutParams
    }

    fun hide() {
        val layoutParams = this.layoutParams as ViewGroup.LayoutParams
        layoutParams.height = 0
        this.layoutParams = layoutParams
    }
}