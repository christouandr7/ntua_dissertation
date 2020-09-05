package com.christouandr7.elasticsearchrecsystem.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Properties {

    private Pro volume;
    private Pro accuracy;
    private Pro completeness;
    private Pro ramGain;
    //private Pro ramLimit;
    private Pro spaceGain;
    //private Pro spaceLimit;
    private Pro availability;
    private Pro averageResponseTime;

    public Pro getVolume() {
        return volume;
    }

    public void setVolume(Pro volume) {
        this.volume = volume;
    }

    public Pro getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Pro accuracy) {
        this.accuracy = accuracy;
    }

    public Pro getCompleteness() {
        return completeness;
    }

    public void setCompleteness(Pro completeness) {
        this.completeness = completeness;
    }

    public Pro getRamGain() {
        return ramGain;
    }

    public void setRamGain(Pro ramGain) {
        this.ramGain = ramGain;
    }

    /*public Pro getRamLimit() {
        return ramLimit;
    }

    public void setRamLimit(Pro ramLimit) {
        this.ramLimit = ramLimit;
    }
*/
    public Pro getSpaceGain() {
        return spaceGain;
    }

    public void setSpaceGain(Pro spaceGain) {
        this.spaceGain = spaceGain;
    }
    /*
        public Pro getSpaceLimit() {
            return spaceLimit;
        }

        public void setSpaceLimit(Pro spaceLimit) {
            this.spaceLimit = spaceLimit;
        }
    */
    public Pro getAvailability() {
        return availability;
    }

    public void setAvailability(Pro availability) {
        this.availability = availability;
    }

    public Pro getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(Pro averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }
}
