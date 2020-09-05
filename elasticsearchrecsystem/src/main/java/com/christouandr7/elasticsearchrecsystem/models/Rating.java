package com.christouandr7.elasticsearchrecsystem.models;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.ArrayList;

@Document(indexName = "recsystem", type = "Rating", shards = 1)
public class Rating {
    @Id
    private String id;
    private String userId;
    private String blueprintId;
    private int rating;
    private UserRequirements userRequirements;
    private ArrayList<String> dataUtilitiesStringArray = new ArrayList<String >();
    private ArrayList<Double> dataUtilitiesValuesArray = new ArrayList<Double>();
    private ArrayList<Double> normalizedDataUtilities = new ArrayList<Double>();

    private String dataUtilities = "";
    private String dataUtilitiesValues = "";

    public String getDataUtilities() {
        return dataUtilities;
    }

    public String getDataUtilitiesValues() {
        return dataUtilitiesValues;
    }

    public ArrayList<String> getDataUtilitiesStringArray() {
        return dataUtilitiesStringArray;
    }

    public ArrayList<Double> getDataUtilitiesValuesArray() {
        return dataUtilitiesValuesArray;
    }

    public ArrayList<Double> getNormalizedDataUtilities() {
        return normalizedDataUtilities;
    }

    public void setDataUtilitiesArrays(){
        //System.out.println("eimai mesa!");
        Double ramGain, spaceGain, volume, availability, accuracy, respTime, completeness;

        for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++) {
            if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAccuracy() != null) {
                accuracy = userRequirements.getAttributes().getDataUtility()[i].getProperties().getAccuracy().getMinimum();
                //System.out.println("eimai mesa acc!");
                //dataUtilities += "accuracy, ";
                dataUtilitiesStringArray.add("accuracy");
                dataUtilitiesValuesArray.add(accuracy);
                //dataUtilitiesValues += accuracy + ", ";
                normalizedDataUtilities.add(normalizationFunction(accuracy,0,1));
            }
            else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAvailability() != null) {
                availability = userRequirements.getAttributes().getDataUtility()[i].getProperties().getAvailability().getMinimum();
                //System.out.println("eimai mesa av!");
                dataUtilitiesStringArray.add("availability");
                dataUtilitiesValuesArray.add(availability);
                //dataUtilities += "availability, ";
                //dataUtilitiesValues += availability + ", ";
                normalizedDataUtilities.add(normalizationFunction(availability,0,100));

            }
            else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAverageResponseTime() != null) {
                respTime = userRequirements.getAttributes().getDataUtility()[i].getProperties().getAverageResponseTime().getMaximum();
                //System.out.println("eimai mesa resp!");
                dataUtilitiesStringArray.add("averageResponseTime");
                dataUtilitiesValuesArray.add(respTime);
                //dataUtilities += "averageResponseTime, ";
                //dataUtilitiesValues += respTime + ", ";
                normalizedDataUtilities.add(1-normalizationFunction(respTime,0,6));

            }
            else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getCompleteness() != null) {
                completeness = userRequirements.getAttributes().getDataUtility()[i].getProperties().getCompleteness().getMinimum();
                //System.out.println("eimai mesa com!");
                dataUtilitiesStringArray.add("completeness");
                dataUtilitiesValuesArray.add(completeness);
                //dataUtilities += "completeness, ";
                //dataUtilitiesValues += completeness + ", ";
                normalizedDataUtilities.add(normalizationFunction(completeness,0,15));

            }
            else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getVolume() != null) {
                volume = userRequirements.getAttributes().getDataUtility()[i].getProperties().getVolume().getMinimum();
                //System.out.println("eimai mesa vol!");
                dataUtilitiesValuesArray.add(volume);
                dataUtilitiesStringArray.add("volume");

                //dataUtilities += "volume, ";
                //dataUtilitiesValues += volume + ", ";
                normalizedDataUtilities.add(normalizationFunction(volume,0,10000));

            }
            else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getSpaceGain() != null) {
                spaceGain = userRequirements.getAttributes().getDataUtility()[i].getProperties().getSpaceGain().getValue();
                //System.out.println("eimai mesa spg!");
                dataUtilitiesStringArray.add("spaceGain");
                dataUtilitiesValuesArray.add(spaceGain);
                //dataUtilities += "spaceGain, ";
                //dataUtilitiesValues += spaceGain + ", ";
                normalizedDataUtilities.add(normalizationFunction(spaceGain,0,200));

            }
            else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getRamGain() != null){
                ramGain = userRequirements.getAttributes().getDataUtility()[i].getProperties().getRamGain().getValue();
                //System.out.println("eimai mesa ramg!");
                dataUtilitiesValuesArray.add(ramGain);
                dataUtilitiesStringArray.add("ramGain");
                //dataUtilities += "ramGain, ";
                //dataUtilitiesValues += ramGain + ", ";
                normalizedDataUtilities.add(normalizationFunction(ramGain,0,200));

            }
        }

//        dataUtilities = dataUtilities.substring(0,dataUtilities.length()-2);
  //      dataUtilitiesValues = dataUtilitiesValues.substring(0,dataUtilitiesValues.length()-2);
        //System.out.println("RatingId: " + getId() + " UserId: " + getUserId() + " BlueprintId: " + getBlueprintId());
        //System.out.println(dataUtilities);
        //System.out.println(dataUtilitiesValues);

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public UserRequirements getUserRequirements() {
        return userRequirements;
    }

    public void setUserRequirements(UserRequirements userRequirements) {
        this.userRequirements = userRequirements;
    }

    public double normalizationFunction(double utility, double min, double max){
        double normalized = (utility - min)/(max-min);
        return normalized;
    }


}