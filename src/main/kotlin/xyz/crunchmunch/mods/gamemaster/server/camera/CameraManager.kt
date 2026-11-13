package xyz.crunchmunch.mods.gamemaster.server.camera

import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.registry.DynamicRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistries
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistryKeys
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframeTypes
import xyz.crunchmunch.mods.gamemaster.utils.absSnapTo
import xyz.crunchmunch.mods.gamemaster.utils.teleportTo
import java.util.*

object CameraManager {
    val ACTIVE_SCENE_ATTACHMENT = AttachmentRegistry.create(GameMaster.id("camera/active_scene")) { builder ->
        builder.persistent(ResourceKey.codec(GameMasterRegistryKeys.CAMERA_SCENE))
    }

    private val activeScenes: MutableList<ActiveCameraScene> = Collections.synchronizedList(mutableListOf())

    fun init() {
        DynamicRegistries.register(GameMasterRegistryKeys.CAMERA_SCENE, CameraScene.CODEC)
        CameraKeyframeTypes.init()

        ServerTickEvents.END_LEVEL_TICK.register { level ->
            var hasFinished = false
            val finishedScenes by lazy {
                hasFinished = true
                mutableListOf<ActiveCameraScene>()
            }

            for (scene in this.activeScenes) {
                if (scene.level != level)
                    continue

                for ((_, track) in scene.tracks) {
                    track.tick()
                }

                if (scene.tracks.values.all { it.isComplete }) {
                    finishedScenes.add(scene)
                }
            }

            if (hasFinished) {
                for (scene in finishedScenes) {
                    this.endScene(scene)
                }
            }
        }
    }

    private fun CameraScene.buildTracks(level: ServerLevel, camera: Display): Map<ResourceKey<MapCodec<out CameraKeyframe>>, CameraTrack<CameraKeyframe>> {
        val keyframeTracks = mutableMapOf<ResourceKey<MapCodec<out CameraKeyframe>>, MutableMap<Int, CameraKeyframe>>()

        this.keyframes.forEach { tick, keyframe ->
            val key = GameMasterRegistries.CAMERA_KEYFRAME_TYPE.getResourceKey(keyframe.codec).orElseThrow()
            val frames = keyframeTracks.computeIfAbsent(key) { mutableMapOf() }

            if (frames.contains(tick))
                throw IllegalArgumentException("Overlapping keyframe type ${key.identifier()} at tick $tick! (existing: ${frames[tick]!!}, new: $keyframe)")

            frames[tick] = keyframe
        }

        return keyframeTracks.mapValues { CameraTrack(it.value, level, camera, this.key(level)).apply { validate() } }
    }

    private fun CameraScene.key(level: Level): ResourceKey<CameraScene> {
        return level.registryAccess().lookup(GameMasterRegistryKeys.CAMERA_SCENE)
            .flatMap { it.getResourceKey(this) }
            .orElseGet {
                ResourceKey.create(GameMasterRegistryKeys.CAMERA_SCENE,
                    GameMaster.id("preview/${level.dimension().identifier().namespace}/${level.dimension().identifier().path}"))
            }
    }

    fun beginScene(server: MinecraftServer, scene: CameraScene, targets: Collection<ServerPlayer>) {
        if (scene.keyframes.isEmpty)
            throw IllegalStateException("Scene does not contain any keyframes!")

        val level = server.getLevel(scene.dimension) ?: throw IllegalArgumentException("Could not find scene dimension ${scene.dimension.identifier()}!")
        val sceneKey = scene.key(level)

        for (player in targets) {
            player.removeAttached(ACTIVE_SCENE_ATTACHMENT)
        }

        // Remove any orphaned cameras
        level.allEntities.filter { it.type != EntityTypes.PLAYER && it.hasAttached(ACTIVE_SCENE_ATTACHMENT)
            && level.players().none { p -> p.getAttached(ACTIVE_SCENE_ATTACHMENT) == it.getAttached(ACTIVE_SCENE_ATTACHMENT) }
        }
            .forEach { it.remove(Entity.RemovalReason.DISCARDED) }

        val camera = this.createCameraEntity(level, scene)
        val tracks = scene.buildTracks(level, camera)
        level.addFreshEntity(camera)

        this.activeScenes.add(ActiveCameraScene(scene, level, camera, tracks))

        for (player in targets) {
            player.teleportTo(level, scene.start.position().x, scene.start.position().y, scene.start.position().z, scene.start.yRot(), scene.start.xRot())

            player.setGameMode(GameType.SPECTATOR)
            player.setAttached(ACTIVE_SCENE_ATTACHMENT, sceneKey)
            player.setCamera(camera)
        }
    }

    fun endScene(activeScene: ActiveCameraScene) {
        // If the scene was ended prematurely before the queue could finish running,
        // we need to reset all state as best as possible.

        val key = activeScene.scene.key(activeScene.level)

        for ((_, track) in activeScene.tracks) {
            track.stopAndReset()
        }

        for (player in activeScene.level.players()) {
            if (player.getAttached(ACTIVE_SCENE_ATTACHMENT) == key) {
                player.setCamera(null)
                player.setGameMode(GameType.ADVENTURE)
            }
        }

        this.activeScenes.remove(activeScene)
    }

    private fun createCameraEntity(level: ServerLevel, scene: CameraScene): Display {
        if (level.dimension() != scene.dimension)
            throw IllegalArgumentException("Provided scene dimension ${scene.dimension.identifier()} does not match given level dimension ${level.dimension().identifier()}!")

        val entity = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED) ?: throw IllegalStateException("Failed to create shared camera entity!")
        entity.absSnapTo(scene.start)
        entity.isPermanentlyInvulnerable = true
        entity.isInvisible = true
        entity.transformationInterpolationDelay = 0

        return entity
    }
}
