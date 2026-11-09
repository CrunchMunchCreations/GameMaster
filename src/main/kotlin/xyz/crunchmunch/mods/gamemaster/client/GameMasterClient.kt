package xyz.crunchmunch.mods.gamemaster.client

import de.phyrone.brig.wrapper.executesNoResult
import de.phyrone.brig.wrapper.literal
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents
import net.minecraft.gizmos.Gizmos
import net.minecraft.gizmos.TextGizmo
import net.minecraft.util.Mth
import net.minecraft.world.entity.Display
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.utils.leftRotation
import xyz.crunchmunch.mods.gamemaster.utils.rightRotation
import xyz.crunchmunch.mods.gamemaster.utils.scale
import xyz.crunchmunch.mods.gamemaster.utils.translation
import java.awt.Color
import java.text.NumberFormat

class GameMasterClient : ClientModInitializer {
    private var showDisplayOrigins = false

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            dispatcher.literal("gamemasterclient") {
                literal("debug") {
                    literal("toggle_display_origins") {
                        executesNoResult {
                            showDisplayOrigins = !showDisplayOrigins
                        }
                    }
                }
            }
        }

        LevelExtractionEvents.END_EXTRACTION.register { context ->
            if (!showDisplayOrigins) return@register

            for (entity in context.level().entitiesForRendering()) {
                entity.passengersAndSelf.forEach {
                    if (it is Display) {
                        Gizmos.point(it.position(), Color.BLUE.rgb, 6f)
                            .setAlwaysOnTop()

                        Gizmos.point(it.position().add(Vec3(it.translation)), Color.YELLOW.rgb, 6f)
                            .setAlwaysOnTop()

                        val style = TextGizmo.Style.whiteAndCentered().withScale(0.1f)
                        val format = NumberFormat.getInstance()
                        Gizmos.billboardText("t: ${it.translation.toString(format)}",
                            it.position()
                                .add(Vec3(it.translation))
                                .subtract(0.0, 0.075, 0.0), style).setAlwaysOnTop()

                        Gizmos.billboardText("lr: ${it.leftRotation.getEulerAnglesXYZ(Vector3f()).mul(Mth.RAD_TO_DEG).toString(format)}",
                            it.position()
                                .add(Vec3(it.translation))
                                .subtract(0.0, 0.075*2, 0.0), style).setAlwaysOnTop()

                        Gizmos.billboardText("rr: ${it.rightRotation.getEulerAnglesXYZ(Vector3f()).mul(Mth.RAD_TO_DEG).toString(format)}",
                            it.position()
                                .add(Vec3(it.translation))
                                .subtract(0.0, 0.075*3, 0.0), style).setAlwaysOnTop()

                        Gizmos.billboardText("s: ${it.scale.toString(format)}",
                            it.position()
                                .add(Vec3(it.translation))
                                .subtract(0.0, 0.075*4, 0.0), style).setAlwaysOnTop()
                    }
                }
            }
        }
    }
}
