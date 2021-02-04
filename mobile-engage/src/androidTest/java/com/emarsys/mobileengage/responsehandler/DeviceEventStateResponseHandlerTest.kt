package com.emarsys.mobileengage.responsehandler

import com.emarsys.common.feature.InnerFeature
import com.emarsys.core.endpoint.ServiceEndpointProvider
import com.emarsys.core.feature.FeatureRegistry
import com.emarsys.core.request.model.RequestModel
import com.emarsys.core.response.ResponseModel
import com.emarsys.core.storage.StringStorage
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Test
import java.net.URL

class DeviceEventStateResponseHandlerTest {
    companion object {
        private const val EVENT_HOST = "https://mobile-events.eservice.emarsys.net"
        private const val EVENT_BASE = "$EVENT_HOST/v4/apps/%s/events"
    }

    private lateinit var mockRequestModel: RequestModel
    private lateinit var mockEventServiceProvider: ServiceEndpointProvider
    private lateinit var mockEventServiceV4Provider: ServiceEndpointProvider
    private lateinit var mockDeviceEventStateStorage: StringStorage
    private lateinit var handler: DeviceEventStateResponseHandler

    @Before
    fun setup() {
        mockRequestModel = mock {
            on { url } doReturn URL(EVENT_BASE)
        }
        mockEventServiceProvider = mock {
            on { provideEndpointHost() } doReturn EVENT_HOST
        }
        mockEventServiceV4Provider = mock {
            on { provideEndpointHost() } doReturn EVENT_HOST
        }
        mockDeviceEventStateStorage = mock()

        handler = DeviceEventStateResponseHandler(mockEventServiceProvider, mockEventServiceV4Provider, mockDeviceEventStateStorage)
        FeatureRegistry.enableFeature(InnerFeature.EVENT_SERVICE_V4)
    }

    @Test
    fun testShouldHandle_shouldReturnTrue_whenRequestWasSuccessful(){
        val response = buildResponseModel(mockRequestModel)

        val result = handler.shouldHandleResponse(response)

        result shouldBe true
    }

    @Test
    fun testShouldHandle_shouldReturnTrue_whenRequestWasSuccessfulWithDifferentCode(){
        val response = buildResponseModel(mockRequestModel, statusCode = 202)

        val result = handler.shouldHandleResponse(response)

        result shouldBe true
    }

    @Test
    fun testShouldHandle_shouldReturnFalse_whenV4FeatureIsNotEnabled(){
        FeatureRegistry.disableFeature(InnerFeature.EVENT_SERVICE_V4)

        val response = buildResponseModel(mockRequestModel)

        val result = handler.shouldHandleResponse(response)

        result shouldBe false
    }

    @Test
    fun testShouldHandle_shouldReturnFalse_whenNotMobileEngageUrl(){
        whenever(mockRequestModel.url).thenReturn(URL("https://www.test.url.com/test/url/com"))

        val response = buildResponseModel(mockRequestModel)

        val result = handler.shouldHandleResponse(response)

        result shouldBe false
    }

    @Test
    fun testShouldHandle_shouldReturnFalse_whenDeviceEventStateIsNotInTheResponse(){
        val response = buildResponseModel(mockRequestModel, responseBody = "{}")

        val result = handler.shouldHandleResponse(response)

        result shouldBe false
    }

    @Test
    fun testHandleResponse(){
        val response = buildResponseModel(mockRequestModel)

        handler.handleResponse(response)

        verify(mockDeviceEventStateStorage).set(response.parsedBody.getString("deviceEventState"))
    }

    private fun buildResponseModel(requestModel: RequestModel, responseBody: String = "{'deviceEventState': {'123': '456', '78910':'6543'}}", statusCode: Int = 200): ResponseModel {
        return ResponseModel.Builder()
                .statusCode(statusCode)
                .message("OK")
                .body(responseBody)
                .requestModel(requestModel)
                .build()
    }

}