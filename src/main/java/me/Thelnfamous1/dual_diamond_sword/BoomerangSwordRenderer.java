package me.Thelnfamous1.dual_diamond_sword;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BoomerangSwordRenderer<T extends BoomerangSwordEntity> extends EntityRenderer<T> {
   private final ItemRenderer itemRenderer;
   private final float scale;
   private final boolean fullBright;

   public BoomerangSwordRenderer(EntityRendererProvider.Context pContext, float pScale, boolean pFullBright) {
      super(pContext);
      this.itemRenderer = pContext.getItemRenderer();
      this.scale = pScale;
      this.fullBright = pFullBright;
   }

   public BoomerangSwordRenderer(EntityRendererProvider.Context pContext) {
      this(pContext, 1.0F, false);
   }

   @Override
   protected int getBlockLightLevel(T pEntity, BlockPos pPos) {
      return this.fullBright ? 15 : super.getBlockLightLevel(pEntity, pPos);
   }

   @Override
   public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.pushPose();
      pMatrixStack.scale(this.scale, this.scale, this.scale);
      pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(-90.0F - Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot())));
      pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(((float)pEntity.tickCount + pPartialTicks) * -75.0F));
      this.itemRenderer.renderStatic(pEntity.getItem(), ItemTransforms.TransformType.GROUND, pPackedLight, OverlayTexture.NO_OVERLAY, pMatrixStack, pBuffer, pEntity.getId());
      pMatrixStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(T pEntity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}