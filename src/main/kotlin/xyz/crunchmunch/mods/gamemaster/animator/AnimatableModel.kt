package xyz.crunchmunch.mods.gamemaster.animator

import com.mojang.math.Transformation
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.animator.animation.Animation
import xyz.crunchmunch.mods.gamemaster.animator.animation.AnimationState
import xyz.crunchmunch.mods.gamemaster.animator.animation.LoopType
import xyz.crunchmunch.mods.gamemaster.animator.animation.MultiAnimationDefinition
import xyz.crunchmunch.mods.gamemaster.animator.util.TransformUtil
import xyz.crunchmunch.mods.gamemaster.utils.*

class AnimatableModel(
    val model: ModelDefinition, val animations: MultiAnimationDefinition,
    val level: ServerLevel
) {
    val modelKey: ResourceKey<ModelDefinition> = this.level.registryAccess().lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getResourceKey(model).orElseThrow()
    val animationsKey: ResourceKey<MultiAnimationDefinition> = this.level.registryAccess().lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getResourceKey(animations).orElseThrow()

    lateinit var rootDisplay: Display
        private set
    lateinit var idToDisplayMapping: Map<String, Display>
        private set
    lateinit var initialPositions: Map<String, Vector3f>
        private set

    var currentState = AnimationState.STOPPED
        private set

    var currentAnimation: Animation? = null
        private set

    var nextAnimation: Animation? = null

    private var currentTick = 0

    private var cachedX = 0.0
    private var cachedY = 0.0
    private var cachedZ = 0.0
    private var cachedYaw = 0f
    private var cachedPitch = 0f

    fun isEntityLoaded(): Boolean {
        return ::rootDisplay.isInitialized && !this.rootDisplay.isRemoved
    }

    fun createNew(pos: Vec3) {
        this.rootDisplay = EntityType.TEXT_DISPLAY.create(this.level, EntitySpawnReason.TRIGGERED)
            ?: throw IllegalStateException("Failed to load root display!")

        this.cachedX = pos.x
        this.cachedY = pos.y
        this.cachedZ = pos.z
        this.cachedYaw = 0f
        this.cachedPitch = 0f

        this.rootDisplay.snapTo(pos)
        this.rootDisplay.setAttached(AnimatorAttachments.MODEL_KEY, this.modelKey)
        this.rootDisplay.setAttached(AnimatorAttachments.ANIMATIONS_KEY, this.animationsKey)

        this.idToDisplayMapping = this.recursiveLoadParts(model.parts, this.rootDisplay).apply {
            this["root"] = this@AnimatableModel.rootDisplay
        }
        this.initialPositions = this.idToDisplayMapping.mapValues { it.value.translation.copy() }

        this.level.addFreshEntity(this.rootDisplay)
    }

    fun loadFromExisting(root: Display) {
        this.rootDisplay = root
        this.cachedX = root.x
        this.cachedY = root.y
        this.cachedZ = root.z
        this.cachedYaw = root.yRot
        this.cachedPitch = root.xRot

        this.idToDisplayMapping = this.recursiveLoadExistingParts(root).apply {
            this["root"] = root
        }
        this.initialPositions = this.idToDisplayMapping.mapValues { it.value.translation.copy() }
    }

    fun queueAnimation(id: String) {
        this.nextAnimation = this.animations.animations[id]
    }

    fun queueAnimation(animation: Animation) {
        this.nextAnimation = animation
    }

    fun stopAnimation() {
        this.currentState = AnimationState.STOPPED
    }

    fun tick() {
        val animation = this.currentAnimation
        var hasChanged = false

        if (animation != null) {
            // Animation playing
            when (this.currentState) {
                AnimationState.PLAYING -> {
                    if (this.currentTick <= animation.maxLength) {
                        val parts = animation.getPartsAfterTick(currentTick)

                        // First pass, calculate the initial animation transforms
                        for ((id, pair) in parts) {
                            val (endTick, posRot) = pair
                            val display = this.idToDisplayMapping[id] ?: continue

                            if (posRot.position != display.getAttached(AnimatorAttachments.LOCAL_TRANSLATION)
                                || posRot.rotation != display.getAttached(AnimatorAttachments.LOCAL_ROTATION)
                                || endTick != display.getAttached(AnimatorAttachments.END_TICK)
                            ) {
                                hasChanged = true
                                display.setAttached(AnimatorAttachments.PREV_LOCAL_TRANSLATION, display.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION))
                                display.setAttached(AnimatorAttachments.PREV_LOCAL_ROTATION, display.getAttachedOrCreate(AnimatorAttachments.LOCAL_ROTATION))
                                display.setAttached(AnimatorAttachments.LOCAL_TRANSLATION, posRot.position)
                                display.setAttached(AnimatorAttachments.LOCAL_ROTATION, posRot.rotation)
                                display.setAttached(AnimatorAttachments.START_TICK, currentTick)
                                display.setAttached(AnimatorAttachments.END_TICK, endTick)
                            }
                        }
                    } else {
                        // Loop back the animation if we can.
                        when (animation.loop) {
                            LoopType.LOOP -> {
                                if (this.nextAnimation == null) {
                                    this.nextAnimation = animation
                                }
                            }

                            LoopType.PLAY_ONCE -> {
                                for ((id, display) in this.idToDisplayMapping) {
                                    display.transformationInterpolationDuration = 0
                                    display.transformationInterpolationDelay = 0
                                    display.translation = this.initialPositions[id] ?: Vector3f()
                                    display.leftRotation = Quaternionf()

                                    display.setAttached(AnimatorAttachments.PREV_LOCAL_TRANSLATION, Vector3f())
                                    display.setAttached(AnimatorAttachments.PREV_LOCAL_ROTATION, Vector3f())
                                    display.setAttached(AnimatorAttachments.LOCAL_TRANSLATION, Vector3f())
                                    display.setAttached(AnimatorAttachments.LOCAL_ROTATION, Vector3f())
                                    display.setAttached(AnimatorAttachments.START_TICK, 0)
                                    display.setAttached(AnimatorAttachments.END_TICK, 0)
                                }
                            }

                            LoopType.HOLD_ON_LAST_FRAME -> {}
                        }

                        this.currentState = AnimationState.TRANSITIONING
                        this.currentTick = 0
                    }
                }

                AnimationState.TRANSITIONING -> {
                    if (this.currentTick++ >= animation.loopDelay) {
                        if (this.nextAnimation != null) {
                            this.currentAnimation = this.nextAnimation
                            this.nextAnimation = null
                            this.currentState = AnimationState.PLAYING
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

            if (this.nextAnimation != null) {
                this.currentAnimation = this.nextAnimation
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

        // Second pass, apply to all things down the chain
        // Only actually handle this if anything did animate.
        if (hasChanged) {
            val translation = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION)
            val rotation = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.LOCAL_ROTATION)
            val startTick = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.START_TICK)
            val endTick = this.rootDisplay.getAttachedOrCreate(AnimatorAttachments.END_TICK)

            val matrixStack = Matrix4fStack(this.idToDisplayMapping.size).apply {
                translate(translation)
                rotateXYZ(rotation.copy()
                    .add(
                        rootDisplay.xRot, rootDisplay.yRot, 0f
                    )
                    .mul(Mth.DEG_TO_RAD))
            }

            val remainingDuration = if (currentState != AnimationState.PLAYING || currentTick > endTick)
                0
            else
                (endTick - startTick) - (currentTick - startTick)

            recursiveAnimateHierarchy(
                this.rootDisplay, currentTick, remainingDuration, startTick,
//                    StackingVector3f(translation), StackingVector3f(rotation)
                matrixStack
            )

            // We have to handle translation and rotation here slightly differently, because we're the root.
            this.rootDisplay.posRotInterpolationDuration = remainingDuration
            this.rootDisplay.transformationInterpolationDelay = 0
            this.rootDisplay.transformationInterpolationDuration = remainingDuration
            this.rootDisplay.translation = translation
            this.rootDisplay.yRot = cachedYaw + rotation.y
            this.rootDisplay.xRot = cachedPitch + rotation.x
        }

        if (this.currentState != AnimationState.STOPPED) {
            this.currentTick++
        }
    }

    fun remove() {
        recursiveRemove(this.rootDisplay)
    }

    private fun recursiveRemove(entity: Entity) {
        for (e in entity.passengers) {
            recursiveRemove(e)
        }

        entity.remove(Entity.RemovalReason.DISCARDED)
    }

    private fun recursiveAnimateHierarchy(part: Display,
                                          currentTick: Int, parentRemainingDuration: Int, parentStartTick: Int,
//                                          addedTranslation: StackingVector3f, addedRotation: StackingVector3f
                                          matrixStack: Matrix4fStack
    ) {
        for (entity in part.passengers) {
            if (entity !is Display)
                continue

            if (!idToDisplayMapping.containsValue(entity))
                continue

            val partId = entity.getAttachedOrThrow(AnimatorAttachments.MODEL_PART_ID)

            var startTick = entity.getAttachedOrCreate(AnimatorAttachments.START_TICK)
            val endTick = entity.getAttachedOrCreate(AnimatorAttachments.END_TICK)
            val animationLength = endTick - startTick
            var remainingDuration = if (currentTick > endTick)
                0
            else
                (endTick - startTick) - (currentTick - startTick)

            var localTranslation = entity.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION)
            var localRotation = entity.getAttachedOrCreate(AnimatorAttachments.LOCAL_ROTATION)

            if (parentRemainingDuration in 1..<remainingDuration && parentStartTick == currentTick) {
                // Handle getting in-between first
                entity.setAttached(AnimatorAttachments.START_TICK, currentTick)
                entity.setAttached(AnimatorAttachments.END_TICK, currentTick + parentRemainingDuration)
                startTick = currentTick

                val previousTranslation = entity.getAttachedOrCreate(AnimatorAttachments.PREV_LOCAL_TRANSLATION)
                val previousRotation = entity.getAttachedOrCreate(AnimatorAttachments.PREV_LOCAL_ROTATION)

                val delta = ((animationLength - remainingDuration).toFloat() / animationLength.toFloat())
                localTranslation = TransformUtil.lerp(delta, previousTranslation, localTranslation)
                localRotation = TransformUtil.lerp(delta, previousRotation, localRotation)

                remainingDuration = parentRemainingDuration
            }

            matrixStack.pushMatrix()
            /*val rotation = addedRotation.peek()

            val matrix = Matrix4f()
            matrix.scale(1f)
            matrix.translate(localTranslation)
            matrix.rotateXYZ(
                rotation.x * Mth.DEG_TO_RAD,
                rotation.y * Mth.DEG_TO_RAD,
                rotation.z * Mth.DEG_TO_RAD,
            )

            val translationOffset = matrix.getTranslation(Vector3f())

            addedTranslation.push(entity.getAttachedOrCreate(AnimatorAttachments.LOCAL_TRANSLATION))
            addedRotation.push(rotation)*/

            matrixStack.translate(this.initialPositions[partId] ?: Vector3f())
            matrixStack.translate(localTranslation)
            matrixStack.rotateXYZ(localRotation.copy().mul(1f, 1f, -1f).mul(Mth.DEG_TO_RAD))

            if (startTick == currentTick) {
                val transformation = Transformation(matrixStack.get(Matrix4f()))

                if (
                    transformation.translation != entity.translation || transformation.leftRotation != entity.leftRotation
                    || transformation.rightRotation != entity.rightRotation || transformation.scale != entity.scale
                ) {
                    entity.transformationInterpolationDelay = 0
                    entity.transformationInterpolationDuration = remainingDuration
                    entity.setTransformation(transformation)
                }
            }

            matrixStack.popMatrix()

            matrixStack.pushMatrix()
            matrixStack.translate(localTranslation)
            matrixStack.rotateXYZ(localRotation.copy().mul(1f, 1f, -1f).mul(Mth.DEG_TO_RAD))

            this.recursiveAnimateHierarchy(entity, currentTick, remainingDuration, startTick, matrixStack)

            matrixStack.popMatrix()
//            addedTranslation.pop()
//            addedRotation.pop()
        }
    }

    private fun recursiveLoadParts(parts: List<ModelDefinition.ModelPart>, parent: Display): MutableMap<String, Display> {
        val displays = mutableMapOf<String, Display>()

        for (part in parts) {
            val display = EntityType.ITEM_DISPLAY.create(this.level, EntitySpawnReason.TRIGGERED)
                ?: throw IllegalStateException("Failed to load child display for ID ${part.id}!")
            display.snapTo(parent.position().add(part.origin.x.toDouble(), part.origin.y.toDouble(), part.origin.z.toDouble()))
            display.startRiding(parent, true, false)
            display.translation = part.origin.copy()
            display.setAttached(AnimatorAttachments.MODEL_PART_ID, part.id)
            display.setAttached(AnimatorAttachments.MODEL_KEY, this.modelKey)

            val definition = model.definitions[part.id]
            if (definition != null) {
                display.itemStack = ItemStack(definition.item, 1).apply {
                    this.itemModel = definition.model.orElse(null)
                }
            }

            displays[part.id] = display
            displays.putAll(this.recursiveLoadParts(part.children, display))

            this.level.addFreshEntity(display)
        }

        return displays
    }

    private fun recursiveLoadExistingParts(parent: Display): MutableMap<String, Display> {
        val displays = mutableMapOf<String, Display>()

        for (entity in parent.passengers) {
            if (entity is Display && entity.hasAttached(AnimatorAttachments.MODEL_KEY) && entity.hasAttached(AnimatorAttachments.MODEL_PART_ID)) {
                if (entity.getAttached(AnimatorAttachments.MODEL_KEY) != modelKey)
                    continue

                val partId = entity.getAttachedOrThrow(AnimatorAttachments.MODEL_PART_ID)
                displays[partId] = entity
                displays.putAll(this.recursiveLoadExistingParts(entity))
            }
        }

        return displays
    }
}
