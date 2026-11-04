package xyz.crunchmunch.mods.gamemaster.animator

import com.google.common.collect.Queues
import com.mojang.math.Transformation
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.*
import xyz.crunchmunch.mods.gamemaster.animator.animation.Animation
import xyz.crunchmunch.mods.gamemaster.animator.animation.AnimationState
import xyz.crunchmunch.mods.gamemaster.animator.animation.LoopType
import xyz.crunchmunch.mods.gamemaster.animator.animation.MultiAnimationDefinition
import xyz.crunchmunch.mods.gamemaster.animator.util.InterpolatingVector3f
import xyz.crunchmunch.mods.gamemaster.utils.*
import java.util.*

open class AnimatableModel(
    val model: ModelDefinition, val animations: MultiAnimationDefinition,
    val level: ServerLevel
) {
    val modelKey: ResourceKey<ModelDefinition> = this.level.registryAccess().lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getResourceKey(model).orElseThrow()
    val animationsKey: ResourceKey<MultiAnimationDefinition> = this.level.registryAccess().lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getResourceKey(animations).orElseThrow()

    lateinit var rootDisplay: Display
        private set
    lateinit var idToDisplayMapping: Map<String, Display>
        private set
    val initialPositions: Map<String, Vector3f> = this.model.collectAllParts()
        .associate { it.id to it.origin.copy() }

    var currentState = AnimationState.STOPPED
        private set

    var currentAnimation: Animation? = null
        private set

    val queuedAnimations: Queue<Animation> = Queues.newArrayDeque()

    private var currentTick = 0

    private var cachedX = 0.0
    private var cachedY = 0.0
    private var cachedZ = 0.0
    private var cachedYaw = 0f
    private var cachedPitch = 0f

    open fun isEntityLoaded(): Boolean {
        return ::rootDisplay.isInitialized && !this.rootDisplay.isRemoved
    }

    open fun isEntityInitialized(): Boolean {
        return ::rootDisplay.isInitialized
    }

    fun getPartById(id: String): Display? {
        return this.idToDisplayMapping[id]
    }

    open fun createNew(pos: Vec3, rootDisplay: Display = EntityTypes.TEXT_DISPLAY.create(this.level, EntitySpawnReason.TRIGGERED)
        ?: throw IllegalStateException("Failed to load root display!")
    ) {
        rootDisplay.width = 1f
        rootDisplay.height = 1f
        rootDisplay.shadowRadius = 0.5f
        this.rootDisplay = rootDisplay

        this.cachedX = pos.x
        this.cachedY = pos.y
        this.cachedZ = pos.z
        this.cachedYaw = rootDisplay.yRot
        this.cachedPitch = rootDisplay.xRot

        this.rootDisplay.snapTo(pos)
        this.rootDisplay.setAttached(AnimatorAttachments.MODEL_KEY, this.modelKey)
        this.rootDisplay.setAttached(AnimatorAttachments.ANIMATIONS_KEY, this.animationsKey)

        this.idToDisplayMapping = this.recursiveLoadParts(model.parts, this.rootDisplay).apply {
            this["root"] = this@AnimatableModel.rootDisplay
        }

        this.level.addFreshEntity(this.rootDisplay)
    }

    open fun loadFromExisting(root: Display) {
        root.width = 1f
        root.height = 1f
        root.shadowRadius = 0.5f
        this.rootDisplay = root

        this.cachedX = root.x
        this.cachedY = root.y
        this.cachedZ = root.z
        this.cachedYaw = root.yRot
        this.cachedPitch = root.xRot

        this.idToDisplayMapping = this.recursiveLoadExistingParts(root).apply {
            this["root"] = root
        }
    }

    fun queueAnimationIfNonePlaying(id: String) {
        if (this.currentAnimation == null)
            this.queueAnimation(id)
    }

    fun queueAnimation(id: String) {
        this.queueAnimation(this.animations.animations[id]!!)
    }

    fun queueAnimation(animation: Animation) {
        this.queuedAnimations.add(animation)
    }

    fun queueAnimationOnce(id: String) {
        this.queueAnimationOnce(this.animations.animations[id]!!)
    }

    fun queueAnimationOnce(animation: Animation) {
        if (!this.queuedAnimations.contains(animation) && this.currentAnimation != animation)
            this.queueAnimation(animation)
    }

    fun overrideAnimation(id: String) {
        this.overrideAnimation(this.animations.animations[id]!!)
    }

    fun overrideAnimation(animation: Animation) {
        if (this.currentAnimation != animation) {
            this.stopAnimation()
            this.queueAnimation(animation)
        }
    }

    fun stopAnimation() {
        this.currentState = AnimationState.STOPPED
    }

    open fun tick(forceUpdate: Boolean = false) {
        val animation = this.currentAnimation
        var hasChanged = forceUpdate

        if (animation != null) {
            // Animation playing
            when (this.currentState) {
                AnimationState.PLAYING -> {
                    if (this.currentTick <= animation.maxLength) {
                        val parts = animation.getPartsAfterTick(currentTick)

                        // First pass, calculate the initial animation transforms
                        for ((id, pair) in parts) {
                            val (endTick, posRotScale) = pair
                            val display = this.idToDisplayMapping[id] ?: continue

                            infix fun Optional<Vector3fc>.neq(attached: InterpolatingVector3f?): Boolean {
                                return this.isPresent && (this.orElseThrow() != attached?.current || endTick != attached.endTick)
                            }

                            if (posRotScale.position neq display.getAttached(AnimatorAttachments.LOCAL_TRANSLATION)
                                || posRotScale.rotation neq display.getAttached(AnimatorAttachments.LOCAL_ROTATION)
                                || posRotScale.scale neq display.getAttached(AnimatorAttachments.LOCAL_SCALE)
                            ) {
                                hasChanged = true
                                posRotScale.position.ifPresent { value ->
                                    display.modifyAttached(AnimatorAttachments.LOCAL_TRANSLATION) { vec: InterpolatingVector3f? ->
                                        (vec ?: InterpolatingVector3f()).update(value, currentTick, endTick)
                                    }
                                }

                                posRotScale.rotation.ifPresent { value ->
                                    display.modifyAttached(AnimatorAttachments.LOCAL_ROTATION) { vec: InterpolatingVector3f? ->
                                        (vec ?: InterpolatingVector3f()).update(value, currentTick, endTick)
                                    }
                                }

                                posRotScale.scale.ifPresent { value ->
                                    display.modifyAttached(AnimatorAttachments.LOCAL_SCALE) { vec: InterpolatingVector3f? ->
                                        (vec ?: InterpolatingVector3f()).update(value, currentTick, endTick)
                                    }
                                }
                            }
                        }
                    } else {
                        // Loop back the animation if we can.
                        when (animation.loop) {
                            LoopType.LOOP -> {
                                if (this.queuedAnimations.isEmpty()) {
                                    this.queueAnimation(animation)
                                }
                            }

                            LoopType.PLAY_ONCE -> {
                                for ((id, display) in this.idToDisplayMapping) {
                                    display.transformationInterpolationDuration = 0
                                    display.transformationInterpolationDelay = 0
                                    display.translation = this.initialPositions[id] ?: Vector3f()
                                    display.leftRotation = Quaternionf()
                                    display.scale = Vector3f(1f, 1f, 1f)

                                    display.removeAttached(AnimatorAttachments.LOCAL_TRANSLATION)
                                    display.removeAttached(AnimatorAttachments.LOCAL_ROTATION)
                                    display.removeAttached(AnimatorAttachments.LOCAL_SCALE)
                                }
                            }

                            LoopType.HOLD_ON_LAST_FRAME -> {}
                        }

                        this.currentState = AnimationState.TRANSITIONING
                        this.currentTick = 0

                        this.tick(true)
                        return
                    }
                }

                AnimationState.TRANSITIONING -> {
                    if (this.currentTick >= animation.loopDelay) {
                        if (this.queuedAnimations.isNotEmpty()) {
                            this.currentAnimation = this.queuedAnimations.poll()
                            this.currentState = AnimationState.PLAYING

                            this.currentTick = 0
                            this.tick()
                            return
                        } else {
                            this.currentAnimation = null
                            this.currentState = AnimationState.STOPPED
                        }

                        this.currentTick = 0
                    }
                }

                AnimationState.STOPPED -> {
                    this.currentAnimation = null
                    this.currentTick = 0
                }
            }
        } else {
            if (this.currentState != AnimationState.STOPPED) {
                this.currentState = AnimationState.STOPPED
                this.currentTick = 0
            }

            if (this.queuedAnimations.isNotEmpty()) {
                this.currentAnimation = this.queuedAnimations.poll()
                this.currentState = AnimationState.PLAYING
                this.currentTick = 0
            }
        }

        // If the rotation was updated externally, make sure to handle that.
        if (this.cachedYaw != this.rootDisplay.yRot || cachedPitch != this.rootDisplay.xRot) {
            hasChanged = true
            this.cachedYaw = this.rootDisplay.yRot
            this.cachedPitch = this.rootDisplay.xRot
        }

        if (!hasChanged) {
            for (display in this.idToDisplayMapping.values) {
                val translation = display.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION)
                val rotation = display.getAttachedOrCreate(AnimatorAttachments.LOCAL_ROTATION)
                val scale = display.getAttachedOrCreate(AnimatorAttachments.LOCAL_SCALE)

                if (InterpolatingVector3f.requiresUpdate(currentTick, translation, rotation, scale)) {
                    hasChanged = true
                    break
                }
            }
        }

        // Second pass, apply to all things down the chain
        // Only actually handle this if anything did animate.
        if (hasChanged) {
            val translation = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION)
            val rotation = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.LOCAL_ROTATION)
            val scale = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.LOCAL_SCALE)
            val rescale = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.RESCALE)

            val estimatedEndTick = estimateEndTick(currentTick, translation, rotation, scale)
            val remainingDuration = estimatedEndTick - currentTick

            val matrixStack = Matrix4fStack(this.idToDisplayMapping.size).apply {
                translate(translation.get(estimatedEndTick))
                scale(scale.get(estimatedEndTick))
                scale(rescale)
                rotateZYX((rotation.get(estimatedEndTick) + Vector3f(rootDisplay.xRot, -rootDisplay.yRot, 0f)) * Mth.DEG_TO_RAD)
            }

            recursiveAnimateHierarchy(this.rootDisplay, currentTick, matrixStack)

            // We have to handle translation and rotation here slightly differently, because we're the root.
            this.rootDisplay.posRotInterpolationDuration = remainingDuration
            this.rootDisplay.transformationInterpolationDelay = 0
            this.rootDisplay.transformationInterpolationDuration = remainingDuration
            this.rootDisplay.translation = translation.get(estimatedEndTick)
            this.rootDisplay.yRot = cachedYaw + rotation.get(estimatedEndTick).y()
            this.rootDisplay.xRot = cachedPitch + rotation.get(estimatedEndTick).x()
        }

        if (this.currentState != AnimationState.STOPPED) {
            this.currentTick++
        }
    }

    open fun remove(includeRoot: Boolean = true) {
        val list = collectEntitiesForRemoval(this.rootDisplay)
        for (entity in list) {
            if (includeRoot || entity != this.rootDisplay) {
                entity.stopRiding()
                entity.remove(Entity.RemovalReason.DISCARDED)
            }
        }
    }

    protected open fun collectEntitiesForRemoval(entity: Entity): MutableList<Entity> {
        val list = mutableListOf<Entity>()
        for (e in entity.passengers) {
            list.addAll(collectEntitiesForRemoval(e))
        }

        list.add(entity)
        return list
    }

    fun estimateEndTick(currentTick: Int, translation: InterpolatingVector3f, rotation: InterpolatingVector3f, scale: InterpolatingVector3f): Int {
        var estimatedEndTick = -1

        if (translation.endTick > 0 && translation.endTick > currentTick)
            estimatedEndTick = translation.endTick

        if (rotation.endTick > 0 && rotation.endTick > currentTick && (estimatedEndTick == -1 || rotation.endTick < estimatedEndTick))
            estimatedEndTick = rotation.endTick

        if (scale.endTick > 0 && scale.endTick > currentTick && (estimatedEndTick == -1 || scale.endTick < estimatedEndTick))
            estimatedEndTick = scale.endTick

        if (estimatedEndTick == -1)
            return currentTick

        return estimatedEndTick
    }

    protected open fun recursiveAnimateHierarchy(part: Display, currentTick: Int, matrixStack: Matrix4fStack) {
        for (entity in part.passengers) {
            if (entity !is Display)
                continue

            if (!idToDisplayMapping.containsValue(entity))
                continue

            val partId = entity.getAttachedOrThrow(AnimatorAttachments.MODEL_PART_ID)

            val localTranslation = entity.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION)
            val localRotation = entity.getAttachedOrCreate(AnimatorAttachments.LOCAL_ROTATION)
            val localScale = entity.getAttachedOrCreate(AnimatorAttachments.LOCAL_SCALE)

            val estimatedEndTick = estimateEndTick(currentTick, localTranslation, localRotation, localScale)
            val remainingDuration = estimatedEndTick - currentTick

            matrixStack.pushMatrix()

            matrixStack.translate(this.initialPositions[partId] ?: Vector3f())
            matrixStack.scale(localScale.get(estimatedEndTick))
            matrixStack.scale(entity.getAttachedOrCreate(AnimatorAttachments.RESCALE))
            matrixStack.rotateZYX(localRotation.get(estimatedEndTick) * Vector3f(1f, -1f, -1f) * Mth.DEG_TO_RAD)
            matrixStack.translate(localTranslation.get(estimatedEndTick))

            /*Gizmos.billboardText("${localRotation.x()} ${localRotation.y()} ${localRotation.z()}", entity.position().add(Vec3(matrixStack.getTranslation(Vector3f()))), TextGizmo.Style.whiteAndCentered().withScale(0.25f))
                .persistForMillis(100)
                .setAlwaysOnTop()*/

            val transformation = Transformation(matrixStack.get(Matrix4f()))

            if (
                transformation.translation() != entity.translation || transformation.leftRotation() != entity.leftRotation
                || transformation.rightRotation() != entity.rightRotation || transformation.scale() != entity.scale
            ) {
                entity.transformationInterpolationDelay = 0
                entity.transformationInterpolationDuration = remainingDuration
                entity.setTransformation(transformation)
            }

            matrixStack.popMatrix()

            matrixStack.pushMatrix()
            //matrixStack.scale(localScale)
            matrixStack.scale(entity.getAttachedOrCreate(AnimatorAttachments.RESCALE))
            matrixStack.rotateZYX(localRotation.get(estimatedEndTick) * Vector3f(1f, -1f, -1f) * Mth.DEG_TO_RAD)
            matrixStack.translate(localTranslation.get(estimatedEndTick))

            this.recursiveAnimateHierarchy(entity, currentTick, matrixStack)

            matrixStack.popMatrix()
        }
    }

    protected open fun recursiveLoadParts(parts: List<ModelDefinition.ModelPart>, parent: Display): MutableMap<String, Display> {
        val displays = mutableMapOf<String, Display>()

        for (part in parts) {
            val display = EntityTypes.ITEM_DISPLAY.create(this.level, EntitySpawnReason.TRIGGERED)
                ?: throw IllegalStateException("Failed to load child display for ID ${part.id}!")
            display.snapTo(parent.position().add(part.origin.x.toDouble(), part.origin.y.toDouble(), part.origin.z.toDouble()))
            display.startRiding(parent, true, false)
            display.translation = part.origin.copy()
            display.scale = Vector3f(part.rescale)
            display.setAttached(AnimatorAttachments.MODEL_PART_ID, part.id)
            display.setAttached(AnimatorAttachments.MODEL_KEY, this.modelKey)
            display.setAttached(AnimatorAttachments.RESCALE, part.rescale)

            val definition = model.definitions[part.id]
            if (definition != null) {
                display.itemStack = ItemStack(definition.item, 1).apply {
                    this.itemModel = definition.model.orElse(null)
                }
            }

            displays[part.id] = display
            displays.putAll(this.recursiveLoadParts(part.children, display))
            display.width = 1f
            display.height = 1f

            this.level.addFreshEntity(display)
        }

        return displays
    }

    protected open fun recursiveLoadExistingParts(parent: Display): MutableMap<String, Display> {
        val displays = mutableMapOf<String, Display>()

        for (entity in parent.passengers) {
            if (entity is Display && entity.hasAttached(AnimatorAttachments.MODEL_KEY) && entity.hasAttached(AnimatorAttachments.MODEL_PART_ID)) {
                if (entity.getAttached(AnimatorAttachments.MODEL_KEY) != modelKey)
                    continue

                val partId = entity.getAttachedOrThrow(AnimatorAttachments.MODEL_PART_ID)
                displays[partId] = entity
                displays.putAll(this.recursiveLoadExistingParts(entity))

                entity.width = 1f
                entity.height = 1f
            }
        }

        return displays
    }
}
