package task3

import com.bpodgursky.jbool_expressions.*
import com.bpodgursky.jbool_expressions.parsers.ExprParser
import com.bpodgursky.jbool_expressions.rules.RuleSet
import com.google.gson.Gson
import settings.OutputSettings
import task2.TaskSecondSettings
import task3.TaskThirdSettings.FILE_INDEXES_NAME
import java.io.File
import java.io.FileOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val invertedIndex = mutableMapOf<String, MutableList<Location>>()
lateinit var expressionVariables: Map<String, String>
lateinit var documentLocations: Set<String>

@OptIn(ExperimentalTime::class)
fun task3() {
    measureTime {
        generateInvertedIndex()
    }.also { duration ->
        println("Генерация инвертированного списка терминов завершилась за $duration")
    }
    measureTime {
        val expression = "A & (!B | C) & D"
        val map = mapOf(
            "A" to "потоки",
            "B" to "разработчик",
            "C" to "программист",
            "D" to "профиль",
        )
        booleanSearch(expression,map)
    }.also { duration ->
        println("Поиск завершился за $duration")
    }
}

// Генерим инвертированный список терминов
fun generateInvertedIndex() {
    val files = with(TaskSecondSettings) {
        File("${OutputSettings.PATH_NAME}/$FOLDER_NAME/$FOLDER_TOKENS_NAME").listFiles()
    }
    files?.forEach { indexFile(it) }
    val gson = Gson()
    val outputDirectory = File("${OutputSettings.PATH_NAME}/${TaskThirdSettings.FOLDER_NAME}").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val file = File(outputDirectory, FILE_INDEXES_NAME).apply { createNewFile() }
    FileOutputStream(file, true).bufferedWriter().use { output ->
        val invertedIndexList = invertedIndex.map { InvertedIndex(it.key, it.value) }
        output.appendLine(gson.toJson(invertedIndexList))
    }
}

// Проходимся по файлу и проводим индексацию слов
fun indexFile(file: File) {
    val fileName = file.name
    file.readLines().forEachIndexed { index, word ->
        var locations = invertedIndex[word]
        if (locations == null) {
            locations = mutableListOf()
            invertedIndex[word] = locations
        }
        locations.add(Location(fileName, index + 1))
    }
}

// Булевый поиск, используется библиотека - https://github.com/bpodgursky/jbool_expressions
fun booleanSearch(expression: String, variablesMap: Map<String, String>) {
    documentLocations = invertedIndex.values.flatten().map { it.fileName }.toSet()
    expressionVariables = variablesMap
    val expr = ExprParser.parse(expression) // Парсим булево выражение из строки
    val simplifiedExpr = RuleSet.simplify(expr) // Упрощаем булево выражение
    val set = iterateExpression(simplifiedExpr)
    set.forEach { println(it) }
    println("Количество найденных документов: ${set.size} шт.")
}

// Проходимся по булевому выражению через рекурсию
fun iterateExpression(expression: Expression<String>): Set<String> {
    return when (expression) {
        is Not -> documentLocations
            .minus(findWordInDocs((expression.e as Variable).value))
            .toSet()
        is Or -> expression.children.map { iterateExpression(it) }.unionFlatten()
        is And -> expression.children.map { iterateExpression(it) }.intersectFlatten()
        is Variable -> findWordInDocs(expression.value)
        else -> setOf()
    }
}

// Возвращает множество документов, в которых содержится заданное слово
fun findWordInDocs(wordToken: String): Set<String> {
    return findWordLocations(wordToken).map { it.fileName }.toSet()
}

// Достаем из мапы по токену слова само слово и ищем его по инвертированному списку
fun findWordLocations(wordToken: String): List<Location> {
    val w = expressionVariables[wordToken]?.toLowerCase()
    return invertedIndex[w]?.toList() ?: listOf()
}

// Возвращаем объединения подвложенных множеств
fun <T> Iterable<Iterable<T>>.unionFlatten(): Set<T> {
    var result = setOf<T>()
    for (element in this) {
        result = result.union(element)
    }
    return result
}

// Возвращаем пересечения подвложенных множеств
fun <T> Iterable<Iterable<T>>.intersectFlatten(): Set<T> {
    var result = first().toSet()
    for (element in drop(1)) {
        result = result.intersect(element)
    }
    return result
}

// Вспомогательный класс, для работы с инвертированным списком
data class InvertedIndex(
    val term: String,
    val locations: MutableList<Location>
)

// Вспомогательный класс, для работы с расположением искомого слова
data class Location(
    val fileName: String,
    val lineIndex: Int
) {
    override fun toString() = "{$fileName, line index $lineIndex}"
}

object TaskThirdSettings {
    const val FOLDER_NAME = "task3"
    const val FILE_INDEXES_NAME = "inverted_index.txt"
}