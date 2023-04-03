package task4

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import settings.OutputSettings
import task2.TaskSecondSettings
import task3.InvertedIndex
import task3.TaskThirdSettings
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.log2

// Объект - термин или его лемматизированная форма
data class ObjectWeight(
    val idfArray: DoubleArray,
    val tfIdfMatrix: Array<DoubleArray>
)

var invertedIndexList: List<InvertedIndex> = listOf()

fun task4(withFileAppend: Boolean = false): Pair<ObjectWeight, ObjectWeight> {
    val gson = Gson()
    val itemType = object : TypeToken<List<InvertedIndex>>() {}.type
    val invertedIndexFile =
        File(File("${OutputSettings.PATH_NAME}/${TaskThirdSettings.FOLDER_NAME}"), TaskThirdSettings.FILE_INDEXES_NAME)
    invertedIndexList = gson.fromJson(invertedIndexFile.readText(), itemType)

    // Вычисляем tf-idf для терминов
    val terms = with(TaskSecondSettings) {
        File("${OutputSettings.PATH_NAME}/${FOLDER_NAME}/${FOLDER_TOKENS_NAME}")
            .listFiles()
            .map { it.name to it.readLines() }
    }
    val termsWeight = calcWeight(terms, TaskSecondSettings.FOLDER_TOKENS_NAME)

//    // Вычисляем tf-idf для лемм
//    val lemmas = with(TaskSecondSettings) {
//        File("${OutputSettings.PATH_NAME}/${FOLDER_NAME}/${FOLDER_LEMMAS_NAME}")
//            .listFiles()
//            .map { it.name to it.readLines() }
//    }
//    // Форматируем леммы
//    val formattedLemmas = lemmas.map { lemma ->
//        Pair(lemma.first, lemma.second.map { line ->  line.split(':')[0] })
//    }
//    val lemmasWeight = calcWeight(formattedLemmas, TaskSecondSettings.FOLDER_LEMMAS_NAME)

    return Pair(termsWeight, termsWeight)
}

fun calcWeight(objects: List<Pair<String, List<String>>>, outputDir: String): ObjectWeight {
    val documentsCount = objects.size

    val tfMatrix = Array(invertedIndexList.size) { DoubleArray(documentsCount) }
    val idfArray = DoubleArray(invertedIndexList.size)
    val tfIdfMatrix = Array(invertedIndexList.size) { DoubleArray(documentsCount) }

    invertedIndexList.forEachIndexed { objectIndex, invertedIndex ->
        // Для начала получаем idf для каждого объекта
        idfArray[objectIndex] = getIdf(invertedIndex, documentsCount)
        objects.forEachIndexed { docIndex, it ->
            // Затем получаем tf для связки объект - документ
            tfMatrix[objectIndex][docIndex] = getTf(invertedIndex, it.first, it.second)
            // Получаем tf-idf для связки объекта - документ как
            // Произведение idf(объекта) * tf(объект - документ)
            tfIdfMatrix[objectIndex][docIndex] =
                tfMatrix[objectIndex][docIndex] * idfArray[objectIndex]
        }
    }
    saveResultInFiles(tfMatrix, idfArray, tfIdfMatrix, objects, outputDir)

    return ObjectWeight(idfArray, tfIdfMatrix)
}

fun saveResultInFiles(
    tfMatrix: Array<DoubleArray>,
    idfArray: DoubleArray,
    tfIdfMatrix: Array<DoubleArray>,
    documents: List<Pair<String, List<String>>>,
    outputDir: String
) {
    val outputDirectory = File("${OutputSettings.PATH_NAME}/${TaskFourthSettings.FOLDER_NAME}/${outputDir}").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val df = DecimalFormat("#.#####")
    df.roundingMode = RoundingMode.CEILING
    val tfFile = File(outputDirectory, TaskFourthSettings.TF_FILE_NAME)
    val idfFile = File(outputDirectory, TaskFourthSettings.IDF_FILE_NAME)
    val tfIdfFile = File(outputDirectory, TaskFourthSettings.TFID_FILE_NAME)
    val maxCount = invertedIndexList.maxOf { it.term.length } + 1
    val documentRow = StringBuilder("".padEnd(maxCount))
    documents.forEach {
        documentRow.append(it.first.padEnd(8))
    }
    documentRow.appendLine()
    val tfTable = StringBuilder(documentRow)
    val idfTable = StringBuilder()
    val tfIdfTable = StringBuilder(documentRow)

    tfMatrix.forEachIndexed { index, doubles ->
        val rowBuilder = StringBuilder(invertedIndexList[index].term.padEnd(maxCount))
        doubles.forEach {
            rowBuilder.append(df.format(it).padEnd(8))
        }
        tfTable.appendLine(rowBuilder.toString())
    }

    idfArray.forEachIndexed { index, d ->
        idfTable.append(invertedIndexList[index].term.padEnd(maxCount))
            .append(df.format(d).padEnd(8))
            .appendLine()
    }

    tfIdfMatrix.forEachIndexed { index, doubles ->
        val rowBuilder = StringBuilder(invertedIndexList[index].term.padEnd(maxCount))
        doubles.forEach {
            rowBuilder.append(df.format(it).padEnd(8))
        }
        tfIdfTable.appendLine(rowBuilder.toString())
    }
    tfFile.writeText(tfTable.toString())
    idfFile.writeText(idfTable.toString())
    tfIdfFile.writeText(tfIdfTable.toString())
}

fun getTf(invertedIndex: InvertedIndex, docName: String, doc: List<String>): Double {
    val wordCountInDoc = invertedIndex.locations
        .filter { it.fileName == docName }.size.toDouble()
    return wordCountInDoc / doc.size
}

fun getIdf(invertedIndex: InvertedIndex, documentsCount: Int): Double {
    val docsWithWordCount = invertedIndex.locations.distinctBy { it.fileName }.size
    return log2(documentsCount.toDouble() / docsWithWordCount)
}

object TaskFourthSettings {
    const val FOLDER_NAME = "task4"
    const val TF_FILE_NAME = "tfTable.txt"
    const val IDF_FILE_NAME = "idfTable.txt"
    const val TFID_FILE_NAME = "tfIdfTable.txt"
}