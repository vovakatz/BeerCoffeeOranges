import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.*
import java.util.ArrayList
import java.util.regex.Pattern


class Main {
    companion object {

        var threadCount = 0
        var data = Collections.synchronizedList(mutableListOf<JSONObject>())
        var data1 = Collections.synchronizedList(mutableListOf<SensorData>())

        @JvmStatic fun main(args: Array<String>) {
            for (url in getSensorUrls()) {
                increment()
                while (threadCount > 20){
                    Thread.sleep(100)
                }
                thread (start = true) {
                    try {
                        getData(url)
                    } finally {
                        decrement()
                    }
                }
            }
            while (threadCount != 0){
                Thread.sleep(100)
            }

            data = data.sortedWith(compareBy({it.get("level").toString().toInt()},{it.get("@iot.id").toString().toInt()}))
            val output = data.map {
                "${it["location"]},${it["@iot.id"]},${it["type"]},${it["level"]}"
            }
            println(output.joinToString("\n"))
        }

        @Synchronized fun increment() = threadCount++
        @Synchronized fun decrement() = threadCount--

        private fun getData(url: String) {
            val sensorLoc = URL(url)

            with(sensorLoc.openConnection() as HttpURLConnection) {
                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = it.readText()
                    data1.add(SensorData(response))
                    val parser = JSONParser()
                    val jsonObj = parser.parse(response) as JSONObject

                    data.add(jsonObj)
                }
            }
        }

        private fun getSensorUrls(): List<String> {
            val url = URL("http://wwtelemetry.redtech.engineering/")
            val urls = ArrayList<String>()
            with(url.openConnection() as HttpURLConnection) {
                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = it.readText()

                    val matcher = Pattern.compile("<li>(.+?)</li>").matcher(response)
                    while (matcher.find()) {
                        urls.add(matcher.group(1))
                    }
                    return urls
                }
            }
        }

        class SensorData(rawData: String) {
            var id = 0
            var level = 0
            var count = 0
            var selfLink = ""
            var location = ""
            var type = ""

            init {
                val rawArray = rawData.substring(1, rawData.length - 2).split(", \"")
                rawArray.forEach {
                    val keyVal = it.split("\":")
                    val key = keyVal[0].replace("\"", "").trim()
                    val value = keyVal[1].replace("\"", "").trim()
                    when (key) {
                        "@iot.id" -> id = value.toInt()
                        "level" -> level = value.toInt()
                        "@iot.count" -> count = value.toInt()
                        "@iot.selfLink" -> selfLink = value
                        "location" -> location = value
                        "type" -> type = value
                    }
                }
            }


        }
    }
}