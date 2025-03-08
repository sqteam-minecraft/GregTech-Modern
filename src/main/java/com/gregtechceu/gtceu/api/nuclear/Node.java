package com.gregtechceu.gtceu.api.nuclear;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;

/**
 * Represents a node in the flow network, identified by an ID and optionally linked to a reactor element.
 */
public record Node(String id, IReactorElement component) {}