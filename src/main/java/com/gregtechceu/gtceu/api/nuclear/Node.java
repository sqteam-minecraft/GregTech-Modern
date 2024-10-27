package com.gregtechceu.gtceu.api.nuclear;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;

public class Node {
    private String id;
    private IReactorElement component;

    public Node(String id, IReactorElement component) {
        this.id = id;
        this.component = component;
    }

    public String getId() {
        return id;
    }

    public IReactorElement getComponent() {
        return component;
    }
}