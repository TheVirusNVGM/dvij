package com.trainguy9512.locomotion.mixin;

import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.entity.FirstPersonPlayerJointAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Shadow @Final private Minecraft minecraft;

    @Inject(
            method = "stopDestroyBlock",
            at = @At("HEAD")
    )
    public void disableMiningAnimationOnNoLongerMining(CallbackInfo ci) {
        assert this.minecraft.player != null;
        if (!this.minecraft.player.getAbilities().instabuild) {
            JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
//                if (dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).getCurrentValue() && !dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).getPreviousValue()) {
//                    dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_ATTACKED).trigger();
//                }
                if (dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).getPreviousValue()) {
                    dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).setValue(false);
                }
            });
        }
    }

    @Inject(
            method = "startDestroyBlock",
            at = @At("HEAD")
    )
    public void enableMiningAnimationOnBeginMining(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        assert this.minecraft.player != null;
        if (!this.minecraft.player.getAbilities().instabuild) {
            JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).setValue(true));
        }
    }

    @Inject(
            method = "destroyBlock",
            at = @At("RETURN")
    )
    public void destroyBlockInCreativeInstantly(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        assert this.minecraft.player != null;
        if (cir.getReturnValue() && this.minecraft.player.getAbilities().instabuild) {
            JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> dataContainer.getDriver(FirstPersonPlayerJointAnimator.HAS_ATTACKED).trigger());
        }
    }
}
