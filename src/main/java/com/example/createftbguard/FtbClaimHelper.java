package com.example.createftbguard;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class FtbClaimHelper {

    public static boolean canInteract(Level level, BlockPos targetPos, @Nullable UUID ownerUuid) {
        if (!GuardConfig.ENABLE_MOD.get() || !(level instanceof ServerLevel serverLevel)) {
            return true;
        }

        try {
            var manager = FTBChunksAPI.api().getManager();
            if (manager == null) return true;

            ChunkDimPos chunkPos = new ChunkDimPos(serverLevel, targetPos);
            ClaimedChunk claim = manager.getChunk(chunkPos);

            if (claim == null) {
                return GuardConfig.ALLOW_WILDERNESS_FROM_CLAIM.get();
            }

            if (ownerUuid == null) {
                return false;
            }

            Team chunkTeam = claim.getTeamData().getTeam();

            if (chunkTeam.getRankForPlayer(ownerUuid).isMemberOrBetter()) {
                return true;
            }

            if (GuardConfig.ALLOW_ALLIES.get()) {
                Optional<Team> ownerTeamOpt = FTBTeamsAPI.api().getManager().getTeamForPlayerID(ownerUuid);
                if (ownerTeamOpt.isPresent() && chunkTeam.isAllied(ownerTeamOpt.get())) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public static boolean isSameClaimOwner(Level level, BlockPos sourcePos, BlockPos targetPos) {
        if (!GuardConfig.ENABLE_MOD.get() || !(level instanceof ServerLevel serverLevel)) {
            return true;
        }

        try {
            var manager = FTBChunksAPI.api().getManager();
            if (manager == null) return true;

            ClaimedChunk sourceClaim = manager.getChunk(new ChunkDimPos(serverLevel, sourcePos));
            ClaimedChunk targetClaim = manager.getChunk(new ChunkDimPos(serverLevel, targetPos));

            if (targetClaim == null) {
                return GuardConfig.ALLOW_WILDERNESS_FROM_CLAIM.get();
            }

            if (sourceClaim == null) {
                return false;
            }

            Team sourceTeam = sourceClaim.getTeamData().getTeam();
            Team targetTeam = targetClaim.getTeamData().getTeam();

            if (sourceTeam.getId().equals(targetTeam.getId())) {
                return true;
            }

            return GuardConfig.ALLOW_ALLIES.get() && targetTeam.isAllied(sourceTeam);
        } catch (Exception ignored) {}

        return true;
    }
}
