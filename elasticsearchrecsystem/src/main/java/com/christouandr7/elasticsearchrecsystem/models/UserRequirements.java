package com.christouandr7.elasticsearchrecsystem.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequirements {

    private FunctionalRequirements functionalRequirements;
    private Attributes attributes;

    public FunctionalRequirements getFunctionalRequirements() {
        return functionalRequirements;
    }

    public void setFunctionalRequirements(FunctionalRequirements functionalRequirements) {
        this.functionalRequirements = functionalRequirements;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

}
