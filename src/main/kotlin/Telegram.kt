import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val BOT_TOKEN = "8591825097:AAF0jbqDCJV1xfHkA-ZFp-O1XyF1kNz2GcM"

fun main() {

    val baseUrl = "https://api.telegram.org/bot$BOT_TOKEN"
    val client = HttpClient.newBuilder().build()

    val urlGetMe = "$baseUrl/getMe"
    val requestGetMe = HttpRequest.newBuilder()
        .uri(URI.create(urlGetMe))
        .build()

    val responseGetMe = client.send(requestGetMe, HttpResponse.BodyHandlers.ofString())

    println("Ответ на getMe:")
    println(responseGetMe.body())
    println()

    val urlGetUpdates = "$baseUrl/getUpdates"
    val requestGetUpdates = HttpRequest.newBuilder()
        .uri(URI.create(urlGetUpdates))
        .build()

    val responseGetUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    println("Ответ на getUpdates:")
    println(responseGetUpdates.body())
}