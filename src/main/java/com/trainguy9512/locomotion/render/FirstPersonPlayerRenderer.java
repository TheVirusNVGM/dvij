package com.trainguy9512.locomotion.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.access.MatrixModelPart;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.entity.FirstPersonPlayerJointAnimator;
import com.trainguy9512.locomotion.animation.driver.DriverKey;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class FirstPersonPlayerRenderer implements RenderLayerParent<PlayerRenderState, PlayerModel> {

    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blockRenderer;
    private final ItemModelResolver itemModelResolver;
    private final JointAnimatorDispatcher jointAnimatorDispatcher;

    public FirstPersonPlayerRenderer(EntityRendererProvider.Context context) {
        this.minecraft = Minecraft.getInstance();
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.itemRenderer = minecraft.getItemRenderer();
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.itemModelResolver = context.getItemModelResolver();
        this.jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();
    }

    public void render(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer, LocalPlayer playerEntity, int combinedLight) {

        JointAnimatorDispatcher jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();

        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(
                dataContainer -> jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().ifPresent(
                        animationPose -> {

                            JointChannel rightArmPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.RIGHT_ARM_JOINT);
                            JointChannel leftArmPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.LEFT_ARM_JOINT);
                            JointChannel rightItemPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.RIGHT_ITEM_JOINT);
                            JointChannel leftItemPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.LEFT_ITEM_JOINT);

                            poseStack.pushPose();
                            poseStack.mulPose(Axis.ZP.rotationDegrees(180));


                            AbstractClientPlayer abstractClientPlayer = this.minecraft.player;
                            PlayerRenderer playerRenderer = (PlayerRenderer)this.entityRenderDispatcher.getRenderer(abstractClientPlayer);
                            PlayerModel playerModel = playerRenderer.getModel();
                            playerModel.resetPose();

                            ((MatrixModelPart)(Object) playerModel.rightArm).locomotion$setMatrix(rightArmPose.getTransform());
                            ((MatrixModelPart)(Object) playerModel.leftArm).locomotion$setMatrix(leftArmPose.getTransform());

                            playerModel.body.visible = false;

                            this.renderArm(abstractClientPlayer, playerModel, HumanoidArm.LEFT, poseStack, buffer, combinedLight);
                            this.renderArm(abstractClientPlayer, playerModel, HumanoidArm.RIGHT, poseStack, buffer, combinedLight);

                            //this.entityRenderDispatcher.render(abstractClientPlayer, 0, 0, 0, partialTicks, poseStack, buffer, combinedLight);

                            boolean leftHanded = this.minecraft.options.mainHand().get() == HumanoidArm.LEFT;

                            ItemStack leftHandRenderedItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.RENDERED_MAIN_HAND_ITEM : FirstPersonPlayerJointAnimator.RENDERED_OFF_HAND_ITEM);
                            ItemStack rightHandRenderedItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.RENDERED_OFF_HAND_ITEM : FirstPersonPlayerJointAnimator.RENDERED_MAIN_HAND_ITEM);
                            ItemStack leftHandItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.MAIN_HAND_ITEM : FirstPersonPlayerJointAnimator.OFF_HAND_ITEM);
                            ItemStack rightHandItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.OFF_HAND_ITEM : FirstPersonPlayerJointAnimator.MAIN_HAND_ITEM);

                            leftHandItem = ItemStack.isSameItemSameComponents(leftHandItem, leftHandRenderedItem) ? leftHandItem : leftHandRenderedItem;
                            rightHandItem = ItemStack.isSameItemSameComponents(rightHandItem, rightHandRenderedItem) ? rightHandItem : rightHandRenderedItem;

                            this.renderItem(abstractClientPlayer, rightHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, rightItemPose, buffer, combinedLight, HumanoidArm.RIGHT);
                            this.renderItem(abstractClientPlayer, leftHandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, poseStack, leftItemPose, buffer, combinedLight, HumanoidArm.LEFT);


//                            if (!this.minecraft.isPaused()) {
//                                LocomotionMain.LOGGER.info(rightItemPose.getTransform().getScale(new Vector3f()));
//                            }

                            //this.renderItemInHand(abstractClientPlayer, ItemStack.EMPTY, poseStack, HumanoidArm.LEFT, animationPose, bufferSource, i);


                            //playerRenderer.renderRightHand(poseStack, bufferSource, i, abstractClientPlayer);
                            //poseStack.popPose();
                            poseStack.popPose();
                        }
                )
        );

        buffer.endBatch();
    }

    private void renderArm(AbstractClientPlayer abstractClientPlayer, PlayerModel playerModel, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int combinedLight) {
        PlayerSkin skin = abstractClientPlayer.getSkin();
        poseStack.pushPose();
        switch(arm){
            case LEFT -> {
                if (skin.model() == PlayerSkin.Model.SLIM) {
                    poseStack.translate(0.5 / 16f, 0, 0);
                }
                playerModel.leftSleeve.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
                playerModel.leftArm.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(skin.texture())), combinedLight, OverlayTexture.NO_OVERLAY);
            }
            case RIGHT -> {
                if (skin.model() == PlayerSkin.Model.SLIM) {
                    poseStack.translate(-0.5 / 16f, 0, 0);
                }
                playerModel.rightSleeve.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
                playerModel.rightArm.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(skin.texture())), combinedLight, OverlayTexture.NO_OVERLAY);
            }
        }
        poseStack.popPose();
    }

    public void renderItem(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            JointChannel jointChannel,
            MultiBufferSource buffer,
            int combinedLight,
            HumanoidArm side
    ) {
        if (!itemStack.isEmpty()) {
            poseStack.pushPose();
            jointChannel.transformPoseStack(poseStack, 16f);

            ItemRenderType renderType = ItemRenderType.fromItemStack(itemStack);
            ItemStack itemStackToRender = renderType == ItemRenderType.THIRD_PERSON_ITEM_STATIC ? itemStack.copy() : itemStack;
            //? if >= 1.21.5 {
            this.itemRenderer.renderStatic(entity, itemStackToRender, displayContext, poseStack, buffer, entity.level(), combinedLight, OverlayTexture.NO_OVERLAY, entity.getId() + displayContext.ordinal());
            //?} else
            /*this.itemRenderer.renderStatic(entity, itemStackToRender, displayContext, side == HumanoidArm.LEFT, poseStack, buffer, entity.level(), combinedLight, OverlayTexture.NO_OVERLAY, entity.getId() + displayContext.ordinal());*/
            poseStack.popPose();
        }
    }

    public void transformCamera(PoseStack poseStack){
        if(this.minecraft.options.getCameraType().isFirstPerson()){
            this.jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().ifPresent(animationPose -> {
                JointChannel cameraPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.CAMERA_JOINT);

                Vector3f cameraRot = cameraPose.getEulerRotationZYX();
                cameraRot.z *= -1;
                cameraPose.rotate(cameraRot, JointChannel.TransformSpace.LOCAL, JointChannel.TransformType.REPLACE);

                cameraPose.transformPoseStack(poseStack, 16f);
                //poseStack.mulPose(cameraPose.getTransform().setTranslation(cameraPose.getTransform().getTranslation(new Vector3f().div(16f))));
            });
        }
    }

    @Override
    public @NotNull PlayerModel getModel() {
        return ((PlayerRenderer)entityRenderDispatcher.getRenderer(minecraft.player)).getModel();
    }

    private enum ItemRenderType {
        THIRD_PERSON_ITEM,
        THIRD_PERSON_ITEM_STATIC;

        public static final List<Item> STATIC_ITEMS = List.of(
                Items.SHIELD
        );

        public static final List<Item> BLOCK_ITEM_OVERRIDES = List.of(
                Items.COBWEB
        );

        public static ItemRenderType fromItemStack(ItemStack itemStack) {
            Item item = itemStack.getItem();
            if (STATIC_ITEMS.contains(item)) {
                return THIRD_PERSON_ITEM_STATIC;
            }
            return THIRD_PERSON_ITEM;
        }
    }
}
