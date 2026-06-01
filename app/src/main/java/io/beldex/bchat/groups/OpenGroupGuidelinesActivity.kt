package io.beldex.bchat.groups

import android.os.Bundle
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityOpenGroupGuidelinesBinding
import io.beldex.bchat.BaseActionBarActivity

class OpenGroupGuidelinesActivity : BaseActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOpenGroupGuidelinesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        with(binding) {
            back.setOnClickListener { finish() }
            title.text = getString(R.string.ConversationActivity_open_group_guidelines)
        }
        binding.communityGuidelinesTextView.text = getString(R.string.community_guidelines)

    }
}