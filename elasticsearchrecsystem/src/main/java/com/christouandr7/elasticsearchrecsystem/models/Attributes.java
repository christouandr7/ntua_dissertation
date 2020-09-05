package com.christouandr7.elasticsearchrecsystem.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attributes {

    private DataUtility[] dataUtility;

    public DataUtility[] getDataUtility() {
        return dataUtility;
    }

    public void setDataUtility(DataUtility[] dataUtility) {
        this.dataUtility = dataUtility;
    }

}
