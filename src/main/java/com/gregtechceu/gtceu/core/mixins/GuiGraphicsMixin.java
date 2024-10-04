package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.LampBlockItem;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.client.renderer.item.DecayableItemRenderer;
import com.gregtechceu.gtceu.client.renderer.item.LampItemOverlayRenderer;
import com.gregtechceu.gtceu.client.renderer.item.ToolChargeBarRenderer;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    // Prevents recursion in the hook below
    @Unique
    private static final ThreadLocal<ItemStack> GTCEU$OVERRIDING_FOR = new ThreadLocal<>();

    @Shadow
    private void renderItem(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y,
                            int seed, int guiOffset) {
        throw new AssertionError();
    }

    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                     value = "FIELD",
                     target = "Lnet/minecraft/client/gui/GuiGraphics;minecraft:Lnet/minecraft/client/Minecraft;",
                     shift = At.Shift.BEFORE,
                     ordinal = 0))
    private void gtceu$renderCustomItemDecorations(Font font, ItemStack stack, int x, int y, String text,
                                                   CallbackInfo ci) {
        GuiGraphics self = (GuiGraphics) (Object) this;
        if (stack.getItem() instanceof IGTTool toolItem) {
            ToolChargeBarRenderer.renderBarsTool(self, toolItem, stack, x, y);
        } else if (stack.getItem() instanceof IComponentItem componentItem) {
            ToolChargeBarRenderer.renderBarsItem(self, componentItem, stack, x, y);
        } else if (stack.getItem() instanceof LampBlockItem) {
            LampItemOverlayRenderer.renderOverlay(self, stack, x, y);
        }
    }

//    @Redirect(
//            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
//            at = @At(
//                     value = "INVOKE",
//                     target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V")
//    )
//    private void gtceu$renderDecayableItem(ItemRenderer instance, ItemStack itemStack, ItemDisplayContext displayContext,
//                                           boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
//                                           int combinedLight, int combinedOverlay, BakedModel p_model) {
//        GuiGraphics self = (GuiGraphics) (Object) this;
//        if (!(itemStack.getItem() instanceof TagPrefixItem prefixItem) || !itemStack.getOrCreateTag().contains("DecayTime")) {
//            instance.render(itemStack, displayContext, leftHand, poseStack, buffer, combinedLight, combinedOverlay, p_model);
//        }
//    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(value = "HEAD"),
            cancellable = true)
    protected void gtceu$renderItem(@Nullable LivingEntity livingEntity, @Nullable Level level, ItemStack stack,
                                    int x, int y, int seed, int z, CallbackInfo ci) {
        if (GTCEU$OVERRIDING_FOR.get() != null) {
            return;
        }
        //if (stack.getItem() instanceof TagPrefixItem prefixItem) {
        //    DecayableItemRenderer.render((GuiGraphics) (Object) this, livingEntity, level, stack, x, y, seed);
        //}

        Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
        if (Screen.hasShiftDown() && researchData != null) {
            Collection<GTRecipe> recipes = researchData.getFirst().getDataStickEntry(researchData.getSecond());
            if (recipes != null && !recipes.isEmpty()) {
                for (var recipe : recipes) {
                    ItemStack output = ItemRecipeCapability.CAP
                            .of(recipe.getOutputContents(ItemRecipeCapability.CAP).get(0).content).getItems()[0];

                    if (!output.isEmpty() && !ItemStack.isSameItemSameTags(output, stack)) {
                        gtceu$renderInstead(livingEntity, level, output, x, y, seed, z);
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }

    @Unique
    private void gtceu$renderInstead(@Nullable LivingEntity livingEntity, @Nullable Level level, ItemStack stack,
                                     int x, int y, int seed, int z) {
        GTCEU$OVERRIDING_FOR.set(stack);
        try {
            this.renderItem(livingEntity, level, stack, x, y, seed, z);
        } finally {
            GTCEU$OVERRIDING_FOR.remove();
        }
    }
}
