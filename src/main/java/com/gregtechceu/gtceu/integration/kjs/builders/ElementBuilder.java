package com.gregtechceu.gtceu.integration.kjs.builders;

import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.registry.registrate.BuilderBase;
import com.gregtechceu.gtceu.common.data.GTElements;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementBuilder extends BuilderBase<Element> {

    public transient long protons, neutrons, halfLifeSeconds;
    public transient String name, symbol;
    public List<Element> decayTo;
    public transient boolean isIsotope;

    public ElementBuilder(ResourceLocation i, Object... args) {
        super(i);
        // Special handling if somehow called from create(name, type, args...) (it does that)
        protons = args[0] instanceof Number number ? number.intValue() : Double.valueOf(args[0].toString()).intValue();
        neutrons = ((Number) args[1]).intValue();
        halfLifeSeconds = ((Number) args[2]).intValue();
        decayTo = args[3] == null ? null : (List<Element>) args[3];
        name = i.getPath();
        symbol = args[4] == null ? "" : args[4].toString();
        isIsotope = (Boolean) args[5];
    }

    @Override
    public Element register() {
        return value = GTElements.createAndRegister(protons, neutrons, halfLifeSeconds, decayTo, name, symbol,
                isIsotope);
    }
}
