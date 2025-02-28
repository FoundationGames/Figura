package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    PlayerEntityRendererMixin(EntityRenderDispatcher dispatcher, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) { super(dispatcher, model, shadowRadius); }
    
    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    public void render(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        FiguraMod.setRenderingMode(abstractClientPlayerEntity, vertexConsumerProvider, ((PlayerEntityRenderer) (Object) this).getModel(), g);
    }

    @Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    public void render_tail(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        PlayerEntityModelAccess playerEntityModel = (PlayerEntityModelAccess) model;
        playerEntityModel.getDisabledParts().clear();
    }

    @Inject(at = @At("HEAD"), method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;)V", cancellable = true)
    private void renderArm_Head(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo info) {
        FiguraMod.setRenderingMode(player, vertexConsumers, ((PlayerEntityRenderer) (Object) this).getModel(), 0);
        PlayerData playerData = FiguraMod.getCurrData();
        PlayerEntityRenderer realRenderer = (PlayerEntityRenderer)(Object)this;
        PlayerEntityModel model = realRenderer.getModel();

        if(playerData.script != null) {
            playerData.script.render(FiguraMod.deltaTime);
        }

        TrustContainer trustData = PlayerTrustManager.getContainer(new Identifier("players", playerData.playerId.toString()));

        if (playerData != null && playerData.script != null && playerData.script.vanillaModifications != null && trustData.getBoolSetting(PlayerTrustManager.allowVanillaModID)) {
                playerData.script.applyCustomValues(model);
        } else {
            ModelPartAccess mpa = (ModelPartAccess) (Object) model.rightArm;
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());

            mpa = (ModelPartAccess) (Object) model.leftArm;
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
        }
    }

    @Inject(at = @At("RETURN"), method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;)V", cancellable = true)
    private void renderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo info) {
        FiguraMod.setRenderingMode(player, vertexConsumers, ((PlayerEntityRenderer) (Object) this).getModel(), 0);
        PlayerData playerData = FiguraMod.getCurrData();
        PlayerEntityRenderer realRenderer = (PlayerEntityRenderer)(Object)this;
        PlayerEntityModel model = realRenderer.getModel();
        
        if (playerData != null) {
            
            if (playerData.model != null) {
                if (playerData.texture == null || !playerData.texture.ready) {
                    return;
                }
                //We actually wanna use this custom vertex consumer, not the one provided by the render arguments.
                VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(playerData.texture.id));
                
                for (CustomModelPart part : playerData.model.all_parts) {
                    if(part.parentType == CustomModelPart.ParentType.RightArm && arm == model.rightArm){
                        matrices.push();
                        
                        model.rightArm.rotate(matrices);
                        part.render(999, matrices, vc, light, OverlayTexture.DEFAULT_UV);
                        
                        matrices.pop();
                    } else if(part.parentType == CustomModelPart.ParentType.LeftArm && arm == model.leftArm){
                        matrices.push();

                        model.leftArm.rotate(matrices);
                        part.render(999, matrices, vc, light, OverlayTexture.DEFAULT_UV);

                        matrices.pop();
                    }
                }
                
            }
        }

        PlayerEntityModelAccess playerEntityModel = (PlayerEntityModelAccess) model;
        playerEntityModel.getDisabledParts().clear();
    }
    
    
    @Inject(at = @At("RETURN"), method = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;setModelPose(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V")
    public void setModelPose(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfo inf){
        PlayerEntityModel model = this.getModel();
        PlayerEntityModelAccess playerEntityModel = (PlayerEntityModelAccess) model;

        if(playerEntityModel.getDisabledParts().contains(model.helmet)) model.helmet.visible = false;
        if(playerEntityModel.getDisabledParts().contains(model.jacket)) model.jacket.visible = false;
        if(playerEntityModel.getDisabledParts().contains(model.leftPantLeg)) model.leftPantLeg.visible = false;
        if(playerEntityModel.getDisabledParts().contains(model.rightPantLeg)) model.rightPantLeg.visible = false;
        if(playerEntityModel.getDisabledParts().contains(model.leftSleeve)) model.leftSleeve.visible = false;
        if(playerEntityModel.getDisabledParts().contains(model.rightSleeve)) model.rightSleeve.visible = false;
        
        playerEntityModel.getDisabledParts().clear();
    }

}