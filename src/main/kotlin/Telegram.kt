import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val BOT_TOKEN = "8591825097:AAF0jbqDCJV1xfHkA-ZFp-O1XyF1kNz2GcM"

fun main() {
    val service = TelegramBotService(BOT_TOKEN)

    var updateId = 0

    while (true) {
        Thread.sleep(2000)

        val updates = service.getUpdates(updateId)
        println(updates)

        val updateIdRegex = "\"update_id\":\\s*(\\d+)".toRegex()
        val updateIdMatch = updateIdRegex.findAll(updates).lastOrNull() ?: continue
        val lastUpdateId = updateIdMatch.groupValues[1].toInt()
        updateId = lastUpdateId + 1

        val chatIdRegex = "\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-?\\d+)".toRegex()
        val chatIdMatch = chatIdRegex.findAll(updates).lastOrNull() ?: continue
        val chatId = chatIdMatch.groupValues[1]

        val textRegex = "\"text\":\\s*\"(.+?)\"".toRegex()
        val textMatch = textRegex.findAll(updates).lastOrNull() ?: continue
        val text = textMatch.groupValues[1]

        println("update_id = $lastUpdateId")
        println("chat_id = $chatId")
        println("text = $text")
        println("====================================")

        if (text == "Hello") {
            service.sendMessage(chatId, "Hello")
        }
    }
}

class TelegramBotService(private val token: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val url = "https://api.telegram.org/bot$token/getUpdates?offset=$updateId"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: String, text: String): String {
        val url = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$text"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}