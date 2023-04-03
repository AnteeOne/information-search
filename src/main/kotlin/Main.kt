import task1.task1
import task2.task2
import task3.task3
import task4.task4
import task5.task5

fun main() {
    val url = "https://habr.com/ru/"
//    task1(url)
//    task2()
//   task3()
    val termsWeight = task4()
    val query = "вакансии android разработчик"
    task5(termsWeight.first, query)
}
