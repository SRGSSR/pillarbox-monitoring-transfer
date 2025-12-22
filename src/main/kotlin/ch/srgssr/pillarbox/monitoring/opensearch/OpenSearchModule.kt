package ch.srgssr.pillarbox.monitoring.opensearch

import ch.srgssr.pillarbox.monitoring.opensearch.repository.EventRepository
import ch.srgssr.pillarbox.monitoring.opensearch.setup.AliasSetupTask
import ch.srgssr.pillarbox.monitoring.opensearch.setup.ISMPolicySetupTask
import ch.srgssr.pillarbox.monitoring.opensearch.setup.IndexSetupTask
import ch.srgssr.pillarbox.monitoring.opensearch.setup.IndexTemplateSetupTask
import ch.srgssr.pillarbox.monitoring.opensearch.setup.OpenSearchSetupService
import ch.srgssr.pillarbox.monitoring.opensearch.setup.OpenSearchSetupTask
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module providing OpenSearch-related components.
 *
 * Provides:
 * - A configured [HttpClient] for OpenSearch requests.
 * - [OpenSearchSetupService] for orchestrating setup tasks.
 * - [EventRepository] for accessing event data in OpenSearch.
 *
 * @see HttpClient
 * @see OpenSearchSetupTask
 * @see OpenSearchSetupService
 * @see EventRepository
 */
fun openSearchModule() =
  module {
    single<HttpClient>(named("openSearchHttpClient")) {
      val config: OpenSearchConfig = get()

      HttpClient(CIO) {
        engine {
          requestTimeout = config.timeoutMillis
        }

        install(HttpTimeout) {
          requestTimeoutMillis = config.timeoutMillis
          connectTimeoutMillis = config.timeoutMillis
          socketTimeoutMillis = config.timeoutMillis
        }

        defaultRequest {
          url(config.uri.toString())
          contentType(ContentType.Application.Json)
        }
      }
    }

    single { ISMPolicySetupTask(get(named("openSearchHttpClient"))) }
    single { IndexTemplateSetupTask(get(named("openSearchHttpClient"))) }
    single { IndexSetupTask(get(named("openSearchHttpClient"))) }
    single { AliasSetupTask(get(named("openSearchHttpClient"))) }

    single<List<OpenSearchSetupTask>> {
      listOf(
        get<ISMPolicySetupTask>(),
        get<IndexTemplateSetupTask>(),
        get<IndexSetupTask>(),
        get<AliasSetupTask>(),
      )
    }

    single {
      OpenSearchSetupService(
        get(named("openSearchHttpClient")),
        get(),
        get(),
      )
    }

    single { EventRepository(get(named("openSearchHttpClient")), get()) }
  }
