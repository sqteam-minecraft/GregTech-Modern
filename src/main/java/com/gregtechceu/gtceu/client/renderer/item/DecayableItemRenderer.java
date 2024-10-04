package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.client.model.ItemBakedModel;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GradientUtil;
import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DecayableItemRenderer implements IRenderer
{

    public DecayableItemRenderer() {}
/*
    public static void render(GuiGraphics graphics, LivingEntity entity, Level level, ItemStack stack, int xPosition, int yPosition, int seed) {
        if (stack.getItem() instanceof TagPrefixItem prefixItem) {
            int decayTime = stack.getOrCreateTag().getInt("DecayTime");
            if (decayTime <= 0) return;

            if (prefixItem.material.getElement() == null) return;

            TagPrefixItemRenderer model = TagPrefixItemRenderer.getMODELS().stream().filter(m -> m.getItem().equals(prefixItem))
                    .findFirst().orElse(null);
            if (model == null) return;

            ResourceLocation texturePath = model.getType().getItemTexturePath(model.getIconSet(), true);
            ResourceTexture texture = ResourceTexture.fromSpirit(texturePath);

            List<Material> decayMaterials = GTMaterials.getDecayMaterials(prefixItem.material);

            int color = prefixItem.material.getMaterialARGB();
            int decayColor = decayMaterials.get(0).getMaterialARGB();
            int blendedColor = GradientUtil.blend(color, decayColor, decayTime / (prefixItem.material.getElement().halfLifeSeconds() * 1.443f));

            texture.setColor(blendedColor);
            texture.draw(graphics, 0, 0, xPosition, yPosition, 16, 16);
        }
    }*/

    private final Map<Optional<Direction>, List<BakedQuad>> itemModelCache = new ConcurrentHashMap<>();
    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand,
                           PoseStack matrixStack, MultiBufferSource buffer, int combinedLight,
                           int combinedOverlay, BakedModel model)
    {
        IItemRendererProvider.disabled.set(true);
        Minecraft.getInstance().getItemRenderer().render(stack, transformType, leftHand, matrixStack, buffer,
                combinedLight, combinedOverlay, model);
//
//        (ItemBakedModel) (state, direction, random) -> itemModelCache.computeIfAbsent(
//                Optional.ofNullable(direction),
//                direction1 -> model.getQuads(state, direction, Minecraft.getInstance().player.getRandom()))
        //bakeQuads(direction1.orElse(null), ITEM_CONNECTIONS, 0)
        IItemRendererProvider.disabled.set(false);
        //DecayableItemRenderer
        //TagPrefixItemRenderer.renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
        //IRenderer.super.renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }
}
