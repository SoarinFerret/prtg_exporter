package org.infobip.prometheus.prtgexporter.prtg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.Response
import org.infobip.prometheus.prtgexporter.AbstractProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap

@Service
class PrtgSensorDataProvider @Autowired constructor(@Value("\${prtg.url:http://127.0.0.1:8080}") val prtgUrl: String,
                                                    @Value("\${prtg.username:}") val prtgUsername: String,
                                                    @Value("\${prtg.passhash:}") val prtgPasshash: String,
                                                    @Value("\${prtg.password:}") val prtgPassword: String,
                                                    @Value("\${prtg.sensors.initial.count:1000}") var sensorCount: Int,
                                                    @Value("\${prtg.sensors.page.size:1000}") val pageSize: Int,
                                                    @Value("\${prtg.sensors.channels.parallelism:200}") val channelsParallelism: Int,
                                                    @Value("\${prtg.sensors.limit:2147483647}") val softLimit: Int,
                                                    @Value("\${prtg.pause:20000}") val pause: Long,
                                                    @Value("\${prtg.sensors.filter.enabled:false}") val filterEnabled: Boolean,
                                                    @Value("\${prtg.sensors.filter.objids:}") val filterSensorIds: Array<String>,
                                                    @Value("\${prtg.sensors.filter.tags:}") val filterTags: Array<String>,
                                                    @Value("\${prtg.sensors.filter.groups:}") val filterGroups: Array<String>) : AbstractProcessor() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val asyncHttpClient = asyncHttpClient()
    private val fetchPrtgAllSensorData = AtomicReference<Collection<PrtgSensorData>>()

    init {
        fetch()
    }

    fun getSensorData(): Collection<PrtgSensorData> {
        return fetchPrtgAllSensorData.get() ?: emptyList()
    }

    override fun process() {
        try {
            fetch()
            Thread.sleep(pause)
        } catch (e: Exception) {
            log.error("Error fetching data", e)
        }
    }

    private fun fetch(): Collection<PrtgSensorData> {
        log.info("Fetching data - start")
        val collection = fetchPrtgAllSensorData(asyncHttpClient)
        fetchPrtgAllSensorData.set(collection)
        log.info("Fetching data - end")
        return collection
    }

    private fun fetchPrtgAllSensorData(asyncHttpClient: AsyncHttpClient): Collection<PrtgSensorData> {
        val allSensors = mutableListOf<PrtgSensorData>()
        var sensors = fetchPrtgSensorData(0, sensorCount, asyncHttpClient)
        allSensors.addAll(sensors)
        while (sensors.size == sensorCount && sensors.size < softLimit) {
            sensors = fetchPrtgSensorData(allSensors.size, pageSize, asyncHttpClient)
            allSensors.addAll(sensors)
        }
        this.sensorCount = allSensors.size
        return allSensors
    }

    private fun fetchPrtgSensorData(start: Int, count: Int, asyncHttpClient: AsyncHttpClient): Collection<PrtgSensorData> {
        var toRequest = count
        var from = start
        val futures = mutableListOf<Future<Response>>()

        var requestSize = Math.min(pageSize, toRequest)
        while (requestSize > 0) {
            var url = append(append("$prtgUrl/api/table.json?content=sensors&columns=objid,device,name,group,tags,lastvalue&start=$from&count=$requestSize&filter_active=-1", "username", prtgUsername),
                    if (prtgPassword.isEmpty()) "passhash" else "password",
                    if (prtgPassword.isEmpty()) prtgPasshash else prtgPassword
                    )
            if (filterEnabled) {
                url += if(filterTags.count() > 0) filterTags.joinToString(",", "&filter_tags=@tag(", ")", -1, "...", {URLEncoder.encode(it, "UTF-8")}) else ""
                url += filterSensorIds.joinToString(separator = "") {"&filter_objid=${URLEncoder.encode(it, "UTF-8")}"}
                url += filterGroups.joinToString(separator = "") {"&filter_group=${URLEncoder.encode(it, "UTF-8")}"}
            }
            futures.add(asyncHttpClient.prepareGet(url).execute())
            toRequest -= requestSize
            from += requestSize
            requestSize = Math.min(pageSize, toRequest)
        }

        return futures.map { parseSensorsResponse(it.get()) }.flatten()
    }

    private fun parseSensorsResponse(response: Response): Collection<PrtgSensorData> {
        val objectMapper = ObjectMapper()
        val prtgResponse = objectMapper.readValue(TokenReplacingStream(TokenReplacingStream(response.responseBodyAsStream, "\"\"".toByteArray(), "null".toByteArray()), "\"No data\"".toByteArray(), "null".toByteArray()), PrtgSensorsResponse::class.java)
        //val prtgResponse = objectMapper.readValue(ReplacingInputStream(response.responseBodyAsStream, "\"\"", "null"), PrtgSensorsResponse::class.java)
        val sensors = prtgResponse.sensors!!
        collectChannels(sensors)
        return sensors
    }

    private fun collectChannels(sensors: Collection<PrtgSensorData>) {
        sensors.chunked(channelsParallelism).map {
            val futures = it.map { it to fetchChannels(it.objid!!) }
            futures.forEach { (sensor, future) ->
                sensor.channels = parseChannelsResponse(future.get())
            }
        }
    }

    private fun fetchChannels(objid: Long): Future<Response> {
        val url = append(append("$prtgUrl/api/table.json?content=channels&columns=objid,name,lastvalue&count=10000&start=0&filter_active=-1&id=$objid", "username", prtgUsername),
                if (prtgPassword.isEmpty()) "passhash" else "password",
                if (prtgPassword.isEmpty()) prtgPasshash else prtgPassword)
        return asyncHttpClient.prepareGet(url).execute()
    }

    private fun parseChannelsResponse(response: Response): Collection<PrtgChannelData> {
        val objectMapper = ObjectMapper()
        //val prtgResponse = objectMapper.readValue(ReplacingInputStream(response.responseBodyAsStream, "\"\"", "null"), PrtgChannelsResponse::class.java)
        val prtgResponse = objectMapper.readValue(TokenReplacingStream(TokenReplacingStream(response.responseBodyAsStream, "\"\"".toByteArray(), "null".toByteArray()), "\"No data\"".toByteArray(), "null".toByteArray()), PrtgChannelsResponse::class.java)
        //val prtgResponse = objectMapper.readValue(TokenReplacingStream(response.responseBodyAsStream, "\"\"".toByteArray(), "null".toByteArray()), PrtgChannelsResponse::class.java)
        return prtgResponse.channels!!
    }

    private fun append(s: String, key: String, value: Any): String {
        return when {
            value.toString().isBlank() -> s
            s.contains("?") -> "$s&${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
            else -> "$s?${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrtgSensorsResponse(
        var sensors: Collection<PrtgSensorData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrtgSensorData(
        var objid: Long? = null,
        var device: String? = null,
        var name: String? = null,
        var group: String? = null,
        var tags: String? = null,
        var lastvalue: String? = null,
        var lastvalue_raw: Double? = null,
        var channels: Collection<PrtgChannelData>? = null,
        var additionalLabels: MutableMap<String, String> = HashMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrtgChannelsResponse(
        var channels: Collection<PrtgChannelData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrtgChannelData(
        var objid: Long? = null,
        var name: String? = null,
        var lastvalue: String? = null,
        var lastvalue_raw: Double? = null
)
