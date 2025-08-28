package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.*;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(DebugPackets.class)
public abstract class DebugPacketsMixin {
    @Shadow
    private static void sendPacketToAllPlayers(ServerLevel level, CustomPacketPayload payload) {
    }

    @Shadow
    private static List<String> getMemoryDescriptions(LivingEntity entity, long gameTime) {
        return null;
    }

    @Inject(method = "sendPoiPacketsForChunk", at = @At("HEAD"))
    private static void devtools$sendPoiPackets(ServerLevel level, ChunkPos chunkPos, CallbackInfo ci) {
        sendPacketToAllPlayers(level, new WorldGenAttemptDebugPayload(chunkPos.getWorldPosition().above(100), 1f, 1f, 1f, 1f, 1f));
    }

    @Inject(method = "sendPoiAddedPacket", at = @At("HEAD"))
    private static void devtools$sendPoiAddition(ServerLevel world, BlockPos pos, CallbackInfo ci) {
        world.getPoiManager().getType(pos).ifPresent(registryEntry -> {
            int tickets = world.getPoiManager().getFreeTickets(pos);
            String name = registryEntry.getRegisteredName();
            sendPacketToAllPlayers(world, new PoiAddedDebugPayload(pos, name, tickets));
        });
    }

    @Inject(
        method = {"sendPoiRemovedPacket"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendPoiRemoval(ServerLevel world, BlockPos pos, CallbackInfo ci) {
        sendPacketToAllPlayers(world, new PoiRemovedDebugPayload(pos));
    }

    @Inject(
        method = {"sendPoiTicketCountPacket"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendPointOfInterest(ServerLevel world, BlockPos pos, CallbackInfo ci) {
        int tickets = world.getPoiManager().getFreeTickets(pos);
        sendPacketToAllPlayers(world, new PoiTicketCountDebugPayload(pos, tickets));
    }

    @Inject(
        method = {"sendVillageSectionsPacket"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendPoi(ServerLevel world, BlockPos pos, CallbackInfo ci) {
        Registry<Structure> registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        SectionPos chunkSectionPos = SectionPos.of(pos);

        for (Holder<Structure> entry : registry.getTagOrEmpty(StructureTags.VILLAGE)) {
            if (!world.structureManager().startsForStructure(chunkSectionPos, entry.value()).isEmpty()) {
                sendPacketToAllPlayers(world, new VillageSectionsDebugPayload(Set.of(chunkSectionPos), Set.of()));
                return;
            }
        }

        sendPacketToAllPlayers(world, new VillageSectionsDebugPayload(Set.of(), Set.of(chunkSectionPos)));
    }

    @Inject(
        method = {"sendPathFindingPacket"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendPathfindingData(Level world, Mob mob, @Nullable Path path, float nodeReachProximity, CallbackInfo ci) {
        if (path != null) {
            sendPacketToAllPlayers((ServerLevel)world, new PathfindingDebugPayload(mob.getId(), path, nodeReachProximity));
        }
    }

    @Inject(
        method = {"sendNeighborsUpdatePacket"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendNeighborUpdate(Level world, BlockPos pos, CallbackInfo ci) {
        if (!world.isClientSide) {
            sendPacketToAllPlayers((ServerLevel)world, new NeighborUpdatesDebugPayload(world.getGameTime(), pos));
        }
    }

    @Inject(
        method = {"sendStructurePacket"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendStructureStart(WorldGenLevel world, StructureStart structureStart, CallbackInfo ci) {
        List<StructuresDebugPayload.PieceInfo> pieces = new ArrayList<>();

        for (int i = 0; i < structureStart.getPieces().size(); i++) {
            pieces.add(new StructuresDebugPayload.PieceInfo(structureStart.getPieces().get(i).getBoundingBox(), i == 0));
        }

        ServerLevel serverWorld = world.getLevel();
        sendPacketToAllPlayers(serverWorld, new StructuresDebugPayload(serverWorld.dimension(), structureStart.getBoundingBox(), pieces));
    }

    @Inject(
        method = {"sendGoalSelector"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendGoalSelector(Level world, Mob mob, GoalSelector goalSelector, CallbackInfo ci) {
        List<GoalDebugPayload.DebugGoal> goals = ((MobAccessor) mob)
            .getGoalSelector()
            .getAvailableGoals()
            .stream()
            .map(goal -> new GoalDebugPayload.DebugGoal(goal.getPriority(), goal.isRunning(), goal.getGoal().toString()))
            .toList();
        sendPacketToAllPlayers((ServerLevel)world, new GoalDebugPayload(mob.getId(), mob.blockPosition(), goals));
    }

    @Inject(
        method = {"sendRaids"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendRaids(ServerLevel world, Collection<Raid> raids, CallbackInfo ci) {
        sendPacketToAllPlayers(world, new RaidsDebugPayload(raids.stream().map(Raid::getCenter).toList()));
    }

    @Inject(
        method = {"sendEntityBrain"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendBrainDebugData(LivingEntity livingEntity, CallbackInfo ci) {
        if (!(livingEntity instanceof Mob entity))
            return;

        ServerLevel serverWorld = (ServerLevel) entity.level();
        int angerLevel = entity instanceof Warden wardenEntity ? wardenEntity.getClientAngerLevel() : -1;
        List<String> gossips = new ArrayList<>();
        Set<BlockPos> pois = new HashSet<>();
        Set<BlockPos> potentialPois = new HashSet<>();
        String profession;
        int xp;
        String inventory;
        boolean wantsGolem;
        if (entity instanceof Villager villager) {
            profession = villager.getVillagerData().profession().unwrapKey().orElseThrow().location().toString();
            xp = villager.getVillagerXp();
            inventory = villager.getInventory().toString();
            wantsGolem = villager.wantsToSpawnGolem(serverWorld.getGameTime());
            villager.getGossips().getGossipEntries().forEach((uuid, associatedGossip) -> {
                Entity gossipEntity = serverWorld.getEntity(uuid);
                if (gossipEntity != null) {
                    String name = DebugEntityNameGenerator.getEntityName(gossipEntity);

                    for (Object2IntMap.Entry<GossipType> entry : associatedGossip.object2IntEntrySet()) {
                        gossips.add(name + ": " + entry.getKey().getSerializedName() + " " + entry.getValue());
                    }
                }
            });
            Brain<?> brain = villager.getBrain();
            devtools$addPoi(brain, MemoryModuleType.HOME, pois);
            devtools$addPoi(brain, MemoryModuleType.JOB_SITE, pois);
            devtools$addPoi(brain, MemoryModuleType.MEETING_POINT, pois);
            devtools$addPoi(brain, MemoryModuleType.HIDING_PLACE, pois);
            devtools$addPoi(brain, MemoryModuleType.POTENTIAL_JOB_SITE, potentialPois);
        } else {
            profession = "";
            xp = 0;
            inventory = "";
            wantsGolem = false;
        }

        sendPacketToAllPlayers(
            serverWorld,
            new BrainDebugPayload(
                new BrainDebugPayload.BrainDump(
                    entity.getUUID(),
                    entity.getId(),
                    entity.getName().getString(),
                    profession,
                    xp,
                    entity.getHealth(),
                    entity.getMaxHealth(),
                    entity.position(),
                    inventory,
                    entity.getNavigation().getPath(),
                    wantsGolem,
                    angerLevel,
                    entity.getBrain().getActiveActivities().stream().map(Activity::toString).toList(),
                    entity.getBrain().getRunningBehaviors().stream().map(BehaviorControl::debugString).toList(),
                    getMemoryDescriptions(entity, serverWorld.getGameTime()),
                    gossips,
                    pois,
                    potentialPois
                )
            )
        );
    }

    @Unique
    private static void devtools$addPoi(Brain<?> brain, MemoryModuleType<GlobalPos> memoryModuleType, Set<BlockPos> set) {
        brain.getMemory(memoryModuleType).map(GlobalPos::pos).ifPresent(set::add);
    }

    @Inject(
        method = {"sendBeeInfo"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendBeeDebugData(Bee bee, CallbackInfo ci) {
        sendPacketToAllPlayers(
            (ServerLevel)bee.level(),
            new BeeDebugPayload(
                new BeeDebugPayload.BeeInfo(
                    bee.getUUID(),
                    bee.getId(),
                    bee.position(),
                    bee.getNavigation().getPath(),
                    bee.getHivePos(),
                    bee.getSavedFlowerPos(),
                    bee.getTravellingTicks(),
                    bee.getGoalSelector().getAvailableGoals().stream().map(prioritizedGoal -> prioritizedGoal.getGoal().toString()).collect(Collectors.toSet()),
                    bee.getBlacklistedHives()
                )
            )
        );
    }

    @Inject(
        method = {"sendBreezeInfo"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendBreezeDebugData(Breeze breeze, CallbackInfo ci) {
        sendPacketToAllPlayers(
            (ServerLevel)breeze.level(),
            new BreezeDebugPayload(
                new BreezeDebugPayload.BreezeInfo(
                    breeze.getUUID(),
                    breeze.getId(),
                    breeze.getTarget() == null ? null : breeze.getTarget().getId(),
                    (BlockPos)breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET).orElse(null)
                )
            )
        );
    }

    @Inject(
        method = {"sendGameEventInfo"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendGameEvent(Level world, Holder<GameEvent> event, Vec3 pos, CallbackInfo ci) {
        if (world instanceof ServerLevel serverWorld) {
            event.unwrapKey().ifPresent(key -> sendPacketToAllPlayers(serverWorld, new GameEventDebugPayload(key, pos)));
        }
    }

    @Inject(
        method = {"sendGameEventListenerInfo"},
        at = {@At("HEAD")}
    )
    private static void devtools$sendGameEventListener(Level world, GameEventListener eventListener, CallbackInfo ci) {
        if (world instanceof ServerLevel serverWorld) {
            sendPacketToAllPlayers(serverWorld, new GameEventListenerDebugPayload(eventListener.getListenerSource(), eventListener.getListenerRadius()));
        }
    }
}
