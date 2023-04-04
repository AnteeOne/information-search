package task5

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import settings.OutputSettings
import task2.TaskSecondSettings
import task3.InvertedIndex
import task3.TaskThirdSettings
import task4.ObjectWeight
import task4.invertedIndexList
import java.io.File
import java.io.FileReader
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

val gson = Gson()
val itemStopWordsType = object : TypeToken<List<String>>() {}.type
val russianStopWords: List<String> =
    gson.fromJson(FileReader("stop_words_russian.json"), itemStopWordsType)

fun task5(termsWeight: ObjectWeight, query: String) {
    val itemInvertedIndexType = object : TypeToken<List<InvertedIndex>>() {}.type
    val invertedIndexFile =
        File(File("${OutputSettings.PATH_NAME}/${TaskThirdSettings.FOLDER_NAME}"), TaskThirdSettings.FILE_INDEXES_NAME)
    invertedIndexList = gson.fromJson(invertedIndexFile.readText(), itemInvertedIndexType)
    val documents = with(TaskSecondSettings) {
        File("${OutputSettings.PATH_NAME}/$FOLDER_NAME/$FOLDER_TOKENS_NAME")
            .listFiles()
    }
    val lemmas = RussianAnalyzer().lemmatizeTokens(query)
    val lemmasIndexes =
        lemmas.map { term -> invertedIndexList.indexOfFirst { it.term == term } }
            .filter { it != -1 }

    // v2 - вектор выражение дополняется до размеров документа
    val queryVector = DoubleArray(invertedIndexList.size)
    lemmasIndexes.forEach { termIndex ->
        val term = invertedIndexList[termIndex].term
        val tf = lemmas.count { it == term } / lemmas.size.toDouble()
        val idf = termsWeight.idfArray[termIndex]
        queryVector[termIndex] = tf * idf
        println("tf-idf of $term = ${tf * idf}")
    }
    if (queryVector.all { it == 0.0 }) {
        println("По запросу \"$query\" ничего не найдено.")
        return
    }
    val docVector = Array(documents.size) { DoubleArray(invertedIndexList.size) }
    documents?.forEachIndexed { docIndex, _ ->
        invertedIndexList.forEachIndexed { termIndex, _ ->
            docVector[docIndex][termIndex] = termsWeight.tfIdfMatrix[termIndex][docIndex]
        }
    }
    val df = DecimalFormat("#.#####")
    df.roundingMode = RoundingMode.CEILING

    println("По запросу \"$query\" найдены следующие документы:")
    docVector
        .mapIndexed { index, doubles -> documents[index].name to doubles }
        .filter { !it.second.all { d -> d == 0.0 } }
        .map { Triple(it.first, cosineSimilarity(it.second, queryVector), it.second.count { d -> d == 0.0 }) }
        .sortedByDescending { it.second }
        .forEach { docItem ->
            val count = lemmasIndexes.map { index -> invertedIndexList[index] }
                .joinToString { item -> "${item.term} - ${item.locations.count { it.fileName == docItem.first }}" }
            println("${docItem.first}\ncosine similarity: ${df.format(docItem.second)}\nzeros count:${docItem.third}\n$count\n")
        }
}

fun cosineSimilarity(vectorA: DoubleArray, vectorB: DoubleArray): Double {
    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0
    for (i in vectorA.indices) {
        dotProduct += vectorA[i] * vectorB[i]
        normA += vectorA[i].pow(2.0)
        normB += vectorB[i].pow(2.0)
    }
    return dotProduct / (sqrt(normA) * sqrt(normB))
}

fun Analyzer.lemmatizeTokens(query: String): List<String> {
    //Приведение к нижнему регистру
    val tokens = query.toLowerCase()
        //Замена всех знаков препинания и прочих символов на пробелы
        //Каждое слово объявляется отдельным токеном
        .replace(Regex("[^a-zA-Zа-яА-Я ]"), " ")
        //Разбиваем слова на список по пробелу
        .split(" ")
        //Фильтруем пробелы и пустые строки
        .filter { it != " " && it != "" }
        //Фильтруем стоп-слова
        .filter { !russianStopWords.contains(it) }
        //Конкатенируем все в одну строку для подачи на вход в библиотеку
        .joinToString(" ")

    val stream = tokenStream("sampleField", tokens)
    val lemmas = mutableListOf<String>()
    stream.reset()
    stream.use {
        while (stream.incrementToken()) {
            val lemma = stream.getAttribute(CharTermAttribute::class.java).toString()
            lemmas.add(lemma)
        }
    }
    return lemmas
}