package com.example.createftbguard;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuardEventHandler {

    private final Map<UUID, Vec3> lastSafePositions = new ConcurrentHashMap<>();
    private final Map<UUID, BlockPos> activeContainerPositions = new ConcurrentHashMap<>();
    private final Map<UUID, AirshipSession> recentAirshipUsers = new ConcurrentHashMap<>();

    private static class AirshipSession {
        final UUID playerId;
        final long timestamp;
        AirshipSession(UUID playerId, long timestamp) {
            this.playerId = playerId;
            this.timestamp = timestamp;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!GuardConfig.ENABLE_MOD.get() || !(event.getLevel() instanceof ServerLevel level)) return;

        BlockState state = event.getPlacedBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        if (GuardConfig.BLOCKED_CREATE_BLOCKS.get().contains(id.toString())) {
            Entity entity = event.getEntity();
            UUID ownerId = entity != null ? entity.getUUID() : null;

            if (!FtbClaimHelper.canInteract(level, event.getPos(), ownerId)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!GuardConfig.ENABLE_MOD.get() || !(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos sourcePos = event.getPos();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());

        if (GuardConfig.BLOCKED_CREATE_BLOCKS.get().contains(blockId.toString())) {
            for (var dir : event.getNotifiedSides()) {
                BlockPos targetPos = sourcePos.relative(dir);
                if (!FtbClaimHelper.isSameClaimOwner(level, sourcePos, targetPos)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!GuardConfig.ENABLE_MOD.get()) return;
        Entity target = event.getTarget();
        if (isAirshipEntity(target)) {
            recentAirshipUsers.put(target.getUUID(), new AirshipSession(event.getEntity().getUUID(), event.getLevel().getGameTime()));
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!GuardConfig.ENABLE_MOD.get()) return;
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        boolean isContraption = entity instanceof AbstractContraptionEntity;
        boolean isTrain = entity instanceof CarriageContraptionEntity;
        boolean isAirship = isAirshipEntity(entity);

        if (!isContraption && !isTrain && !isAirship) return;

        if (isTrain) {
            if (GuardConfig.ALLOW_TRAINS_IN_CLAIMS.get()) {
                return;
            }
        }

        BlockPos currentBlockPos = entity.blockPosition();
        UUID ownerId = resolveOwner(entity);

        boolean allowed = true;

        if (isAirship && GuardConfig.REQUIRE_AIRSHIP_BOARDING.get()) {
            boolean hasBoarded = false;
            if (ownerId != null && FtbClaimHelper.canInteract(serverLevel, currentBlockPos, ownerId)) {
                hasBoarded = true;
            } else {
                for (Entity passenger : entity.getPassengers()) {
                    if (passenger instanceof Player player) {
                        if (FtbClaimHelper.canInteract(serverLevel, currentBlockPos, player.getUUID())) {
                            hasBoarded = true;
                            break;
                        }
                    }
                }
                AirshipSession session = recentAirshipUsers.get(entity.getUUID());
                if (session != null && (serverLevel.getGameTime() - session.timestamp) < 200) {
                    if (FtbClaimHelper.canInteract(serverLevel, currentBlockPos, session.playerId)) {
                        hasBoarded = true;
                    }
                }
            }

            if (!hasBoarded && !FtbClaimHelper.canInteract(serverLevel, currentBlockPos, ownerId)) {
                allowed = false;
            }
        } else {
            allowed = FtbClaimHelper.canInteract(serverLevel, currentBlockPos, ownerId);
            if (allowed && isContraption) {
                int radius = 4;
                for (int dx = -radius; dx <= radius; dx += radius) {
                    for (int dz = -radius; dz <= radius; dz += radius) {
                        if (!FtbClaimHelper.canInteract(serverLevel, currentBlockPos.offset(dx, 0, dz), ownerId)) {
                            allowed = false;
                            break;
                        }
                    }
                }
            }
        }

        if (allowed) {
            lastSafePositions.put(entity.getUUID(), entity.position());
        } else {
            Vec3 safePos = lastSafePositions.get(entity.getUUID());
            entity.setDeltaMovement(Vec3.ZERO);
            if (safePos != null) {
                entity.teleportTo(safePos.x, safePos.y, safePos.z);
            } else if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                contraptionEntity.disassemble();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!GuardConfig.ENABLE_ANTI_DUPE.get() || event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();

        if (entity instanceof AbstractContraptionEntity || isAirshipEntity(entity)) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                Entity existing = serverLevel.getEntity(entity.getUUID());
                if (existing != null && existing != entity) {
                    CreateFtbGuardMod.LOGGER.warn("[Anti-Dupe] Prevented duplicate contraption/airship UUID: {}", entity.getUUID());
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!GuardConfig.ENABLE_ANTI_DUPE.get() || event.getLevel().isClientSide()) return;
        Player player = event.getEntity();

        if (!FtbClaimHelper.canInteract(event.getLevel(), event.getPos(), player.getUUID())) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
            if (blockId.getNamespace().equals("create")) {
                event.setCanceled(true);
                return;
            }
        }

        activeContainerPositions.put(player.getUUID(), event.getPos());
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        activeContainerPositions.remove(event.getEntity().getUUID());
    }

    private boolean isAirshipEntity(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String fullId = id.toString();
        for (String prefix : GuardConfig.AIRSHIP_ENTITY_IDS.get()) {
            if (fullId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private UUID resolveOwner(Entity entity) {
        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return ownable.getOwnerUUID();
        }
        if (entity.getControllingPassenger() instanceof Player player) {
            return player.getUUID();
        }
        if (!entity.getPassengers().isEmpty() && entity.getPassengers().getFirst() instanceof Player player) {
            return player.getUUID();
        }
        return null;
    }
}
