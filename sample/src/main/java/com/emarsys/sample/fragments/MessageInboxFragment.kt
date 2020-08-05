package com.emarsys.sample.fragments

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.emarsys.Emarsys
import com.emarsys.sample.R
import com.emarsys.sample.TagChangeListener
import com.emarsys.sample.adapters.MessageInboxAdapter
import com.emarsys.sample.extensions.showSnackBar
import kotlinx.android.synthetic.main.fragment_inbox.inboxView
import kotlinx.android.synthetic.main.fragment_inbox.swipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_message_inbox.*

class MessageInboxFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, TagChangeListener {

    private companion object {
        val TAG: String = MessageInboxFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_message_inbox, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageInboxRecycleView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val handlerThread = HandlerThread("imageDownloadThread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        messageInboxRecycleView.adapter = MessageInboxAdapter(this, handler)

        loadMessages()

        swipeRefreshLayout.setOnRefreshListener {
            onRefresh()
            refreshHint.visibility=View.GONE
        }
    }

    override fun onRefresh() {
        swipeRefreshLayout.isRefreshing = true
        loadMessages()
    }

    private fun loadMessages() {
        Emarsys.messageInbox.fetchMessages {
            it.result?.let { notificationStatus ->

                notificationStatus.messages.forEach { notification ->
                    Log.i(TAG, "Messages: ${notification.title}")
                }

                (messageInboxRecycleView.adapter as MessageInboxAdapter).addItems(notificationStatus.messages)
            }

            it.errorCause?.let { cause ->
                inboxView.showSnackBar("Error fetching messages: ${cause.message}")
            }
        }
        swipeRefreshLayout.isRefreshing = false
    }

    override fun addTagClicked(messageId: String) {
        val tag = tagEditText.text.toString()
        if (tag.isNotEmpty()) {
            Emarsys.messageInbox.addTag(tag, messageId)
            loadMessages()
        }
    }

    override fun removeTagClicked(messageId: String) {
        val tag = tagEditText.text.toString()
        if (tag.isNotEmpty()) {
            Emarsys.messageInbox.removeTag(tag, messageId)
            loadMessages()
        }
    }
}
