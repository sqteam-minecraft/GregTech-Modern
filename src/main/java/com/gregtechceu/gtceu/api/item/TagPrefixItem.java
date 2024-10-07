package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.client.renderer.item.TagPrefixItemRenderer;
import com.gregtechceu.gtceu.common.data.GTDamageTypes;

import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GradientUtil;
import com.lowdragmc.lowdraglib.Platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/2/14
 * @implNote MaterialItem
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TagPrefixItem extends Item {

    public final TagPrefix tagPrefix;
    public final Material material;

    public TagPrefixItem(Properties properties, TagPrefix tagPrefix, Material material) {
        super(properties);
        this.tagPrefix = tagPrefix;
        this.material = material;
        if (Platform.isClient()) {
            TagPrefixItemRenderer.create(this, tagPrefix.materialIconType(), material.getMaterialIconSet());
        }
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return getItemBurnTime();
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public void onRegister() {}

    @OnlyIn(Dist.CLIENT)
    public static ItemColor tintColor() {
        Map<Integer, Integer> colorMap = new HashMap<>();
        return (itemStack, index) -> {
            if (itemStack.getItem() instanceof TagPrefixItem prefixItem) {
                if (itemStack.getOrCreateTag().contains("DecayTime")) {
                    Element element = GTMaterials.getDecayableElement(prefixItem.material).getKey();
                    if (element != null) {
                        int decayColor = colorMap.computeIfAbsent(index, item -> {
                            int c = 0xFF121212;

                            List<Material> decayMaterials = GTMaterials.getDecayMaterials(prefixItem.material);

                            if (!decayMaterials.isEmpty()) {
                                Material decayMaterial = index >= decayMaterials.size() ? decayMaterials.get(decayMaterials.size() - 1) : decayMaterials.get(index);

                                c = decayMaterial.getLayerARGB(index);
                                c = c == -1 ? 0xFF121212 : c;
                            }
                            return c;
                        });

                        int color = prefixItem.material.getLayerARGB(index);
                        color = color == -1 ? 0xFF121212 : color;

                        int time = itemStack.getOrCreateTag().getInt("DecayTime");
                        float timeToDecay = element.halfLifeSeconds() * 1.443f * 20;

                        return GradientUtil.blend(color, decayColor, time / timeToDecay);
                    }
                }
                return prefixItem.material.getLayerARGB(index);
            }
            return -1;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if (this.tagPrefix.tooltip() != null) {
            this.tagPrefix.tooltip().accept(material, tooltipComponents);
        }

        if (!stack.getOrCreateTag().contains("DecayTime")) return;

        Element decayProductElement = GTMaterials.getDecayableElement(material).getKey();

        int decayTime = stack.getOrCreateTag().getInt("DecayTime");
        if (decayTime <= 0 || decayProductElement == null) return;

        TriConsumer<Material, List<Component>, Float> decayTooltip =
                (mat, tooltips, decayProgress) -> tooltips
                        .add(Component.translatable("gtceu.tooltip.decay", decayProgress));
        float timeToDecay = decayProductElement.halfLifeSeconds() * 1.443f * 20; // halfLife / ln2
        decayTooltip.accept(material, tooltipComponents, Math.round(decayTime * 10000 / timeToDecay) / 100f);
    }

    @Override
    public String getDescriptionId() {
        return tagPrefix.getUnlocalizedName(material);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return tagPrefix.getUnlocalizedName(material);
    }

    @Override
    public Component getDescription() {
        return tagPrefix.getLocalizedName(material);
    }

    @Override
    public Component getName(ItemStack stack) {
        return getDescription();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof ServerPlayer player) {
            Map.Entry<Element, Long> decayProductElement = GTMaterials.getDecayableElement(material);
            if (decayProductElement != null) {
                List<Material> decayMaterials = GTMaterials.getDecayMaterials(material);

                long halfLife = decayProductElement.getKey().halfLifeSeconds();
                int decayTime = stack.getOrCreateTag().getInt("DecayTime");
                int timeToDecay = (int) (halfLife * 1.443) * 20; // halfLife / ln2
                decayTime++;

                if (decayTime >= timeToDecay) {
                    int count = stack.getCount();
                    stack.setCount(0);

                    decayMaterials.forEach(decayMaterial -> {
                        var targetItem = GTItems.MATERIAL_ITEMS.get(tagPrefix, decayMaterial);

                        ItemStack decayStack = ItemStack.EMPTY;
                        if (targetItem != null) decayStack = targetItem.get().getDefaultInstance();

                        decayStack.setCount(count);
                        player.addItem(decayStack);
                    });
                } else stack.getOrCreateTag().putInt("DecayTime", decayTime);
            }

            if (player.tickCount % 20 == 0) {

                if (tagPrefix != TagPrefix.ingotHot || !material.hasProperty(PropertyKey.BLAST))
                    return;

                float heatDamage = ((material.getBlastTemperature() - 1750) / 1000.0F) + 2;
                ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
                if (!armor.isEmpty() && armor.getItem() instanceof ArmorComponentItem armorItem) {
                    heatDamage *= armorItem.getArmorLogic().getHeatResistance();
                }
                if (heatDamage > 0.0) {
                    player.hurt(GTDamageTypes.HEAT.source(level), heatDamage);
                } else if (heatDamage < 0.0) {
                    player.hurt(player.damageSources().freeze(), -heatDamage);
                }
            }
        }
    }

    public int getItemBurnTime() {
        DustProperty property = material == null ? null : material.getProperty(PropertyKey.DUST);
        if (property != null)
            return (int) (property.getBurnTime() * tagPrefix.getMaterialAmount(material) / GTValues.M);
        return -1;
    }

    // TODO BEACON PAYMENT
}
