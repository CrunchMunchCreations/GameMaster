package xyz.crunchmunch.mods.gamemaster.players.metrics

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.crunchmunch.mods.gamemaster.GameMaster
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.*

/**
 * Represents metric data for players. These files get stored into the server's root directory,
 * under "crunchmunch/gamemaster/metrics/{namespace}/{path}/{uuid}.json", where each JSON file represents a player.
 *
 * The manager lazily loads metric data, only retrieving them when actually needed. This allows a reduced memory footprint
 * for the manager.
 *
 * @property [dataCodec] Represents the codec used for serializing and deserializing the metric data into JSON.
 * @property [defaultData] Represents the default data, used if the player doesn't initially exist in the metrics.
 * @property [id] Represents the ID used for storing the metrics.
 * @property [saveInterval] Represents an interval, in ticks, for when the data should save automatically.
 */
class MetricManager<T>(
    dataCodec: Codec<T>,
    val defaultData: () -> T,
    val id: ResourceLocation,

    private val saveInterval: Int = 100
) : Iterable<MetricData<T>> {
    private val metricsPath = GameMaster.resolvePath("metrics/${id.namespace}/${id.path}")
    val codec = MetricData.makeCodec(dataCodec)

    private val metrics = Collections.synchronizedMap(mutableMapOf<UUID, MetricData<T>>())

    init {
        // Periodic data saves
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (server.tickCount % saveInterval == 0) {
                save()
            }
        }

        // Save on server stop
        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            save()
        }
    }

    /**
     * Retrieves metrics by a player's UUID, if available. Also tries to lazily load the metric data if needed.
     * @return Metric data for a player if it exists, or null if not.
     */
    fun get(uuid: UUID): MetricData<T>? {
        return metrics.computeIfAbsent(uuid) { tryLoad(uuid) }
    }

    /**
     * Retrieves metrics by a player's profile, and creates one if unavailable. Also tries to lazily load the metric data if needed.
     */
    fun get(profile: GameProfile): MetricData<T> {
        return metrics.computeIfAbsent(profile.id) { tryLoad(profile.id) ?: MetricData(profile, defaultData.invoke()) }
    }

    fun get(player: Player): MetricData<T> {
        return get(player.gameProfile)
    }

    /**
     * Direct access to the wrapped metric data rather than the wrapper data.
     */
    fun getDirect(player: Player): T? {
        return get(player.gameProfile).data
    }

    /**
     * Tries to load the metric data from a file. Note that this doesn't store the metric data into the map on its own.
     * @return The deserialized metric data, or null if the file doesn't exist.
     */
    fun tryLoad(uuid: UUID): MetricData<T>? {
        val playerMetricsPath = metricsPath.resolve("$uuid.json")
        if (playerMetricsPath.exists()) {
            return try {
                codec.decode(JsonOps.INSTANCE, JsonParser.parseReader(playerMetricsPath.reader(Charsets.UTF_8, StandardOpenOption.READ)))
                    .orThrow.first
            } catch (e: Throwable) {
                try {
                    val systemTime = System.currentTimeMillis()
                    val backupPath = metricsPath.resolve("${uuid}_$systemTime.json.bkp")
                    playerMetricsPath.copyTo(backupPath, StandardCopyOption.REPLACE_EXISTING)
                    logger.error("Failed to load metric data for $uuid! File contents have been dumped to ${uuid}_$systemTime.json.bkp in case of an overwrite!")
                    e.printStackTrace()
                } catch (e2: Throwable) {
                    logger.error("Failed to load metric data for $uuid! The data backup procedure has also failed, do watch the exceptions!")

                    e2.addSuppressed(e)
                    e2.printStackTrace()
                }

                null
            }
        }

        return null
    }

    /**
     * Saves any data that has been modified onto disk.
     * [forceAll] forces all files that are in the manager to be written.
     */
    fun save(forceAll: Boolean = false) {
        for ((uuid, metricData) in metrics) {
            if (!metricData.isDirty && !forceAll)
                continue

            save(uuid)
        }
    }

    /**
     * Saves the data for this UUID, regardless of if the data has been modified.
     * Does nothing if the player UUID hasn't been loaded into the metric manager.
     */
    fun save(uuid: UUID) {
        val metricData = metrics[uuid] ?: return
        val playerMetricsPath = metricsPath.resolve("$uuid.json")

        if (!playerMetricsPath.exists()) {
            if (!playerMetricsPath.parent.exists())
                playerMetricsPath.createParentDirectories()

            playerMetricsPath.createFile()
        }

        playerMetricsPath.writeText(gson.toJson(codec.encodeStart(JsonOps.INSTANCE, metricData).orThrow), options = arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE))
        metricData.isDirty = false
    }

    override fun iterator(): Iterator<MetricData<T>> {
        return this.metrics.values.iterator()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MetricManager::class.java)
        private val gson = GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }
}