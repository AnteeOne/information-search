package task1

import org.jsoup.nodes.Document
import settings.OutputSettings
import java.io.File
import java.io.FileOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
fun task1(url: String) = with(TaskFirstSettings) {
    val crawlingDuration = measureTime {
        crawle(url)
    }
    println("Crawling completed after ${crawlingDuration.inSeconds} seconds.")
}

private fun crawle(url: String) {
    val outputDirectory = File(OutputSettings.PATH_NAME + "/" + TaskFirstSettings.FOLDER_NAME).apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val indexFile = File(outputDirectory, TaskFirstSettings.INDEX_FILE_NAME).apply { createNewFile() }
    val textFilesDirectory = File(outputDirectory, TaskFirstSettings.FOLDER_FILES_NAME).apply { mkdirs() }
    val predicate = { doc: Document -> doc.body().text().split(' ').size >= 1000 }

    WebCrawler(TaskFirstSettings.PAGES_COUNT, predicate).apply {
        startCrawl(url)
        getPages().forEachIndexed { index, page ->
            FileOutputStream(indexFile, true).bufferedWriter()
                .use { it.append("$index - ${page.first}\n") }
            File(textFilesDirectory, "$index.html").apply {
                createNewFile()
                val docText = page.second
                writeText(docText)
            }
        }
    }
}

object TaskFirstSettings {
    const val FOLDER_NAME = "task1"
    const val FOLDER_FILES_NAME = "htmls"
    const val INDEX_FILE_NAME = "index.txt"

    const val PAGES_COUNT = 100
}