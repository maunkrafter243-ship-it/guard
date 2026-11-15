package com.example.createftbguard.mixin;

import com.example.createftbguard.FtbClaimHelper;
import com.example.createftbguard.GuardConfig;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public class CarriageContraptionMixin {

    @Inject(method = "tickContraption", at = @At("HEAD"), cancellable = true)
    private void onTrainTick(CallbackInfo ci) {
        if (!GuardConfig.ENABLE_MOD.get() || !GuardConfig.BLOCK_TRAINS_IN_CLAIMS.get()) return;

        CarriageContraptionEntity self = (CarriageContraptionEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        UUID driverId = self.getControllingPassenger() instanceof Player p ? p.getUUID() : null;

        if (!FtbClaimHelper.canInteract(level, self.blockPosition(), driverId)) {
            self.setDeltaMovement(0, 0, 0);
            ci.cancel();
        }
    }
}
