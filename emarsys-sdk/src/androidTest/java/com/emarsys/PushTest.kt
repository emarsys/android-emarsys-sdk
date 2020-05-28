package com.emarsys

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.emarsys.core.api.result.CompletionListener
import com.emarsys.core.di.DependencyInjection
import com.emarsys.core.di.getDependency
import com.emarsys.di.FakeDependencyContainer
import com.emarsys.mobileengage.api.event.EventHandler
import com.emarsys.mobileengage.push.PushInternal
import com.emarsys.push.Push
import com.emarsys.testUtil.TimeoutUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.Mockito

class PushTest {
    private lateinit var mockPushInternal: PushInternal
    private lateinit var mockCompletionListener: CompletionListener
    private lateinit var mockEventHandler: EventHandler
    private lateinit var push: Push

    @Rule
    @JvmField
    val timeout: TestRule = TimeoutUtils.timeoutRule

    @Before
    fun setUp() {
        mockPushInternal = Mockito.mock(PushInternal::class.java)
        mockCompletionListener = Mockito.mock(CompletionListener::class.java)
        mockEventHandler = Mockito.mock(EventHandler::class.java)
        val dependencyContainer = FakeDependencyContainer(pushInternal = mockPushInternal)

        DependencyInjection.setup(dependencyContainer)

        push = Push()
    }

    @After
    fun tearDown() {
        try {
            val handler = getDependency<Handler>("coreSdkHandler")
            val looper: Looper? = handler.looper
            looper?.quit()
            DependencyInjection.tearDown()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testPush_trackMessageOpen_delegatesTo_mobileEngageInternal() {
        val intent = Mockito.mock(Intent::class.java)
        push.trackMessageOpen(intent)
        Mockito.verify(mockPushInternal).trackMessageOpen(intent, null)
    }

    @Test
    fun testPush_trackMessageOpenWithCompletionListener_delegatesTo_mobileEngageInternal() {
        val intent = Mockito.mock(Intent::class.java)
        push.trackMessageOpen(intent, mockCompletionListener)
        Mockito.verify(mockPushInternal).trackMessageOpen(intent, mockCompletionListener)
    }

    @Test
    fun testPush_setPushToken_delegatesTo_mobileEngageInternal() {
        val pushToken = "PushToken"
        push.setPushToken(pushToken)
        Mockito.verify(mockPushInternal).setPushToken(pushToken, null)
    }

    @Test
    fun testPush_setPushToken_completionListener_delegatesTo_mobileEngageInternal() {
        val pushToken = "PushToken"
        push.setPushToken(pushToken, mockCompletionListener)
        Mockito.verify(mockPushInternal).setPushToken(pushToken, mockCompletionListener)
    }

    @Test
    fun testPush_removePushToken_delegatesTo_mobileEngageInternal() {
        push.clearPushToken()
        Mockito.verify(mockPushInternal).clearPushToken(null)
    }

    @Test
    fun testPush_removePushTokenWithCompletionListener_delegatesTo_mobileEngageInternal() {
        push.clearPushToken(mockCompletionListener)
        Mockito.verify(mockPushInternal).clearPushToken(mockCompletionListener)
    }

    @Test
    fun testPush_setNotificationEventHandler_delegatesTo_pushInternal() {
        push.setNotificationEventHandler(mockEventHandler)
        Mockito.verify(mockPushInternal).setNotificationEventHandler(mockEventHandler)
    }

    @Test
    fun testPush_setSilentMessageEventHandler_delegatesTo_pushInternal() {
        push.setSilentMessageEventHandler(mockEventHandler)
        Mockito.verify(mockPushInternal).setSilentMessageEventHandler(mockEventHandler)
    }
}