package ch.srgssr.pillarbox.monitoring.test

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

fun createDispatcher(responseMap: Map<Pair<String, String>, MockResponse>): Dispatcher {
  return object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      val key = request.method to request.path
      return responseMap[key] ?: MockResponse().setResponseCode(404)
    }
  }
}
