package task2

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

import java.io.File
import java.io.FileOutputStream
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import settings.OutputSettings
import task1.TaskFirstSettings
import java.io.FileReader
import org.jsoup.Jsoup
import java.io.PrintWriter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
fun task2() {
    val duration = measureTime {
        println("Starting tokenizing and lemmatizing...")
        tokenizeAndLemmatizeHtmls()
    }
    println("Tokenizing and lemmatizing completed after ${duration.inSeconds} seconds.")
}

private fun tokenizeAndLemmatizeHtmls() {
    // Проходимся по папке с html-файлами из первого задания
    val textFilesDirectory = File(OutputSettings.PATH_NAME + "/" + TaskFirstSettings.FOLDER_NAME + "/" + TaskFirstSettings.FOLDER_FILES_NAME)

    with(TaskSecondSettings) {
        // Маппим html-файлы в классы с токенами на русском и анлийском языках
        val tokenizedDocs = textFilesDirectory.listFiles()?.toList()?.map { file ->
            // Парсим текст с html-файлов
            val htmlDoc = Jsoup.parse(file.bufferedReader().use { it.readText() })
            val text = htmlDoc.body().text()

            //Разбиваем текст каждого документа на токены
            val tokens = tokenizeText(text)
            DocumentTokens(
                englishWordTokensList = tokens.filter { it.matches(Regex("[a-zA-Z]+")) },
                russianWordTokensList = tokens.filter { it.matches(Regex("[а-яА-Я]+"))  },
                documentName = file.nameWithoutExtension
            )
        }
        // Создаем директории для файлов с токенами и леммами и очищаем уже имеющиеся директории
        val outputTokensDirectory = File( "${OutputSettings.PATH_NAME}/$FOLDER_NAME/$FOLDER_TOKENS_NAME/").apply {
            mkdirs()
            listFiles()?.forEach { it.deleteRecursively() }
        }
        val outputLemmasDirectory = File( "${OutputSettings.PATH_NAME}/$FOLDER_NAME/$FOLDER_LEMMAS_NAME/").apply {
            mkdirs()
            listFiles()?.forEach { it.deleteRecursively() }
        }

        tokenizedDocs?.forEach {
            // Запысаываем токены
            val resultTokenFile = File(outputTokensDirectory, "tokens_" + it.documentName + ".txt").apply { createNewFile() }
            writeTokens(it.getFilteredWithStopWordsRussianTokens() + it.getFilteredWithStopWordsEnglishTokens(), resultTokenFile)

            //Лемматизация
            val resultLemmasFile = File(outputLemmasDirectory, "lemmas_" + it.documentName + ".txt").apply { createNewFile() }
            resultLemmasFile.printWriter().use { pw ->
                RussianAnalyzer().lemmatizeTokens(it.getFilteredWithStopWordsRussianTokens(), resultLemmasFile, pw)
                EnglishAnalyzer().lemmatizeTokens(it.getFilteredWithStopWordsEnglishTokens(), resultLemmasFile, pw)
            }
        }
    }
}

private fun tokenizeText(text: String): List<String> {
    return text.toLowerCase()
        .replace(Regex("[^a-zA-Zа-яА-Я ]"), " ") //Замена всех знаков препинания и прочих символов на пробелы
        .split(" ") //Разбитие слов на список
        .filter { it != " " && it != "" } //Игнорим пробелы и пустые строки
        .distinct() // Убираем коллизии токенов
}

private fun writeTokens(tokens: List<String>, outputFile: File) {
    FileOutputStream(outputFile, true).bufferedWriter().use { output ->
        tokens.forEach { token ->
            output.appendLine(token)
        }
    }
}

private fun Analyzer.lemmatizeTokens(
    tokens: List<String>,
    outputFile: File,
    printWriter: PrintWriter
) {
    val map = HashMap<String, HashSet<String>>()
    val stream = tokenStream(outputFile.name, tokens.joinToString(" "))
    stream.reset()
    stream.use {
        var i = 0
        while (stream.incrementToken()) {
            val token = tokens[i]
            val lemma = stream.getAttribute(CharTermAttribute::class.java).toString()

            when {
                map[lemma]?.isNotEmpty() == true -> map[lemma]?.add(token)
                else -> map[lemma] = hashSetOf(token)
            }
            i++
        }
    }
    map.forEach { (lemma, lemmaTokens) ->
        printWriter.appendLine("$lemma: ${lemmaTokens.joinToString(" ")}")
    }
}

//Класс для работы с токенами английского и русского языка
data class DocumentTokens(
    private val englishWordTokensList: List<String>,
    private val russianWordTokensList: List<String>,
    val documentName: String
) {
    private val englishStopWords: List<String>
    private val russianStopWords: List<String>

    init {
        val gson = Gson()
        val itemType = object : TypeToken<List<String>>() {}.type
        englishStopWords = gson.fromJson(FileReader(File(TaskSecondSettings.STOP_WORDS_ENGLISH_FILE_NAME)), itemType)
        russianStopWords = gson.fromJson(FileReader(File(TaskSecondSettings.STOP_WORDS_RUSSIAN_FILE_NAME)), itemType)
    }

    //Фильтрация стоп-слов
    fun getFilteredWithStopWordsEnglishTokens(): List<String> = englishWordTokensList.filter { !englishStopWords.contains(it) }

    fun getFilteredWithStopWordsRussianTokens(): List<String> = russianWordTokensList.filter { !russianStopWords.contains(it) }

}
private object TaskSecondSettings {
    const val FOLDER_NAME = "task2"
    const val FOLDER_TOKENS_NAME = "tokens"
    const val FOLDER_LEMMAS_NAME = "lemmas"

    const val STOP_WORDS_ENGLISH_FILE_NAME = "stop_words_english.json"
    const val STOP_WORDS_RUSSIAN_FILE_NAME = "stop_words_russian.json"
}