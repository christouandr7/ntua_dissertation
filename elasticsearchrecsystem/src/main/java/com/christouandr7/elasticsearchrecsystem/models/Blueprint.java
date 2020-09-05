package com.christouandr7.elasticsearchrecsystem.models;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;

public class Blueprint {

    @Id
    private String id;
    private int volume;
    private Double normalizedVolume;
    private Double accuracy;
    private Double normalizedAccuracy;
    private int completeness;
    private Double normalizedCompleteness;
    private int ramGain;
    private Double normalizedRamGain;
    //private Double ramLimit;
    private int spaceGain;
    private Double normalizedSpaceGain;
    //private Double spaceLimit;
    private Double availability;
    private Double normalizedAvailability;
    private Double averageResponseTime;
    private Double normalizedAverageResponseTime;


    ArrayList<Double> normalizedUtilities = new ArrayList<>();
    ArrayList<String> utilities = new ArrayList<>();

    public ArrayList<Double> getNormalizedUtilities() {
        return normalizedUtilities;
    }

    public void setNormalizedUtilities() {
        normalizedVolume = normalizationFunction(volume,0,10000);
        normalizedUtilities.add(normalizedVolume);
        utilities.add("volume");
        normalizedAccuracy = normalizationFunction(accuracy,0,1);
        normalizedUtilities.add(normalizedAccuracy);
        utilities.add("accuracy");
        normalizedAvailability = normalizationFunction(availability,0,100);
        normalizedUtilities.add(normalizedAvailability);
        utilities.add("availability");
        normalizedCompleteness = normalizationFunction(completeness,0,15);
        normalizedUtilities.add(normalizedCompleteness);
        utilities.add("completeness");
        normalizedRamGain = normalizationFunction(ramGain,0,200);
        normalizedUtilities.add(normalizedRamGain);
        utilities.add("ramGain");
        normalizedSpaceGain = normalizationFunction(spaceGain,0,200);
        normalizedUtilities.add(normalizedSpaceGain);
        utilities.add("spaceGain");
        normalizedAverageResponseTime = (1-normalizationFunction(averageResponseTime,0,6));
        normalizedUtilities.add(normalizedAverageResponseTime);
        utilities.add("averageResponseTime");
    }

    public Double getNormalizedVolume() {
        return normalizedVolume;
    }

    public Double getNormalizedAccuracy() {
        return normalizedAccuracy;
    }

    public Double getNormalizedCompleteness() {
        return normalizedCompleteness;
    }

    public Double getNormalizedRamGain() {
        return normalizedRamGain;
    }

    public Double getNormalizedSpaceGain() {
        return normalizedSpaceGain;
    }

    public Double getNormalizedAvailability() {
        return normalizedAvailability;
    }

    public Double getNormalizedAverageResponseTime() {
        return normalizedAverageResponseTime;
    }

    public ArrayList<String> getUtilities() {
        return utilities;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public int getCompleteness() {
        return completeness;
    }

    public void setCompleteness(int completeness) {
        this.completeness = completeness;
    }

    public int getRamGain() {
        return ramGain;
    }

    public void setRamGain(int ramGain) {
        this.ramGain = ramGain;
    }

    public int getSpaceGain() {
        return spaceGain;
    }

    public void setSpaceGain(int spaceGain) {
        this.spaceGain = spaceGain;
    }

    public Double getAvailability() {
        return availability;
    }

    public void setAvailability(Double availability) {
        this.availability = availability;
    }

    public Double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(Double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public double normalizationFunction(double utility, double min, double max){
        double normalized = (utility - min)/(max-min);
        return normalized;
    }
}
