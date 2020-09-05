package com.christouandr7.elasticsearchrecsystem.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionalRequirements {

    private String[] methodTags;
    private String[] vdcTags;
    private String methodTagsString = "";
    private String vdcTagsString = "";
    private String content ="";

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setContent() {
        setMethodTagsString();
        setVdcTagsString();
        this.content = methodTagsString + vdcTagsString;
    }

    public String getMethodTagsString() {
        return methodTagsString;
    }

    public void setMethodTagsString() {
        for (int i = 0; i<methodTags.length; i++){
            methodTagsString += methodTags[i] + " ";
        }
    }

    public String getVdcTagsString() {
        return vdcTagsString;
    }

    public void setVdcTagsString() {
        for (int i = 0; i<vdcTags.length; i++){
            vdcTagsString += vdcTags[i] + " ";
        }
    }

    public String[] getMethodTags() {
        return methodTags;
    }

    public void setMethodTags(String[] methodTags) {
        this.methodTags = methodTags;
    }

    public String[] getVdcTags() {
        return vdcTags;
    }

    public void setVdcTags(String[] vdcTags) {
        this.vdcTags = vdcTags;
    }
}
