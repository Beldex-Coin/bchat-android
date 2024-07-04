package com.thoughtcrimes.securesms.groups

import android.os.Bundle
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityOpenGroupGuidelinesBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity

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
        binding.communityGuidelinesTextView.text = """
BChat is a decentralized messaging platform that protects your privacy. When you're using BChat, you own your conversations and data. It does not collect or store any of your personal information. BChat is where you chat with freedom.

BChat is also more than a messaging application. You can send and receive BDX right from your chat box. 

BChat is built on top of the Beldex network. Masternodes on the Beldex network store and relay encrypted messages between clients. Other projects that are currently being researched or developed by Beldex include BelNet (a decentralized VPN service), Beldex Browser (an ad-free Web3 browser), and the Beldex Privacy Protocol (to anonymize every other asset).

To know more, visit: https://www.beldex.io
For collabs or marketing proposals, contact marketing@beldex.io.
For investments, contact invest.bchat@beldex.io 

To protect and preserve this community, kindly follow the group rules and guidelines.

Be civil. You can share your opinions and constructive criticisms but harassment is not permitted.
Don't promote or shill your token/project. This group is dedicated to BChat and the Beldex ecosystem.
Spammers will be banned. 
Do not share NSFW content or use profane language.

Beware of scammers. Admins will not DM you first.""".trimIndent()

    }
}