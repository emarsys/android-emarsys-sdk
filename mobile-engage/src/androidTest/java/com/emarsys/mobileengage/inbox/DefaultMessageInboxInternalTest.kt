package com.emarsys.mobileengage.inbox

import android.os.Handler
import android.os.Looper
import com.emarsys.core.CoreCompletionHandler
import com.emarsys.core.api.ResponseErrorException
import com.emarsys.core.api.result.CompletionListener
import com.emarsys.core.api.result.ResultListener
import com.emarsys.core.api.result.Try
import com.emarsys.core.request.RequestManager
import com.emarsys.core.request.RestClient
import com.emarsys.core.request.factory.CompletionHandlerProxyProvider
import com.emarsys.core.request.model.RequestModel
import com.emarsys.core.response.ResponseModel
import com.emarsys.core.storage.StringStorage
import com.emarsys.mobileengage.MobileEngageRequestContext
import com.emarsys.mobileengage.api.inbox.InboxResult
import com.emarsys.mobileengage.fake.FakeRestClient
import com.emarsys.mobileengage.fake.FakeResultListener
import com.emarsys.mobileengage.request.MobileEngageRequestModelFactory
import com.emarsys.testUtil.TimeoutUtils
import com.nhaarman.mockitokotlin2.*
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch

class DefaultMessageInboxInternalTest {
    private companion object {
        private const val TAG = "READ"
        private const val LOWER_CASED_TAG = "read"
        private const val MESSAGE_ID = Integer.MAX_VALUE.toString()
        private const val ADD_EVENT_NAME = "inbox:tag:add"
        private const val REMOVE_EVENT_NAME = "inbox:tag:remove"
    }

    private lateinit var mockRequestManager: RequestManager
    private lateinit var mockRequestContext: MobileEngageRequestContext
    private lateinit var mockMessageInboxResponseMapper: MessageInboxResponseMapper
    private lateinit var mockRequestModelFactory: MobileEngageRequestModelFactory
    private lateinit var mockRequestModel: RequestModel
    private lateinit var mockContactFieldValueStorage: StringStorage
    private lateinit var handler: Handler
    private lateinit var messageInboxInternal: DefaultMessageInboxInternal
    private lateinit var latch: CountDownLatch

    @Rule
    @JvmField
    val timeout: TestRule = TimeoutUtils.timeoutRule

    @Before
    fun setUp() {
        latch = CountDownLatch(1)
        mockMessageInboxResponseMapper = mock {
            on { map(any()) } doReturn InboxResult(listOf())
        }
        mockContactFieldValueStorage = mock {
            on { get() } doReturn "contactFieldValue"
        }
        mockRequestManager = mock()
        mockRequestModel = mock {
            on { id } doReturn "requestId"
        }
        mockRequestContext = mock {
            on { contactFieldValueStorage } doReturn mockContactFieldValueStorage
        }
        mockRequestModelFactory = mock {
            on { createFetchInboxMessagesRequest() } doReturn mockRequestModel
        }

        handler = Handler(Looper.getMainLooper())

        messageInboxInternal = DefaultMessageInboxInternal(
                mockRequestManager,
                mockRequestContext,
                mockRequestModelFactory,
                handler,
                mockMessageInboxResponseMapper
        )
    }

    @Test
    fun testFetchInboxMessages_callsRequestModelFactoryForCreateFetchInboxMessagesRequest_andSubmitsToRequestManager() {
        val mockResultListener = mock<ResultListener<Try<InboxResult>>>()
        messageInboxInternal.fetchMessages(mockResultListener)

        verify(mockRequestModelFactory).createFetchInboxMessagesRequest()
        verify(mockRequestManager).submitNow(eq(mockRequestModel), any())
    }

    @Test
    fun testFetchInboxMessages_resultListener_onSuccess() {
        val mockResponse: ResponseModel = mock {
            on { requestModel } doReturn mockRequestModel
            on { statusCode } doReturn 200
            on { message } doReturn "OK"
        }
        val messageInboxInternal = DefaultMessageInboxInternal(
                requestManagerWithRestClient(FakeRestClient(mockResponse, FakeRestClient.Mode.SUCCESS)),
                mockRequestContext,
                mockRequestModelFactory,
                handler,
                mockMessageInboxResponseMapper
        )

        val fakeResultListener = FakeResultListener<InboxResult>(latch, FakeResultListener.Mode.MAIN_THREAD)

        messageInboxInternal.fetchMessages(fakeResultListener)
        fakeResultListener.latch.await()

        fakeResultListener.successCount shouldBe 1
        verify(mockMessageInboxResponseMapper).map(mockResponse)

    }

    @Test
    fun testFetchInboxMessages_resultListener_onErrorWithResponseModel() {
        val errorResponse: ResponseModel = mock {
            on { requestModel } doReturn mockRequestModel
            on { statusCode } doReturn 500
            on { message } doReturn "error"
            on { body } doReturn "Error happened"
        }
        val messageInboxInternal = DefaultMessageInboxInternal(
                requestManagerWithRestClient(FakeRestClient(errorResponse, FakeRestClient.Mode.ERROR_RESPONSE_MODEL)),
                mockRequestContext,
                mockRequestModelFactory,
                handler,
                mockMessageInboxResponseMapper
        )

        val fakeResultListener = FakeResultListener<InboxResult>(latch, FakeResultListener.Mode.MAIN_THREAD)

        messageInboxInternal.fetchMessages(fakeResultListener)
        fakeResultListener.latch.await()

        val expectedException = ResponseErrorException(
                errorResponse.statusCode,
                errorResponse.message,
                errorResponse.body)

        fakeResultListener.successCount shouldBe 0
        fakeResultListener.errorCount shouldBe 1
        fakeResultListener.errorCause shouldBe expectedException
    }

    @Test
    fun testFetchInboxMessages_resultListener_onErrorWithException() {
        val expectedException = Exception("TestException")

        val messageInboxInternal = DefaultMessageInboxInternal(
                requestManagerWithRestClient(FakeRestClient(expectedException)),
                mockRequestContext,
                mockRequestModelFactory,
                handler,
                mockMessageInboxResponseMapper
        )

        val fakeResultListener = FakeResultListener<InboxResult>(latch, FakeResultListener.Mode.MAIN_THREAD)

        messageInboxInternal.fetchMessages(fakeResultListener)
        fakeResultListener.latch.await()

        fakeResultListener.successCount shouldBe 0
        fakeResultListener.errorCount shouldBe 1
        fakeResultListener.errorCause shouldBe expectedException
    }

    @Test
    fun testTrackAddTag_callsRequestModelFactoryForInternalCustomEventRequest_andSubmitsToRequestManager() {
        val mockCompletionListener: CompletionListener = mock()
        val eventAttributes = mapOf(
                "messageId" to MESSAGE_ID,
                "tag" to LOWER_CASED_TAG
        )
        whenever(mockRequestModelFactory.createInternalCustomEventRequest(ADD_EVENT_NAME, eventAttributes)).thenReturn(mockRequestModel)

        messageInboxInternal.addTag(TAG, MESSAGE_ID, mockCompletionListener)

        verify(mockRequestModelFactory).createInternalCustomEventRequest(ADD_EVENT_NAME, eventAttributes)
        verify(mockRequestManager).submit(mockRequestModel, mockCompletionListener)
    }

    @Test
    fun testRemoveTag_callsRequestModelFactoryForInternalCustomEventRequest_andSubmitsToRequestManager() {
        val mockCompletionListener: CompletionListener = mock()
        val eventAttributes = mapOf(
                "messageId" to MESSAGE_ID,
                "tag" to LOWER_CASED_TAG
        )
        whenever(mockRequestModelFactory.createInternalCustomEventRequest(REMOVE_EVENT_NAME, eventAttributes)).thenReturn(mockRequestModel)

        messageInboxInternal.removeTag(TAG, MESSAGE_ID, mockCompletionListener)

        verify(mockRequestModelFactory).createInternalCustomEventRequest(REMOVE_EVENT_NAME, eventAttributes)
        verify(mockRequestManager).submit(mockRequestModel, mockCompletionListener)
    }

    @Suppress("UNCHECKED_CAST")
    private fun requestManagerWithRestClient(restClient: RestClient): RequestManager {
        val mockProvider: CompletionHandlerProxyProvider = mock {
            on { provideProxy(isNull(), any()) } doAnswer {
                it.arguments[1] as CoreCompletionHandler
            }
            on { provideProxy(any(), any()) } doAnswer {
                it.arguments[1] as CoreCompletionHandler
            }
        }
        return RequestManager(
                mock(),
                mock(),
                mock(),
                mock(),
                restClient,
                mock(),
                mock(),
                mockProvider
        )
    }
}