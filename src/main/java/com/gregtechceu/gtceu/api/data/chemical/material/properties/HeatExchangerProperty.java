package com.gregtechceu.gtceu.api.data.chemical.material.properties;

public class HeatExchangerProperty implements IMaterialProperty<HeatExchangerProperty> {

    @Override
    public void verifyProperty(MaterialProperties properties) {
        properties.ensureSet(PropertyKey.INGOT, true);
    }
}
