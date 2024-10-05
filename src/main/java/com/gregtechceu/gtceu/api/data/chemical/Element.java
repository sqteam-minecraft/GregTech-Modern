package com.gregtechceu.gtceu.api.data.chemical;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * This is some kind of Periodic Table, which can be used to determine "Properties" of the Materials.
 */
@Accessors(fluent = true, chain = false)
public class Element {

    /**
     * Amount of Protons
     */
    @Getter
    @Setter
    private long protons;
    /**
     * Amount of Neutrons
     */
    @Getter
    @Setter
    private long neutrons;
    /**
     * Amount of Half Life this Material has in Seconds. -1 for stable Materials
     */
    @Getter
    @Setter
    private long halfLifeSeconds;
    /**
     * String representing the Elements this element decays to. Separated by an '&' Character
     */
    @Getter
    @Setter
    private List<Element> decayTo;
    /**
     * Name of the Element
     */
    @Getter
    @Setter
    private String name;
    /**
     * Symbol of the Element
     */
    @Getter
    @Setter
    private String symbol;
    /**
     * Is this element an isotope?
     */
    @Getter
    @Setter
    private boolean isIsotope;

    public long mass() {
        return protons + neutrons;
    }

    public Element(long protons, long neutrons, long halfLifeSeconds, List<Element> decayTo, String name, String symbol,
                   boolean isIsotope) {
        this.protons = protons;
        this.neutrons = neutrons;
        this.halfLifeSeconds = halfLifeSeconds;
        this.decayTo = decayTo!=null?decayTo:List.of();
        this.name = name;
        this.symbol = symbol;
        this.isIsotope = isIsotope;
    }
}
