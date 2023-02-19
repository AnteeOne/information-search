package task1

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*
import kotlin.collections.HashSet

class WebCrawler(
    private val pagesCount: Int,
    private val pagePredicate: (Document) -> Boolean
) {

    companion object {
        private val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"
    }

    private val pagesVisited = HashSet<String>()
    private val pagesToVisit = LinkedList<String>()
    private val linksAndPageTexts: MutableList<Pair<String, String>> = mutableListOf()

    fun startCrawl(url: String) {

        while (linksAndPageTexts.size < pagesCount) {
            val currentUrl: String

            if (pagesToVisit.isEmpty()) {
                currentUrl = url
                pagesVisited.add(url)
            } else {
                currentUrl = nextUrl()
            }

            crawl(currentUrl)
        }

        println("\n[Done] Visited " + pagesVisited.size + " web page(s)")
    }

    private fun nextUrl(): String {
        var nextUrl: String
        do {
            nextUrl = pagesToVisit.removeAt(0)
        } while (pagesVisited.contains(nextUrl))
        pagesVisited.add(nextUrl)
        return nextUrl
    }

    private fun crawl(url: String): Boolean {
        try {
            val connection = Jsoup.connect(url).userAgent(USER_AGENT)
            val htmlDocument = connection.get()

            val response = connection.response()
            if (response.statusCode() == 200) {
                println("\n[Visiting] Received web page at " + url)
            }

            if (!response.contentType().contains("text/html")) {
                println("[Failure] Retrieved something other than HTML")
                return false
            }

            val linksOnPage = htmlDocument.select("a[href]")
            println("[Visiting] Found " + linksOnPage.size + " links")
            linksOnPage.forEach {
                val link = it.absUrl("href").split('#')[0]
                pagesToVisit.add(link)
            }

            if (pagePredicate(htmlDocument)) {
                linksAndPageTexts.add(url to htmlDocument.html())
            }
            return true
        } catch (ioe: IOException) {
            return false
        }
    }

    fun getPages(): List<Pair<String, String>> = linksAndPageTexts
}
