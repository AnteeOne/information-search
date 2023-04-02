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

data class TermsWeight(
    val idfArray: DoubleArray,
    val tfIdfMatrix: Array<DoubleArray>
)

var invertedIndexList: List<InvertedIndex> = listOf()

fun task4(withFileAppend: Boolean = false): TermsWeight {
    val gson = Gson()
    val itemType = object : TypeToken<List<InvertedIndex>>() {}.type
    val invertedIndexFile =
        File(File("${OutputSettings.PATH_NAME}/${TaskThirdSettings.FOLDER_NAME}"), TaskThirdSettings.FILE_INDEXES_NAME)
    invertedIndexList = gson.fromJson(invertedIndexFile.readText(), itemType)
    val documents = with(TaskSecondSettings) {
        File("${OutputSettings.PATH_NAME}/${FOLDER_NAME}/${FOLDER_TOKENS_NAME}")
            .listFiles()
            .map { it.name to it.readLines() }
    }
    val documentsCount = documents.size

    val tfMatrix = Array(invertedIndexList.size) { DoubleArray(documentsCount) }
    val idfArray = DoubleArray(invertedIndexList.size)
    val tfIdfMatrix = Array(invertedIndexList.size) { DoubleArray(documentsCount) }

    invertedIndexList.forEachIndexed { termIndex, invertedIndex ->
        //Для начала получаем idf для каждого термина
        idfArray[termIndex] = getIdf(invertedIndex, documentsCount)
        documents.forEachIndexed { docIndex, it ->
            val key = it.first to invertedIndex.term
            //Затем получаем tf для связки термин - документ
            tfMatrix[termIndex][docIndex] = getTf(invertedIndex, it.first, it.second)
            //Получаем tf-idf для связки термин - документ как
            //произведение idf(термин) * tf(термин - документ)
            tfIdfMatrix[termIndex][docIndex] =
                tfMatrix[termIndex][docIndex] * idfArray[termIndex]
        }
    }
    if (withFileAppend) {
        saveResultInFiles(tfMatrix, idfArray, tfIdfMatrix, documents)
    }
    return TermsWeight(idfArray, tfIdfMatrix)
}

fun saveResultInFiles(
    tfMatrix: Array<DoubleArray>,
    idfArray: DoubleArray,
    tfIdfMatrix: Array<DoubleArray>,
    documents: List<Pair<String, List<String>>>
) {
    val outputDirectory = File("${OutputSettings.PATH_NAME}/${TaskFourthSettings.FOLDER_NAME}").apply {
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