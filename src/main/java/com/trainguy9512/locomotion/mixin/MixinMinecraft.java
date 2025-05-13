package com.trainguy9512.locomotion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.entity.FirstPersonPlayerJointAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {


    @Shadow @Nullable public ClientLevel level;

    @Shadow private volatile boolean pause;

    @Shadow protected abstract boolean isLevelRunningNormally();

    @Shadow public abstract CompletableFuture<Void> delayTextureReload();

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Inject(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V")
    )
    public void playItemDropAnimation(CallbackInfo ci) {
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_DROPPED_ITEM).trigger();
        });
    }

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"))
    public void injectStartAttackHitEntity(CallbackInfoReturnable<Boolean> cir) {
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_ATTACKED).trigger();
        });
    }

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    public void injectStartAttackMiss(CallbackInfoReturnable<Boolean> cir) {
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_ATTACKED).trigger();
        });
    }

    @Inject(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V")
    )
    public void injectOnSwingPlayerHandWhenBeginningUse(CallbackInfo ci, @Local InteractionHand interactionHand) {
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            switch (interactionHand) {
                case MAIN_HAND -> dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_USED_MAIN_HAND_ITEM).trigger();
                case OFF_HAND -> dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_USED_OFF_HAND_ITEM).trigger();
            }
        });
    }

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;animateTick(III)V")
    )
    private void locomotionTick(CallbackInfo ci) {
        assert this.level != null;
        JointAnimatorDispatcher jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();
        jointAnimatorDispatcher.tickEntityJointAnimators(this.level.entitiesForRendering());
        jointAnimatorDispatcher.tickFirstPersonPlayerJointAnimator();
    }

    /**
     * Play the block cracking particles only if the mining animation has entered its impact state.
     * Play the cracking particles as normal if the first person renderer config is disabled.
     */
//    TODO: If this is going to be re-implemented, NeoForge needs an alternative implementation.
//    @Redirect(
//            method = "continueAttack",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"))
//    public void onlySpawnBreakParticlesOnPickaxeImpact(ParticleEngine instance, BlockPos pos, Direction side) {
//        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(driverContainer -> {
//            if (driverContainer.getDriverValue(FirstPersonPlayerJointAnimator.IS_MINING_IMPACTING) || !LocomotionMain.CONFIG.data().firstPersonPlayer.enableRenderer) {
//                for (float i = 0; i < 8; i++) {
//                    instance.crack(pos, side);
//                }
//            }
//        });
//    }
}
