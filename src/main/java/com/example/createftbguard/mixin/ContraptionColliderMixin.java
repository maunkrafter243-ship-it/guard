package com.example.createftbguard.mixin;

import com.example.createftbguard.FtbClaimHelper;
import com.example.createftbguard.GuardConfig;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = ContraptionCollider.class, remap = false)
public class ContraptionColliderMixin {

    @Inject(method = "collideBlocks", at = @At("HEAD"), cancellable = true)
    private static void onCollideBlocks(AbstractContraptionEntity contraptionEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!GuardConfig.ENABLE_MOD.get()) return;
        Level level = contraptionEntity.level();
        if (level.isClientSide()) return;

        UUID owner = contraptionEntity.getControllingPassenger() instanceof Player p ? p.getUUID() : null;
        BlockPos pos = contraptionEntity.blockPosition();

        if (!FtbClaimHelper.canInteract(level, pos, owner)) {
            contraptionEntity.disassemble();
            cir.setReturnValue(true);
        }
    }
}
