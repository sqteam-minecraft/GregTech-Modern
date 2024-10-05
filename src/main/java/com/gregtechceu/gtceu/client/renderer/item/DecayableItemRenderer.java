package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.client.model.ItemBakedModel;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.gregtechceu.gtceu.utils.GradientUtil;
import com.lowdragmc.lowdraglib.client.bakedpipeline.FaceQuad;
import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.gregtechceu.gtceu.client.renderer.block.MaterialBlockRenderer.LAYER_2_SUFFIX;
import static com.gregtechceu.gtceu.common.data.GTItems.MATERIAL_ITEMS;

public class DecayableItemRenderer implements IRenderer
{

    public DecayableItemRenderer() {}

    private static int mixColors(ItemStack stack) {
        if (stack.getItem() instanceof TagPrefixItem prefixItem) {
            int decayTime = stack.getOrCreateTag().getInt("DecayTime");
            if (decayTime <= 0) return 0;

            if (prefixItem.material.getElement() == null) return 0;

            TagPrefixItemRenderer model = TagPrefixItemRenderer.getMODELS().stream().filter(m -> m.getItem().equals(prefixItem))
                    .findFirst().orElse(null);
            if (model == null) return 0;

            ResourceLocation texturePath = model.getType().getItemTexturePath(model.getIconSet(), true);
            ResourceTexture texture = ResourceTexture.fromSpirit(texturePath);


            List<Material> decayMaterials = GTMaterials.getDecayMaterials(prefixItem.material);

            int color = prefixItem.material.getMaterialARGB();
            if (decayMaterials.size() == 0) return 0;
            int decayColor = decayMaterials.get(0).getMaterialARGB();
            int blendedColor = GradientUtil.blend(color, decayColor, decayTime / (prefixItem.material.getElement().halfLifeSeconds() * 1.443f));


            return GradientUtil.argbToRgba(blendedColor);
        }
        return 0;
    }

    private final Map<Optional<Direction>, List<BakedQuad>> itemModelCache = new ConcurrentHashMap<>();
    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand,
                           PoseStack matrixStack, MultiBufferSource buffer, int combinedLight,
                           int combinedOverlay, BakedModel bakedModel)
    {
        IItemRendererProvider.disabled.set(true);
        Minecraft.getInstance().getItemRenderer().render(stack, transformType, leftHand, matrixStack, buffer,
                combinedLight, combinedOverlay,
                new ItemBakedModel() {
                    BakedModel model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(stack.getItem());
                    @Override
                    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {

                        return model.getQuads(blockState, direction, randomSource)
                                .stream()
                                .map(quad -> {

                                    TextureAtlasSprite oldSprite = quad.getSprite();
                                    TextureAtlasSprite newSprite = new UnprotectedTextureAtlasSprite(oldSprite.atlasLocation(), oldSprite.contents(),
                                            (int) (oldSprite.getX()/oldSprite.getU0()), (int) (oldSprite.getY()/oldSprite.getV0()),
                                                oldSprite.getX(), oldSprite.getY(), mixColors(stack)){
                                    };
                                    int decayTime = stack.getOrCreateTag().getInt("DecayTime");
                                    return new BakedQuad(quad.getVertices(),
                                            decayTime,
                                            quad.getDirection(),
                                            quad.getSprite(),
                                            quad.isShade(),
                                            quad.hasAmbientOcclusion());
                                })
                                .toList();
                    }

                    @Override
                    public ItemTransforms getTransforms() {
                        return model.getTransforms();
                    }
                }
        );
        IItemRendererProvider.disabled.set(false);
    }
    private class UnprotectedTextureAtlasSprite extends TextureAtlasSprite{

        protected UnprotectedTextureAtlasSprite(ResourceLocation atlasLocation, SpriteContents contents, int originX, int originY, int x, int y, int colorToMix) {
            super(atlasLocation, new SpriteContents(
                    contents.name(),
                    new FrameSize(contents.width(), contents.height()),
                    new NativeImage(contents.getOriginalImage().getWidth(), contents.getOriginalImage().getHeight(), true),
                    AnimationMetadataSection.EMPTY,
                    null
            ), originX, originY, x, y);
        }

        private static NativeImage modifyImageColor(NativeImage image1, int materialRGBA){
            try (NativeImage result = new NativeImage(image1.getWidth(), image1.getHeight(), true)) {
                for (int x = 0; x < image1.getWidth(); ++x) {
                    for (int y = 0; y < image1.getHeight(); ++y) {
                        int color = image1.getPixelRGBA(x, y);
                        result.setPixelRGBA(x, y, 0x111111FF);//GradientUtil.multiplyBlendRGBA(, materialRGBA));
                    }
                }
                return result;
            }catch (Exception e){
                e.printStackTrace();
            }
            return image1;
        }
    }
}
