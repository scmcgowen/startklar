package com.herrkatze.startklar.mixin;

import com.herrkatze.startklar.Startklar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.herrkatze.startklar.Startklar.FLYING_SINCE_SPAWN_TYPE;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * After landing, ensure the player cannot fly again outside the spawn.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void resetFallDistance() {
        boolean value = super.onGround();
        int isFlyingSinceSpawn = this.getAttachedOrElse(FLYING_SINCE_SPAWN_TYPE, 0);

        if ((isFlyingSinceSpawn > 0) && value) {
            this.removeAttached(FLYING_SINCE_SPAWN_TYPE);
            Startklar.LOGGER.debug("Removed FLYING_SINCE_SPAWN tag for {}", this.getUUID());
        }


        super.resetFallDistance();
    }

    /**
     * Cancels the code responsible for damaging the Elytra, as the server would otherwise crash, since the player
     * likely will not wear any Elytra's.
     */
    @Inject(
            method = "updateFallFlying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void startklar$skipElytraDurability(CallbackInfo ci) {
        if (isEntityFlying()) {
            ci.cancel();
            this.gameEvent(GameEvent.ELYTRA_GLIDE); // Since we skip this by canceling here
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Unique
    private boolean isEntityFlying() {
        int isFlyingSinceSpawn = this.getAttachedOrElse(FLYING_SINCE_SPAWN_TYPE, 0);
        return isFlyingSinceSpawn > 0;
    }
}