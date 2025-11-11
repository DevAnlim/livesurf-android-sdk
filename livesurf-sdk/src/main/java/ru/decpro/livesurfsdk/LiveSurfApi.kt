package ru.decpro.livesurfsdk

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.random.Random

/**
 * LiveSurf Android SDK (Kotlin)
 *
 * Клиент для работы с API https://api.livesurf.ru/
 *
 * Особенности:
 *  - Авторизация по API-ключу через заголовок "Authorization"
 *  - Поддержка HTTP-методов: GET, POST, PATCH, DELETE
 *  - Контроль лимита запросов (по умолчанию 10 запросов в секунду)
 *  - Повторы при ответах 429/5xx с экспоненциальным бэкоффом и джиттером
 *  - Все сетевые методы — suspend (вызов из корутины)
 *
 */
class LiveSurfApi(
    private val apiKey: String,
    baseUrlInput: String = "https://api.livesurf.ru/",
    private val timeoutSeconds: Long = 15,
    private val rateLimitPerSec: Int = 10,
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 500
) {

    private val baseUrl: String = baseUrlInput.trimEnd('/') + "/"
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    // Состояние для контроля лимита
    private val timestamps = ArrayDeque<Long>()
    private val mutex = Mutex()

    /**
     * Применить ограничение по частоте запросов.
     * Блокирует выполнение если достигнут лимит запросов в последние 1000 мс.
     */
    private suspend fun applyRateLimit() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            // Удаляем устаревшие метки времени (старше 1000ms)
            while (timestamps.isNotEmpty() && timestamps.first() <= now - 1000) {
                timestamps.removeFirst()
            }
            if (timestamps.size >= rateLimitPerSec) {
                val earliest = timestamps.first()
                val sleepMs = 1000 - (now - earliest)
                if (sleepMs > 0) {
                    // Освобождаем мьютекс перед ожиданием, чтобы не блокировать другие корутины
                    mutex.unlock()
                    try {
                        delay(sleepMs)
                    } finally {
                        mutex.lock()
                    }
                }
            }
            timestamps.addLast(System.currentTimeMillis())
        }
    }

    /**
     * Задержка между повторами с экспоненциальным ростом и джиттером.
     * attempt начинается с 1.
     */
    private suspend fun sleepForRetry(attempt: Int) {
        val base = initialBackoffMs * (2.0.pow((attempt - 1).toDouble())).toLong()
        val jitter = (base * 0.2).toLong()
        val delayMs = base + Random.nextLong(-jitter, jitter + 1)
        delay(delayMs)
    }

    /**
     * Универсальный HTTP-запрос с обработкой повторов.
     *
     * @param method HTTP-метод (GET/POST/PATCH/DELETE)
     * @param endpoint путь относительно baseUrl (можно начать с '/')
     * @param jsonBody тело запроса в виде JSONObject (опционально)
     * @return JSONObject при успешном парсинге или сырая строка ответа
     * @throws IOException при ошибках сети или API
     */
    private suspend fun request(method: String, endpoint: String, jsonBody: JSONObject? = null): Any {
        var attempt = 0
        val url = baseUrl + endpoint.trimStart('/')
        while (true) {
            attempt++
            applyRateLimit()

            val builder = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", apiKey)
                .addHeader("Content-Type", "application/json")

            if (jsonBody != null) {
                val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody.toString())
                builder.method(method.uppercase(), body)
            } else {
                builder.method(method.uppercase(), if (method.uppercase() == "GET" || method.uppercase() == "DELETE") null else RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), ""))
            }

            val request = builder.build()
            try {
                client.newCall(request).execute().use { resp ->
                    val code = resp.code
                    val text = resp.body?.string() ?: ""
                    if (code in 200..299) {
                        // Пытаемся распарсить в JSON, иначе возвращаем сырой текст
                        return try {
                            JSONObject(if (text.isBlank()) "{}" else text)
                        } catch (e: Exception) {
                            text
                        }
                    }

                    // При 429 или 5xx — пробуем повторить (до maxRetries)
                    if ((code == 429 || code >= 500) && attempt <= maxRetries) {
                        sleepForRetry(attempt)
                        continue
                    }

                    val msg = try {
                        JSONObject(if (text.isBlank()) "{}" else text).optString("error", text)
                    } catch (e: Exception) {
                        text
                    }
                    throw IOException("Ошибка API ($code): $msg")
                }
            } catch (e: Exception) {
                if (attempt <= maxRetries) {
                    sleepForRetry(attempt)
                    continue
                }
                throw e
            }
        }
    }

    // Удобные обёртки (suspend)
    suspend fun get(endpoint: String) = request("GET", endpoint, null)
    suspend fun post(endpoint: String, body: JSONObject? = null) = request("POST", endpoint, body)
    suspend fun patch(endpoint: String, body: JSONObject? = null) = request("PATCH", endpoint, body)
    suspend fun delete(endpoint: String) = request("DELETE", endpoint, null)

    // Высокоуровневые методы API
    suspend fun getCategories() = get("categories/")
    suspend fun getCountries() = get("countries/")
    suspend fun getLanguages() = get("languages/")
    suspend fun getSourcesAd() = get("sources/ad/")
    suspend fun getSourcesMessengers() = get("sources/messengers/")
    suspend fun getSourcesSearch() = get("sources/search/")
    suspend fun getSourcesSocial() = get("sources/social/")
    suspend fun getUser() = get("user/")
    suspend fun setAutoMode() = post("user/automode/")
    suspend fun setManualMode() = post("user/manualmode/")
    suspend fun getGroups() = get("group/all/")
    suspend fun getGroup(id: Int) = get("group/$id/")
    suspend fun createGroup(data: JSONObject) = post("group/create/", data)
    suspend fun updateGroup(id: Int, data: JSONObject) = patch("group/$id/", data)
    suspend fun deleteGroup(id: Int) = delete("group/$id/")
    suspend fun cloneGroup(id: Int, data: JSONObject? = null) = post("group/$id/clone/", data)
    suspend fun addGroupCredits(id: Int, credits: Int) = post("group/$id/add_credits/", JSONObject().put("credits", credits))
    suspend fun getPage(id: Int) = get("page/$id/")
    suspend fun createPage(data: JSONObject) = post("page/create/", data)
    suspend fun updatePage(id: Int, data: JSONObject) = patch("page/$id/", data)
    suspend fun deletePage(id: Int) = delete("page/$id/")
    suspend fun clonePage(id: Int) = post("page/$id/clone/")
    suspend fun movePageUp(id: Int) = post("page/$id/up/")
    suspend fun movePageDown(id: Int) = post("page/$id/down/")
    suspend fun startPage(id: Int) = post("page/$id/start/")
    suspend fun stopPage(id: Int) = post("page/$id/stop/")
    suspend fun getStats(params: Map<String, String>) : Any {
        val query = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return get("pages-compiled-stats/?$query")
    }
}
