package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

open class NetworkHelper {

	open val client: OkHttpClient by lazy {
		OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.followRedirects(true)
			.followSslRedirects(true)
			.build()
	}

	open val cloudflareClient: OkHttpClient by lazy { client }

	open fun defaultUserAgentProvider(): String =
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}
