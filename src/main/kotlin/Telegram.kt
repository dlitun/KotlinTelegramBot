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

        val startUpdateId = updates.lastIndexOf("\"update_id\":")
        val endUpdateId = updates.lastIndexOf(",\"message\"")

        if (startUpdateId == -1 || endUpdateId == -1) {
            continue
        }

        val updateIdString =
            updates.substring(startUpdateId + 12, endUpdateId)

        updateId = updateIdString.toInt() + 1
    }
}

fun getUpdates(updateId: Int): String {
    val url =
        "https://api.telegram.org/bot$BOT_TOKEN/getUpdates?offset=$updateId"

    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .build()

    val response =
        client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}