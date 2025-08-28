package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.mods.gamemaster.client.GameMasterClient;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Shadow @Final public PathfindingRenderer pathfindingRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer waterDebugRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer heightMapRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer collisionBoxRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer supportBlockRenderer;

    @Shadow @Final public NeighborsUpdateRenderer neighborsUpdateRenderer;

    @Shadow @Final public StructureRenderer structureRenderer;

    @Shadow @Final public LightSectionDebugRenderer skyLightSectionDebugRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer worldGenAttemptRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer solidFaceRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer chunkRenderer;

    @Shadow @Final public BrainDebugRenderer brainDebugRenderer;

    @Shadow @Final public VillageSectionsDebugRenderer villageSectionsDebugRenderer;

    @Shadow @Final public BeeDebugRenderer beeDebugRenderer;

    @Shadow @Final public RaidDebugRenderer raidDebugRenderer;

    @Shadow @Final public GoalSelectorDebugRenderer goalSelectorRenderer;

    @Shadow @Final public GameEventListenerRenderer gameEventListenerRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer lightDebugRenderer;

    @Shadow @Final public BreezeDebugRenderer breezeDebugRenderer;

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer chunkBorderRenderer;

    @Shadow @Final public RedstoneWireOrientationsRenderer redstoneWireOrientationsRenderer;

    @Shadow @Final public GameTestDebugRenderer gameTestDebugRenderer;

    @Shadow @Final public ChunkCullingDebugRenderer chunkCullingDebugRenderer;

    @Shadow @Final public OctreeDebugRenderer octreeDebugRenderer;

    @Inject(method = "render", at = @At("TAIL"))
    private void devtools$render(PoseStack poseStack, Frustum frustum, MultiBufferSource.BufferSource bufferSource, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        GameMasterClient.Companion.getRenderers().values().forEach(rendererEntry -> rendererEntry.render(poseStack, bufferSource, cameraX, cameraY, cameraZ));
    }

    @Inject(
        method = {"<init>"},
        at = {@At("TAIL")}
    )
    private void devtools$init(Minecraft client, CallbackInfo ci) {
        GameMasterClient.Companion.addRenderer("Pathfinding", this.pathfindingRenderer::render);
        GameMasterClient.Companion.addRenderer("Water", this.waterDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Chunk Border", this.chunkBorderRenderer::render);
        GameMasterClient.Companion.addRenderer("Heightmap", this.heightMapRenderer::render);
        GameMasterClient.Companion.addRenderer("Collision", this.collisionBoxRenderer::render);
        GameMasterClient.Companion.addRenderer("Supporting Block", this.supportBlockRenderer::render);
        GameMasterClient.Companion.addRenderer("Neighbor Update", this.neighborsUpdateRenderer::render);
        GameMasterClient.Companion.addRenderer("Redstone Wire Orientation", this.redstoneWireOrientationsRenderer::render);
        GameMasterClient.Companion.addRenderer("Structure", this.structureRenderer::render);
        GameMasterClient.Companion.addRenderer("Light", this.lightDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("World Gen Attempt", this.worldGenAttemptRenderer::render);
        GameMasterClient.Companion.addRenderer("Block Outline", this.solidFaceRenderer::render);
        GameMasterClient.Companion.addRenderer("Chunk Loading", this.chunkRenderer::render);
        GameMasterClient.Companion.addRenderer("Brain / POI / Village", this.brainDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Village Sections", this.villageSectionsDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Bee", this.beeDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Raid Center", this.raidDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Goal", this.goalSelectorRenderer::render);
        GameMasterClient.Companion.addRenderer("Game Test", this.gameTestDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Game Event", this.gameEventListenerRenderer::render);
        GameMasterClient.Companion.addRenderer("Sky Light", this.skyLightSectionDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Breeze", this.breezeDebugRenderer::render);
        GameMasterClient.Companion.addRenderer("Chunk Culling", this.chunkCullingDebugRenderer::render);
        //GameMasterClient.Companion.addRenderer("Octree", this.octreeDebugRenderer::render);
    }
}
