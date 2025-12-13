import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val BOT_TOKEN = "8591825097:AAF0jbqDCJV1xfHkA-ZFp-O1XyF1kNz2GcM"

fun main() {

    var updateId = 0

    while (true) {
        Thread.sleep(2000)

        val updates = getUpdates(updateId)
        println(updates)

        val updateIdRegex = "\"update_id\":(\\d+)".toRegex()
        val lastUpdateIdMatch = updateIdRegex.findAll(updates).lastOrNull()

        if (lastUpdateIdMatch == null) {
            continue
        }

        val lastUpdateId = lastUpdateIdMatch.groupValues[1].toInt()
        updateId = lastUpdateId + 1

        val messageTextRegex = "\"text\":\"(.+?)\"".toRegex()
        val lastTextMatch = messageTextRegex.findAll(updates).lastOrNull()
        val text = lastTextMatch?.groupValues?.get(1)

        println("update_id = $lastUpdateId")
        println("text = $text")
        println("====================================")
    }
}

fun getUpdates(updateId: Int): String {
    val url = "https://api.telegram.org/bot$BOT_TOKEN/getUpdates?offset=$updateId"

    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .build()

    val response: HttpResponse<String> =
        client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}