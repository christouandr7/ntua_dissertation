package com.christouandr7.elasticsearchrecsystem.services;

import com.christouandr7.elasticsearchrecsystem.config.ElasticsearchConfig;
import com.christouandr7.elasticsearchrecsystem.models.*;
import com.christouandr7.elasticsearchrecsystem.models.Properties;
import com.christouandr7.elasticsearchrecsystem.repository.RatingsRepository;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.*;
import com.google.common.base.Stopwatch;
import java.util.Collections;


import javax.jws.soap.SOAPBinding;
import javax.smartcardio.Card;

import static java.lang.Math.round;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.scriptQuery;

import java.io.IOException;
import java.util.*;

@RestController
public class UserRatingsService {

    @Autowired
    private RatingsRepository ratingsRepository;

    @Autowired
    ElasticsearchConfig config;

    @Autowired
    ElasticsearchTemplate template;


    @GetMapping("/ratings")
    public List<Rating> getUsers() {
        return Lists.newArrayList(ratingsRepository.findAll());
    }

    @GetMapping("/ratings/{id}")
    public Rating getUser(@PathVariable String id) {
        return ratingsRepository.findById(id);
    }

    @GetMapping("/scoreByUR")
    public List<Pair<String, Double>> getScoreByURs(@RequestBody UserRequirements userRequirements) {
        //Stopwatch stopwatch = Stopwatch.createStarted();
        ArrayList<Pair<String, Double>> blueprintScores = new ArrayList<>();
        QueryBuilder query = null;
        List<Rating> ratings = null;
        double doubleScore, doubleMaxScore = 1.0;
        float max_score = 0;
        userRequirements.getFunctionalRequirements().setContent();

        Double ramGain, spaceGain, volume, availability, accuracy, respTime, completeness;
        //Double volume = 1000000.0, availability = 10000.0, accuracy = 10000.0, respTime = -1.0, completeness = 100000.0;
        //spaceGain = 2000.0;
        //ramGain = 2000.0;
        String dataUtilities = "";
        String dataUtilitiesValues = "";
        ArrayList<String> dataUtilitiesStringArray = new ArrayList<>();
        ArrayList<Double> dataUtilitiesValuesArray = new ArrayList<>();
        ArrayList<Double> normalizedDataUtilities = new ArrayList<>();

        for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++) {
            if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAccuracy() != null) {
                accuracy = userRequirements.getAttributes().getDataUtility()[i].getProperties().getAccuracy().getMinimum();
                //dataUtilities += "accuracy, ";
                //dataUtilitiesValues += accuracy + ", ";
                dataUtilitiesStringArray.add("accuracy");
                dataUtilitiesValuesArray.add(accuracy);
                normalizedDataUtilities.add(normalizationFunction(accuracy, 0, 1));
            } else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAvailability() != null) {
                availability = userRequirements.getAttributes().getDataUtility()[i].getProperties().getAvailability().getMinimum();
                //dataUtilities += "availability, ";
                //dataUtilitiesValues += availability + ", ";
                dataUtilitiesStringArray.add("availability");
                dataUtilitiesValuesArray.add(availability);
                normalizedDataUtilities.add(normalizationFunction(availability, 0, 100));
            } else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAverageResponseTime() != null) {
                respTime = userRequirements.getAttributes().getDataUtility()[i].getProperties().getAverageResponseTime().getMaximum();
                //dataUtilities += "averageResponseTime, ";
                //dataUtilitiesValues += respTime + ", ";
                dataUtilitiesStringArray.add("averageResponseTime");
                dataUtilitiesValuesArray.add(respTime);
                normalizedDataUtilities.add(1 - normalizationFunction(respTime, 0, 6));
            } else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getCompleteness() != null) {
                completeness = userRequirements.getAttributes().getDataUtility()[i].getProperties().getCompleteness().getMinimum();
                //dataUtilities += "completeness, ";
                //dataUtilitiesValues += completeness + ", ";
                dataUtilitiesStringArray.add("completeness");
                dataUtilitiesValuesArray.add(completeness);
                normalizedDataUtilities.add(normalizationFunction(completeness, 0, 15));
            } else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getVolume() != null) {
                volume = userRequirements.getAttributes().getDataUtility()[i].getProperties().getVolume().getMinimum();
                //dataUtilities += "volume, ";
                //dataUtilitiesValues += volume + ", ";
                dataUtilitiesStringArray.add("volume");
                dataUtilitiesValuesArray.add(volume);
                normalizedDataUtilities.add(normalizationFunction(volume, 0, 10000));
            } else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getSpaceGain() != null) {
                spaceGain = userRequirements.getAttributes().getDataUtility()[i].getProperties().getSpaceGain().getValue();
                //dataUtilities += "spaceGain, ";
                //dataUtilitiesValues += spaceGain + ", ";
                dataUtilitiesStringArray.add("spaceGain");
                dataUtilitiesValuesArray.add(spaceGain);
                normalizedDataUtilities.add(normalizationFunction(spaceGain, 0, 200));
            } else if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getRamGain() != null) {
                ramGain = userRequirements.getAttributes().getDataUtility()[i].getProperties().getRamGain().getValue();
                //dataUtilities += "ramGain, ";
                //dataUtilitiesValues += ramGain + ", ";
                dataUtilitiesStringArray.add("ramGain");
                dataUtilitiesValuesArray.add(ramGain);
                normalizedDataUtilities.add(normalizationFunction(ramGain, 0, 200));
            }
        }

        //System.out.println(normalizedDataUtilities);
/*
        dataUtilities = dataUtilities.substring(0,dataUtilities.length()-2);
        dataUtilitiesValues = dataUtilitiesValues.substring(0,dataUtilitiesValues.length()-2);
        System.out.println(dataUtilities);
        System.out.println(dataUtilitiesValues);
*/

        try {
            Client client = config.client();

            ArrayList<String> blueprintIdsArray = setBlueprintIdsArray(ratingsRepository);

            for (int i = 0; i < blueprintIdsArray.size(); i++) {
                //System.out.println(blueprintIdsArray.get(i));
            }


            for (int i = 0; i < blueprintIdsArray.size(); i++) {
                Double sum = 0.0, sum2 = 0.0, sum3 = 0.0, sum4 = 0.0, sum5 = 0.0, sum6 = 0.0, sum7 = 0.0, sum8 = 0.0;
                Double weightsSum = 0.0, weightsSum2 = 0.0, weightsSum3 = 0.0, weightsSum4 = 0.0, weightsSum5 = 0.0, weightsSum6 = 0.0, weightsSum7 = 0.0, weightsSum8 = 0.0;
                Double score, score2, score3, score4, score5, score6, score7, score8;

                //System.out.println("**************************************************************************");
                QueryBuilder filterQuery =
                        QueryBuilders.boolQuery().
                                filter(QueryBuilders.termQuery("blueprintId", blueprintIdsArray.get(i)));

                NativeSearchQuery nativeSearchQuery =
                        new NativeSearchQueryBuilder()
                                .withQuery(filterQuery)
                                .withPageable(new PageRequest(0, 500))
                                .build();

                ratings = template.queryForList(nativeSearchQuery, Rating.class);

/*                System.out.println("BLUEPRINT ID : " +blueprintIdsArray.get(i));

                for (int rrr = 0; rrr < ratings.size(); rrr++){
                    System.out.println(ratings.get(rrr).getId());
                }
*/
/*
                String blueprintFilterQuery = "{\n" +
                        "\t\"query\" : {\n" +
                        "\t\t\"filtered\" : {\n" +
                        "\t\t\t\"filter\" : {\n" +
                        "\t\t\t\t\"bool\" : {\n" +
                        "\t\t\t\t\t\"must\" : {\n" +
                        "\t\t\t\t\t\t\"term\" : {\n" +
                        "\t\t\t\t\t\t\t\"blueprintId\" : " + blueprintIdsArray.get(i) + "\n" +
                        "\t\t\t\t\t\t}\n" +
                        "\t\t\t\t\t}\n" +
                        "\t\t\t\t}\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}";

                SearchResponse response2 = client.prepareSearch()
                        .setQuery(blueprintFilterQuery).setMinScore(0).setSize(100)
                        .execute().actionGet();

                for (int rr = 0; rr < response2.getHits().getHits().length; rr++){
                    System.out.println(response2.getHits().getAt(rr).getSource().get("id") + "------------------");
                }

*/


                for (int j = 0; j < ratings.size(); j++) {
                    ArrayList<Double> commonUtilities = new ArrayList<>();
                    ArrayList<Double> myCommonUtilities = new ArrayList<>();
                    for (int k = 0; k < normalizedDataUtilities.size(); k++) {
                        /*for (int kk = 0; kk < ratings.get(j).getDataUtilitiesStringArray().size(); kk++) {
                            if (dataUtilitiesStringArray.get(k).equals(ratings.get(j).getDataUtilitiesStringArray().get(kk))) {
                                commonUtilities.add(ratings.get(j).getNormalizedDataUtilities().get(kk));
                                myCommonUtilities.add(normalizedDataUtilities.get(k));
                            }
                        }
                        */
                        if (ratings.get(j).getDataUtilitiesStringArray().contains(dataUtilitiesStringArray.get(k))) {
                            int aaa = ratings.get(j).getDataUtilitiesStringArray().indexOf(dataUtilitiesStringArray.get(k));
                            commonUtilities.add(ratings.get(j).getNormalizedDataUtilities().get(aaa));
                            myCommonUtilities.add(normalizedDataUtilities.get(k));
                        }
                    }


/*
                    Map parameters = new HashMap();
                    parameters.put("maxScore",1);

                    FunctionScoreQueryBuilder contentQuery =
                            new FunctionScoreQueryBuilder(
                            QueryBuilders.boolQuery().
                                    filter(
                                        QueryBuilders.termQuery("id",ratings.get(j).getId())).
                                    must(
                                        QueryBuilders.matchQuery("userRequirements.functionalRequirements.content",
                                            userRequirements.getFunctionalRequirements().getContent()))).
                            add(new ScriptScoreFunctionBuilder(
                                    new Script("(maxScore * (1 - 1/(100*_score)))" ,
                                            ScriptService.ScriptType.INLINE,
                                            "javascript",
                                            parameters))).
                            boostMode("replace");


                    String elastic = "{\n" +
                            "\t\"function_score\" : {\n" +
                            "\t\t\"query\" : {\n" +
                            "\t\t\t\"bool\" : {\n" +
                            "\t\t\t\t\"must\" : [\n" +
                            "\t\t\t\t\t{\n" +
                            "\t\t\t\t\t\t\"term\" : {\n" +
                            "\t\t\t\t\t\t\t\"id\" : {\n" +
                            "\t\t\t\t\t\t\t\t\"value\" : " + ratings.get(j).getId() + "\n" +
                            "\t\t\t\t\t\t\t}\n" +
                            "\t\t\t\t\t\t}\n" +
                            "\t\t\t\t\t},\n" +
                            "\t\t\t\t\t{\n" +
                            "\t\t\t\t\t\t\"match\" : {\n" +
                            "\t\t\t\t\t\t\t\"userRequirements.functionalRequirements.content\" : \"" + userRequirements.getFunctionalRequirements().getContent() + "\" \n" +
                            "\t\t\t\t\t\t}\n" +
                            "\t\t\t\t\t}\t\n" +
                            "\t\t\t\t]\n" +
                            "\t\t\t}\n" +
                            "\t\t},\n" +
                            "\t\t\"functions\" : [\n" +
                            "\t\t\t{\n" +
                            "\t\t\t\t\"script_score\" : {\n" +
                            "\t\t\t\t\t\"script\" : {\n" +
                            "\t\t\t\t\t\t\"lang\" : \"javascript\",\n" +
                            "\t\t\t\t\t\t\"params\" : {\n" +
                            "\t\t\t\t\t\t\t\"max\" : " + 1 +"\n" +
                            "\t\t\t\t\t\t},\n" +
                            "\t\t\t\t\t\t\"inline\" : \"2\"\n" +
                            "\t\t\t\t\t}\n" +
                            "\t\t\t\t}\n" +
                            "\t\t\t}\n" +
                            "\t\t]" +
                            "\t}\n" +
                            "}";

                            */

                    String contentScoreQuery =
                            "{\n" +
                                    "\t\"function_score\" : {\n" +
                                    "\t\t\"query\" : {\n" +
                                    "\t\t\t\"filtered\" : {\n" +
                                    "\t\t\t\t\"query\" : {\n" +
                                    "\t\t\t\t\t\"bool\" : {\n" +
                                    "\t\t\t\t\t\t\"must\" : [\n" +
                                    "\t\t\t\t\t\t\t{\n" +
                                    "\t\t\t\t\t\t\t\t\"match\" : {\n" +
                                    "\t\t\t\t\t\t\t\t\t\"userRequirements.functionalRequirements.content\" : \""
                                    + userRequirements.getFunctionalRequirements().getContent() + "\"\n" +
                                    "\t\t\t\t\t\t\t\t}\n" +
                                    "\t\t\t\t\t\t\t}\n" +
                                    "\t\t\t\t\t\t]\n" +
                                    "\t\t\t\t\t}\n" +
                                    "\t\t\t\t},\n" +
                                    "\t\t\t\t\"filter\" : {\n" +
                                    "\t\t\t\t\t\"term\" : {\n" +
                                    "\t\t\t\t\t\t\"id\" : \"" + ratings.get(j).getId() + "\"\n" +
                                    "\t\t\t\t\t}\n" +
                                    "\t\t\t\t}\n" +
                                    "\t\t\t}\n" +
                                    "\t\t},\n" +
                                    "\t\t\"functions\" : [\n" +
                                    "\t\t\t{\n" +
                                    "\t\t\t\t\"script_score\" : {\n" +
                                    "\t\t\t\t\t\"script\" : \"(maxScore * (1 - 1/(25 * _score)));\",\n" +
                                    //"\t\t\t\t\t\"script\" : \"(1 - 1/(1 + 100 * _score));\",\n" +
                                    "\t\t\t\t\t\"lang\" : \"javascript\",\n" +
                                    "\t\t\t\t\t\"params\" : {\n" +
                                    "\t\t\t\t\t\t\"maxScore\" : 1\n" +
                                    "\t\t\t\t\t}\n" +
                                    "\t\t\t\t}\n" +
                                    "\t\t\t}\n" +
                                    "\t\t],\n" +
                                    "\t\t\"boost_mode\" : \"replace\"\n" +
                                    "\t}\n" +
                                    "}";

                    SearchResponse response = client.prepareSearch()
                            .setQuery(contentScoreQuery).setMinScore(0).setSize(100)
                            .execute().actionGet();

                    //if (max_score < response.getHits().getMaxScore()) max_score = response.getHits().getMaxScore();

                    //System.out.println(response.toString());
                    float floatScore;
                    if (response.getHits().totalHits() >= 0.4) {
                        floatScore = response.getHits().maxScore();
                    }
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
                    else floatScore = (float) 0.4;
                    doubleScore = floatScore;
                    commonUtilities.add(doubleScore);
                    myCommonUtilities.add(1.0);

                    //Rating ratingi = ratingsRepository.findById(ratings.get(j).getId());
                    //ratingi.setCommon(commonUtilities);
                    //ratingi.setUserSearchingCommon(myCommonUtilities);
                    //ratingsRepository.save(ratingi);


                    //System.out.println("************************************************************");
                    //System.out.println(response.toString());
                    //System.out.println("************************************************************");

                    Double euclideanSimilarity = euclideanSimilarity(myCommonUtilities, commonUtilities);
                    //Double euclideanSimilaritySquared = euclideanSimilaritySquared(myCommonUtilities, commonUtilities);
                    //Double cosineSimilarity = cosineSimilarity(myCommonUtilities, commonUtilities);
                    //Double manhattanSimilarity = manhattanSimilarity(myCommonUtilities,commonUtilities);
                    //Double pearsonsSimilarity = pearsonsCorrelationSimilarity(myCommonUtilities, commonUtilities);

                    //System.out.println(commonUtilities.toString());
                    //System.out.println(myCommonUtilities.toString());

                    //System.out.println("CosineSimilarity = " + cosineSimilarity);
                    //System.out.println("EuclideanDistanceSimilarity : " + euclideanSimilarity);
                    //System.out.println("ManhattanSimilarity : " + manhattanSimilarity);
                    //System.out.println("PearsonsCorrelation : " + pearsonsSimilarity);


                    //EUCLIDEAN
                    //sum += euclideanSimilarity * ratings.get(j).getRating();
                    //weightsSum += euclideanSimilarity;


                    /*sum8 += euclideanSimilaritySquared * ratings.get(j).getRating();
                    weightsSum8 += euclideanSimilaritySquared;
                    sum2 += cosineSimilarity * ratings.get(j).getRating();
                    weightsSum2 += cosineSimilarity;
                    Double arccos = Math.acos(cosineSimilarity);
                    sum3 += (1-2*arccos/Math.PI) * ratings.get(j).getRating();
                    weightsSum3 += (1-2*arccos/Math.PI);
                    */


                    //MANHATTAN
                    //sum4 += manhattanSimilarity * ratings.get(j).getRating();
                    //weightsSum4 += manhattanSimilarity;

/*
                    if (normalizedDataUtilities.size() >= ratings.get(j).getDataUtilitiesValuesArray().size()) {
                        sum6 += manhattanSimilarity * commonUtilities.size() * ratings.get(j).getRating();
                        weightsSum6 += manhattanSimilarity * commonUtilities.size();
                    }
                    else {
                        sum6 += manhattanSimilarity * ratings.get(j).getRating();
                        weightsSum6 += manhattanSimilarity;
                    }
*/


                    //if (normalizedDataUtilities.size() >= ratings.get(j).getDataUtilitiesValuesArray().size()) {



                    //sum5 += ratings.get(j).getRating() * euclideanSimilarity/commonUtilities.size();
                    //weightsSum5 += euclideanSimilarity / commonUtilities.size();

                    sum5 += euclideanSimilarity * commonUtilities.size() * ratings.get(j).getRating();
                    weightsSum5 += euclideanSimilarity * commonUtilities.size();

                    /*
                        }
                        else {
                        sum5 += euclideanSimilarity * ratings.get(j).getRating();
                        weightsSum5 += euclideanSimilarity;
                    }
                    */

                }

                //EUCLIDEAN
                //score = sum / weightsSum;
                //score2 = sum2 / weightsSum2;
                //score3 = sum3 / weightsSum3;

                //MANHATTAN
                //score4 = sum4 / weightsSum4;

                //WEIGHTED EUCLIDEAN
                score5 = sum5 / weightsSum5;

                //WEIGHTED MANHATTAN
                //score6 = sum6 / weightsSum6;


                //score7 = sum7 / weightsSum7;
                //score8 = sum8 / weightsSum8;

                //System.out.println("=========================================");
                //System.out.println("Euclidean Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score);
                //System.out.println("EuclideanSquared Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score8);
                //System.out.println("WEIGHTED Euclidean Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score5);
                Pair<String, Double> pair = new Pair(blueprintIdsArray.get(i), score5);
                blueprintScores.add(pair);
                //System.out.println("Cosine Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score2);
                //System.out.println("Angular Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score3);
                //System.out.println("Manhattan Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score4);
                //System.out.println("Pearsons Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score6);
                //System.out.println("WEIGHTED Pearsons Score for Blueprint " + blueprintIdsArray.get(i) + " = " + score7);
/*
                System.out.println("=========================================SCORES WITH NUM OF USERS FORMULA");
                System.out.println("Euclidean Score for Blueprint " + blueprintIdsArray.get(i) + " = " + sum/ratings.size());
                System.out.println("Cosine Score for Blueprint " + blueprintIdsArray.get(i) + " = " + sum2/ratings.size());
                System.out.println("Angular Score for Blueprint " + blueprintIdsArray.get(i) + " = " + sum3/ratings.size());
                System.out.println("Manhattan Score for Blueprint " + blueprintIdsArray.get(i) + " = " + sum4/ratings.size());
*/
            }


/*
                for (SearchHit hit : response.getHits()) {
                    System.out.println(hit.getScore());
                }
                //String ans =  response.toString();

                response = client.prepareSearch()
                        .setQuery(query3)
                        .execute().actionGet();

                System.out.println(response.getHits().getAt(1).getScore());

            response = client.prepareSearch()
                    .setQuery(query2)
                    .execute().actionGet();

            System.out.println(response.getHits().getAt(1).getScore());

            ans =  response.toString();
            // SearchRequest searchRequest = Requests.searchRequest().source(SearchSourceBuilder.searchSource().query(query2));
*/


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //System.out.println(blueprintScores.toString());
        //Collections.sort(blueprintScores, Comparator.comparing(p -> -p.getValue()));

        //stopwatch.stop(); // optional
        //System.out.println("that took: " + stopwatch); // formatted string like "12.3 ms"

        return blueprintScores;

    }

    @PostMapping("/ratings")
    public long postListofRatings(@RequestBody List<Rating> ratings) {
        for (int i = 0; i < ratings.size(); i++) {
            //System.out.println(ratings.get(i).getId());
            ratings.get(i).setDataUtilitiesArrays();
            ratings.get(i).getUserRequirements().getFunctionalRequirements().setContent();
            ratingsRepository.save(ratings.get(i));
        }
        return ratingsRepository.count();
    }

    @DeleteMapping("/ratings/{id}")
    public long deleteRating(@PathVariable String id) {
        ratingsRepository.deleteById(id);
        return ratingsRepository.count() - 1;
    }

    @DeleteMapping("/ratings")
    public long deleteAllRatings() {
        ratingsRepository.deleteAll();
        return ratingsRepository.count();
    }


    @PostMapping("/createBlueprints_Requirements_Ratings")
    public long createBlueprints_Requirements_Ratings() {

        Blueprint blueprint = new Blueprint();
        ArrayList<Blueprint> blueprints = new ArrayList<>();
        UserRequirements userRequirements;
        ArrayList<UserRequirements> userRequirementsList = new ArrayList<>();
        ArrayList<String> idsList = new ArrayList<>();

        String[] mTags = {"blood", "hdl", "ldl", "cholesterol", "bones", "eyes", "flesh"};
        String[] vTags = {"OSR", "patients", "doctors", "nurses", "human", "women", "men", "children", "grandparents"};
        Random random = new Random(22544);

        for (int j = 0; j < 1000; j++) {
            ArrayList<String> methodTags = new ArrayList<>();
            ArrayList<String> vdcTags = new ArrayList<>();
            String methodTagsString = "";
            String vdcTagsString = "";
            String content;

            int index;

            for (int i = 0; i < 1 + random.nextInt(7); i++) {
                index = random.nextInt(7);
                if (!methodTags.contains("\"" + mTags[index] + "\"")) {
                    methodTags.add("\"" + mTags[index] + "\"");
                    methodTagsString += mTags[index] + " ";
                }
            }

            for (int i = 0; i < 1 + random.nextInt(9); i++) {
                index = random.nextInt(9);
                if (!vdcTags.contains("\"" + vTags[index] + "\"")) {
                    vdcTags.add("\"" + vTags[index] + "\"");
                    vdcTagsString += vTags[index] + " ";
                }
            }

            content = methodTagsString + vdcTagsString;
            content = content.substring(0, content.length() - 1);

            //System.out.println(vdcTags);

            String funReqs = "\"functionalRequirements\": {\n" +
                    "\t\t\"methodTags\": " + methodTags + ",\n" +
                    "\t\t\"vdcTags\": " + vdcTags + "\n" +
                    "\t}";

            //String[] methodTagsArray = methodTags.toArray(new String[methodTags.size()]);
            //String[] vdcTagsArray = vdcTags.toArray(new String[vdcTags.size()]);

            String atts = "\"attributes\": {\n" +
                    "\t\t\"dataUtility\": [\n";
            if (random.nextInt(3) > 0)
                atts +=
                        "\t\t\t{\n" +
                                "\t\t\t\t\"id\": \"volume\",\n" +
                                "\t\t\t\t\"description\": \"volume\",\n" +
                                "\t\t\t\t\"type\": \"Volume\",\n" +
                                "\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\"volume\": {\n" +
                                "\t\t\t\t\t\t\"minimum\": " + (4000 + random.nextInt(6000)) + ",\n" +
                                "\t\t\t\t\t\t\"unit\": \"tuple\"\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t}\n" +
                                "\t\t\t},";
            if (random.nextInt(3) > 0)
                atts +=
                        "\n\t\t\t{\n" +
                                "\t\t\t\t\"id\": \"accuracy\",\n" +
                                "\t\t\t\t\"description\": \"Accuracy\",\n" +
                                "\t\t\t\t\"type\": \"Accuracy\",\n" +
                                "\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\"accuracy\": {\n" +
                                "\t\t\t\t\t\t\"minimum\": " + (0.5 + random.nextDouble() * (1 - 0.5)) + ",\n" +
                                "\t\t\t\t\t\t\"unit\": \"none\"\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t}\n" +
                                "\t\t\t},";
            if (random.nextInt(3) > 0)
                atts +=
                        "\n\t\t\t{\n" +
                                "\t\t\t\t\"id\": \"processCompleteness\",\n" +
                                "\t\t\t\t\"description\": \"Process completeness\",\n" +
                                "\t\t\t\t\"type\": \"Process completeness\",\n" +
                                "\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\"completeness\": {\n" +
                                "\t\t\t\t\t\t\"minimum\": " + (3 + random.nextInt(12)) + ",\n" +
                                "\t\t\t\t\t\t\"unit\": \"percentage\"\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t}\n" +
                                "\t\t\t},";
            if (random.nextInt(3) > 0)
                atts +=
                        "\n\t\t\t{\n" +
                                "\t\t\t\t\"id\": \"scaleUpMemory\",\n" +
                                "\t\t\t\t\"description\": \"scale-up memory\",\n" +
                                "\t\t\t\t\"type\": \"Scale-up\",\n" +
                                "\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\"ramGain\": {\n" +
                                "\t\t\t\t\t\t\"unit\": \"percentage\",\n" +
                                "\t\t\t\t\t\t\"value\": " + (100 + random.nextInt(100)) + "\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t}\n" +
                                "\t\t\t},";
            if (random.nextInt(3) > 0)
                atts +=
                        "\n\t\t\t{\n" +
                                "\t\t\t\t\"id\": \"scaleUpSpace\",\n" +
                                "\t\t\t\t\"description\": \"scale-up space\",\n" +
                                "\t\t\t\t\"type\": \"Scale-up\",\n" +
                                "\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\"spaceGain\": {\n" +
                                "\t\t\t\t\t\t\"unit\": \"percentage\",\n" +
                                "\t\t\t\t\t\t\"value\": " + (100 + random.nextInt(100)) + "\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t}\n" +
                                "\t\t\t},";
            if (random.nextInt(3) > 0)
                atts +=
                        "\n\t\t\t\t{\n" +
                                "\t\t\t\t\t\"id\": \"availability\",\n" +
                                "\t\t\t\t\t\"description\": \"availability\",\n" +
                                "\t\t\t\t\t\"type\": \"Availability\",\n" +
                                "\t\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\t\"availability\": {\n" +
                                "\t\t\t\t\t\t\t\"unit\": \"percentage\",\n" +
                                "\t\t\t\t\t\t\t\"minimum\": " + (80 + random.nextInt(20) + random.nextDouble()) + "\n" +
                                "\t\t\t\t\t\t}\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t},";
            if (random.nextInt(3) > 0)
                atts +=
                        "\n\t\t\t\t{\n" +
                                "\t\t\t\t\t\"id\": \"averageResponseTime\",\n" +
                                "\t\t\t\t\t\"description\": \"averageResponseTime\",\n" +
                                "\t\t\t\t\t\"type\": \"averageResponseTime\",\n" +
                                "\t\t\t\t\t\"properties\": {\n" +
                                "\t\t\t\t\t\t\"averageResponseTime\": {\n" +
                                "\t\t\t\t\t\t\t\"unit\": \"none\",\n" +
                                "\t\t\t\t\t\t\t\"maximum\": " + (1 + random.nextInt(4) + random.nextDouble()) + "\n" +
                                "\t\t\t\t\t\t}\n" +
                                "\t\t\t\t\t}\n" +
                                "\t\t\t\t},";

            atts = atts.substring(0, atts.length() - 1);

            atts +=
                    "\n\t\t]\n" +
                            "\t}";


            String inputJson = "{\n" + funReqs + "," + atts + "\n}";

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try {
                userRequirements = mapper.readValue(inputJson, UserRequirements.class);
                String userId = "";
                //for (int i = 0; i < 15; i++) {
                //    userId += allCharacters.charAt(random.nextInt(allCharacters.length()));
                //}


                userId = "" + random.nextInt(5000);

                userRequirements.getFunctionalRequirements().setContent(content);
                //System.out.println("----------------------------------------------------------------");
                //System.out.println("USER ID : " + userId);
                //System.out.println(inputJson);
                //System.out.println();
                userRequirementsList.add(userRequirements);
                idsList.add(userId);
/*
                for (int i = 0; i < userRequirements.getFunctionalRequirements().getVdcTags().length; i++) {
                    System.out.println("++++" + userRequirements.getFunctionalRequirements().getVdcTags()[i]);
                }
*/

            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        for (int i = 0; i < 20; i++) {

            String blueprintId = "";

            blueprintId += (i);
            blueprint.setId(blueprintId);

            blueprint.setVolume(4000 + random.nextInt(6000));
            blueprint.setAccuracy(0.5 + random.nextDouble() * (1 - 0.5));
            blueprint.setCompleteness(3 + random.nextInt(12));
            blueprint.setSpaceGain(100 + random.nextInt(100));
            blueprint.setRamGain(100 + random.nextInt(100));
            blueprint.setAvailability(80 + random.nextInt(20) + random.nextDouble());
            blueprint.setAverageResponseTime(1 + random.nextInt(4) + random.nextDouble());
/*
            System.out.println("id: " + blueprint.getId());
            System.out.println("volume: " + blueprint.getVolume());
            System.out.println("acc: " + blueprint.getAccuracy());
            System.out.println("comp: " + blueprint.getCompleteness());
            System.out.println("ram: " + blueprint.getRamGain());
            System.out.println("space: " + blueprint.getSpaceGain());
            System.out.println("ava: " + blueprint.getAvailability());
            System.out.println("resp: " + blueprint.getAverageResponseTime());
*/
            blueprint.setNormalizedUtilities();
            ArrayList<Double> commonOfBlueprint = new ArrayList<>();


            for (int j = 0; j < userRequirementsList.size(); j++) {
                boolean boo = false;
                UserRequirements requirements = userRequirementsList.get(j);
                ArrayList<String> utilities = new ArrayList<>();
                ArrayList<Double> normalized = new ArrayList<>();
                for (int k = 0; k < requirements.getAttributes().getDataUtility().length; k++) {
                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getVolume() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getVolume().getMinimum()
                                < blueprint.getVolume()) {
                            commonOfBlueprint.add(blueprint.getNormalizedVolume());
                            //System.out.println(idsList.get(j) + " volume: " + requirements.getAttributes().getDataUtility()[k].getProperties().getVolume().getMinimum() );
                        } else boo = true;
                    }

                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getAccuracy() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getAccuracy().getMinimum()
                                < blueprint.getAccuracy()) {
                            commonOfBlueprint.add(blueprint.getNormalizedAccuracy());
                            //System.out.println(idsList.get(j) + " accuracy: " + requirements.getAttributes().getDataUtility()[k].getProperties().getAccuracy().getMinimum() );
                        } else boo = true;
                    }

                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getAvailability() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getAvailability().getMinimum()
                                < blueprint.getAvailability()) {
                            commonOfBlueprint.add(blueprint.getNormalizedAvailability());
                            //System.out.println(idsList.get(j) + " availability: " + requirements.getAttributes().getDataUtility()[k].getProperties().getAvailability().getMinimum() );
                        } else boo = true;
                    }

                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getCompleteness() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getCompleteness().getMinimum()
                                < blueprint.getCompleteness()) {
                            commonOfBlueprint.add(blueprint.getNormalizedCompleteness());
                            //System.out.println(idsList.get(j) + " completeness: " + requirements.getAttributes().getDataUtility()[k].getProperties().getCompleteness().getMinimum() );
                        } else boo = true;
                    }

                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getRamGain() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getRamGain().getValue()
                                > blueprint.getRamGain()) {
                            commonOfBlueprint.add(blueprint.getNormalizedRamGain());

                            //System.out.println(idsList.get(j) + " ramGain: " + requirements.getAttributes().getDataUtility()[k].getProperties().getRamGain().getValue() );
                        } else boo = true;
                    }

                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getSpaceGain() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getSpaceGain().getValue()
                                < blueprint.getSpaceGain()) {
                            commonOfBlueprint.add(blueprint.getNormalizedSpaceGain());

                            //System.out.println(idsList.get(j) + " spaceGain: " + requirements.getAttributes().getDataUtility()[k].getProperties().getSpaceGain().getValue() );
                        } else boo = true;
                    }

                    if (requirements.getAttributes().getDataUtility()[k].getProperties().getAverageResponseTime() != null) {
                        if (requirements.getAttributes().getDataUtility()[k].getProperties().getAverageResponseTime().getMaximum()
                                > blueprint.getAverageResponseTime()) {
                            commonOfBlueprint.add(blueprint.getNormalizedAverageResponseTime());

                            //System.out.println(idsList.get(j) + " respTime: " + requirements.getAttributes().getDataUtility()[k].getProperties().getAverageResponseTime().getMaximum() );
                        } else boo = true;
                    }

                }
                if (!boo) {
                    //System.out.println(idsList.get(j));

                    String ratingId = "";
                    //for (int p = 0; p < 15; p++) {
                    //    ratingId += allCharacters.charAt(random.nextInt(allCharacters.length()));
                    //}

                    Rating rating = new Rating();

                    if (requirements.getAttributes().getDataUtility() != null) {
                        ratingId += (100 + random.nextInt(5000));

                        rating.setId(ratingId);
                        //System.out.println("ratingId: " + rating.getId());
                        rating.setBlueprintId(blueprint.getId());
                        //System.out.println("blueprintId: " + rating.getBlueprintId());
                        rating.setUserId(idsList.get(j));
                        //System.out.println("userId: " + rating.getUserId());
                        rating.setUserRequirements(requirements);
                        rating.setDataUtilitiesArrays();

                        //System.out.println(rating.getUserRequirements().getAttributes().getDataUtility()[0].getId());

                        //System.out.println("Normalized : " + rating.getNormalizedDataUtilities().toString());
                        //System.out.println("Data Utilities String : " + rating.getDataUtilitiesStringArray().toString());


                        //double dist = 0;
                        for (int ii = 0; ii < rating.getNormalizedDataUtilities().size(); ii++) {
                            //System.out.println("//////////// " + rating.getNormalizedDataUtilities().get(ii));
                            //System.out.println("???????????? " + commonOfBlueprint.get(ii));
                        }
                        double dist = manhattanDistance(rating.getNormalizedDataUtilities(), commonOfBlueprint);
                        //System.out.println(".............." + rating.getCommon().toString());
                        //System.out.println("ManhattanDistance : " + dist);
                        //System.out.println("Common Blueprint size : " +commonOfBlueprint.size());

                        if (dist <= rating.getNormalizedDataUtilities().size() * 0.15)
                            rating.setRating(4 + random.nextInt(2));
                        //else if (dist > 0.25 * rating.getCommon().size())
                        //    rating.setRating(2 + random.nextInt(3));
                        if ((dist > rating.getNormalizedDataUtilities().size() * 0.15) && (dist <= rating.getNormalizedDataUtilities().size() * 0.3))
                            rating.setRating(3 + random.nextInt(2));
                        if (dist > rating.getNormalizedDataUtilities().size() * 0.3)
                            rating.setRating(1 + random.nextInt(3));


                        //rating.setRating(1 + random.nextInt(5));
                        //System.out.println("Rating : " + rating.getRating());

                        //System.out.println(rating.getUserRequirements().getFunctionalRequirements().getMethodTags()[0]);

                        //rating.getUserRequirements().getFunctionalRequirements().setContent();
                        //System.out.println("!!!!!!!!!!!!!!!!!" + rating.getUserRequirements().getFunctionalRequirements().getContent());
                        ratingsRepository.save(rating);

                    }
                }
            }


            //System.out.println("=================================================================");
        }

        //System.out.println(blueprintString);


        return ratingsRepository.count();
    }


    @GetMapping("/elasticsearchScores")
    public List<Pair<String, Double>> getElasticsearchScores(@RequestBody UserRequirements userRequirements) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        ArrayList<Pair<String, Double>> blueprintElasticScores = new ArrayList<>();
        //System.out.println(userRequirements.getAttributes().getDataUtility()[0].getId());

        userRequirements.getFunctionalRequirements().setContent();
        ArrayList<String> blueprintsIds = setBlueprintIdsArray(ratingsRepository);


        for (int bid = 0; bid < blueprintsIds.size(); bid++) {
/*
            String decayQuery = "{\n" +
                    "\t\"function_score\" : {\n" +
                    "\t\t\"query\" : {\n" +
                    "\t\t\t\"filtered\" : {\n" +
                    "\t\t\t\t\"filter\" : {\n" +
                    "\t\t\t\t\t\"term\" : {\n" +
                    "\t\t\t\t\t\t\"blueprintId\" : \"" + blueprintsIds.get(bid) + "\"\n" +
                    "\t\t\t\t\t}\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t},\n" +
                    "\t\t\"functions\" : [\n";
*/

            String decayQuery = "{\n" +
                    "\t\"function_score\" : {\n" +
                    "\t\t\"query\" : {\n" +
                    "\t\t\t\"filtered\" : {\n" +
                    "\t\t\t\t\"query\" : {\n" +
                    "\t\t\t\t\t\"bool\" : {\n" +
                    "\t\t\t\t\t\t\"must\" : [\n" +
                    "\t\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\t\"match\" : {\n" +
                    "\t\t\t\t\t\t\t\t\t\"userRequirements.functionalRequirements.content\" : \""
                    + userRequirements.getFunctionalRequirements().getContent() + "\"\n" +
                    "\t\t\t\t\t\t\t\t}\n" +
                    "\t\t\t\t\t\t\t}\n" +
                    "\t\t\t\t\t\t]\n" +
                    "\t\t\t\t\t}\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"filter\" : {\n" +
                    "\t\t\t\t\t\"term\" : {\n" +
                    "\t\t\t\t\t\t\"blueprintId\" : \"" + blueprintsIds.get(bid) + "\"\n" +
                    "\t\t\t\t\t}\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t},\n" +
                    "\t\t\"functions\" : [{\n" +
                    "    \"script_score\" : { \n" +
                    "\t\t\t\t\t\"script\" : \"(maxScore * (1 - 1/(1 * _score)));\",\n" +
                    "        \"lang\" : \"javascript\",\n" +
                    "        \"params\" : {\n" +
                    "        \t\"maxScore\" : 1 \n" +
                    "        }\n" +
                    "    }\n" +
                    "},\n";

            //if exist then string += gauss
            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAccuracy() != null)
                    decayQuery +=
                            "{\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +
                                    "            \"userRequirements.attributes.dataUtility.properties.accuracy.minimum\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getAccuracy().getMinimum() + "\",\n" +
                                    //"              \"scale\": \"0.50\"\n " +
                                    //"              \"scale\": \"0.25\"\n " +
                                    "              \"scale\": \"0.1\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        }, \n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.accuracy.minimum\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAvailability() != null)
                    decayQuery +=
                            "   {\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +

                                    "            \"userRequirements.attributes.dataUtility.properties.availability.minimum\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getAvailability().getMinimum() + "\",\n" +
                                    //"              \"scale\": \"50\"\n" +
                                    //"              \"scale\": \"25\"\n" +
                                    "              \"scale\": \"10\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        },\n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.availability.minimum\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getVolume() != null)
                    decayQuery +=
                            "   {\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +
                                    "            \"userRequirements.attributes.dataUtility.properties.volume.minimum\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getVolume().getMinimum() + "\",\n" +
                                    //"              \"scale\": \"5000\"\n" +
                                    //"              \"scale\": \"2500\"\n" +
                                    "              \"scale\": \"1000\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        },\n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.volume.minimum\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getCompleteness() != null)
                    decayQuery +=
                            "   {\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +
                                    "            \"userRequirements.attributes.dataUtility.properties.completeness.minimum\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getCompleteness().getMinimum() + "\",\n" +
                                    //"              \"scale\": \"6\"\n" +
                                    //"              \"scale\": \"4.5\"\n" +
                                    "              \"scale\": \"4\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        },\n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.completeness.minimum\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getRamGain() != null)
                    decayQuery +=
                            "   {\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +
                                    "            \"userRequirements.attributes.dataUtility.properties.ramGain.value\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getRamGain().getValue() + "\",\n" +
                                    //"              \"scale\": \"50\"\n" +
                                    //"              \"scale\": \"25\"\n" +
                                    "              \"scale\": \"10\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        },\n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.ramGain.value\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getSpaceGain() != null)
                    decayQuery +=
                            "   {\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +
                                    "            \"userRequirements.attributes.dataUtility.properties.spaceGain.value\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getSpaceGain().getValue() + "\",\n" +
                                    //"              \"scale\": \"50\"\n" +
                                    //"              \"scale\": \"25\"\n" +
                                    "              \"scale\": \"10\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        },\n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.spaceGain.value\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            for (int i = 0; i < userRequirements.getAttributes().getDataUtility().length; i++)
                if (userRequirements.getAttributes().getDataUtility()[i].getProperties().getAverageResponseTime() != null)
                    decayQuery +=
                            "   {\n" +
                                    //"          \"exp\": {\n" +
                                    "          \"gauss\": {\n" +
                                    //"          \"linear\": {\n" +
                                    "            \"userRequirements.attributes.dataUtility.properties.averageResponseTime.maximum\": {\n" +
                                    "              \"origin\": \"" + userRequirements.getAttributes().getDataUtility()[i].getProperties().getAverageResponseTime().getMaximum() + "\",\n" +
                                    //"              \"scale\": \"3\"\n" +
                                    //"              \"scale\": \"4\"\n" +
                                    "              \"scale\": \"4.5\"\n " +

                                    "            }\n" +
                                    "          }\n" +
                                    "        },\n";

                else decayQuery += "{\n" +
                        "        \"filter\": {\n" +
                        "          \"missing\": {\n" +
                        "            \"field\": \"userRequirements.attributes.dataUtility.properties.averageResponseTime.maximum\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"boost_factor\": 0\n" +
                        "      },\n";


            decayQuery = decayQuery.substring(0, decayQuery.length() - 2);

            decayQuery +=
                    "\t\t], " +
                            "      \"max_boost\" : \"1\", \"score_mode\": \"avg\", \"boost_mode\" : \"replace\" \n\n" +
                            "\t}\n" +
                            "}\n";

            try {
                Client client = config.client();

                SearchResponse response = client.prepareSearch()
                        .setQuery(decayQuery).setMinScore(0).setSize(500)
                        .execute().actionGet();

                double sum = 0.0;
                double sumOfWeights = 0.0;
                //System.out.println(response.toString());

                for (SearchHit hit : response.getHits()) {
                    //System.out.println(hit.getScore());
                    double similarity = hit.getScore();
                    Map<String, Object> source = hit.getSource();
                    int rating = (int) source.get("rating");
                    //System.out.println("rating : " + rating);
                    sum += rating * similarity;
                    sumOfWeights += similarity;
                }

                double blueprintScore = sum / sumOfWeights;
                //System.out.println("Score for Blueprint " + blueprintsIds.get(bid) + " = " + blueprintScore);
                Pair<String, Double> pair = new Pair<>(blueprintsIds.get(bid), blueprintScore);
                blueprintElasticScores.add(pair);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        //Collections.sort(blueprintElasticScores, Comparator.comparing(p -> -p.getValue()));

        stopwatch.stop();
        //System.out.println("that took: " + stopwatch); // formatted string like "12.3 ms"


        return blueprintElasticScores;
    }


    public static double cosineSimilarity(ArrayList<Double> vectorA, ArrayList<Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        return (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public static double euclideanDistance(ArrayList<Double> vectorA, ArrayList<Double> vectorB) {
        double sum = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            sum += Math.pow(vectorA.get(i) - vectorB.get(i), 2);
        }

        return Math.sqrt(sum);
    }

    public static double euclideanSimilarity(ArrayList<Double> vectorA, ArrayList<Double> vectorB) {
        double euclideanDistance = euclideanDistance(vectorA, vectorB);

        //return 1/Math.exp(euclideanDistance); KERNEL ALMOST SAME RESULTS BUT GREATER ERROR
        return 1 / (1 + euclideanDistance);
    }

    public static double euclideanSimilaritySquared(ArrayList<Double> vectorA, ArrayList<Double> vectorB) {
        double euclideanDistance = euclideanDistance(vectorA, vectorB);

        return 1 / (1 + Math.sqrt(euclideanDistance));
    }

    public static double manhattanDistance(ArrayList<Double> vectorA, ArrayList<Double> vectorB) {
        double sum = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            sum += Math.abs(vectorA.get(i) - vectorB.get(i));
        }

        return sum;
    }

    public static double manhattanSimilarity(ArrayList<Double> vectorA, ArrayList<Double> vectorB) {
        double manhattanDistance = manhattanDistance(vectorA, vectorB);
        double similarity = 1 - (manhattanDistance / vectorA.size());

        return similarity;
    }

    public static ArrayList<String> setBlueprintIdsArray(RatingsRepository ratingsRepository) {
        ArrayList<String> blueprintIdsArray = new ArrayList<>();

        Iterable<Rating> allRatings = ratingsRepository.findAll();
        for (Rating rating : allRatings) {
            String blueprintId = rating.getBlueprintId();
            //System.out.println(blueprintId);
            if (!blueprintIdsArray.contains(blueprintId)) {
                blueprintIdsArray.add(blueprintId);
            }
        }

        return blueprintIdsArray;
    }

    public double normalizationFunction(double utility, double min, double max) {
        return (utility - min) / (max - min);
    }


    @GetMapping("/validation")
    public long validation() {
        deleteAllRatings();
        //System.out.println("----------------" + ratingsRepository.count());
        createBlueprints_Requirements_Ratings();

        List<Rating> allRatings = Lists.newArrayList(ratingsRepository.findAll());
        System.out.println("id,ratingId,userId,BlueprintId,userRating,recsystemScore,ElasticScore");
        //System.out.println("id,ratingId,userId,BlueprintId,userRating,ElasticScore");

        Collections.sort(allRatings, Comparator.comparing(Rating::getId));

        for (int i = 0; i < allRatings.size(); i++) {
            Rating ra = allRatings.get(i);
            Double recsystemScore = 0.0, elasticScore = 0.0;


            List<Pair<String, Double>> blueprintScores = getScoreByURs(ra.getUserRequirements());
            for (int j = 0; j < blueprintScores.size(); j++) {
                if (blueprintScores.get(j).getKey().equals(ra.getBlueprintId())) {
                    //System.out.println("BlueprintID : " + blueprintScores.get(j).getKey() + " with Score : " + blueprintScores.get(j).getValue());
                    recsystemScore = blueprintScores.get(j).getValue();
                }
            }

            List<Pair<String, Double>> blueprintElasticScores = getElasticsearchScores(ra.getUserRequirements());
            for (int j = 0; j < blueprintElasticScores.size(); j++) {
                if (blueprintElasticScores.get(j).getKey().equals(ra.getBlueprintId())) {
                    //System.out.println("BlueprintID : " + blueprintElasticScores.get(j).getKey() + " with ElasticScore : " + blueprintElasticScores.get(j).getValue());
                    elasticScore = blueprintElasticScores.get(j).getValue();

                }
            }

            System.out.println((i) + "," + ra.getId() + "," + ra.getUserId() + "," + ra.getBlueprintId() +
                    "," + ra.getRating() + "," + recsystemScore + "," + elasticScore);


/*
            System.out.println(i + "," + ra.getId() + ","+ra.getUserId() + "," + ra.getBlueprintId() +
                    "," + ra.getRating() + "," + elasticScore );

*/

        }

        return 50;
    }


    @GetMapping("/kfold_validation")
    public long kfold_validation() {
        deleteAllRatings();
        //System.out.println("----------------" + ratingsRepository.count());
        createBlueprints_Requirements_Ratings();

        List<Rating> allRatings = Lists.newArrayList(ratingsRepository.findAll());
        System.out.println("id,ratingId,userId,BlueprintId,userRating,recsystemScore,ElasticScore");
        //System.out.println("id,ratingId,userId,BlueprintId,userRating,ElasticScore");

        Collections.sort(allRatings, Comparator.comparing(Rating::getId));
        int kfoldsize = Math.round(allRatings.size() / 10) + 1;
        System.out.println(kfoldsize);
        int counter = 0;

        for (int kfold = 0; kfold < 10; kfold++) {

            List<Rating> folds = new ArrayList<>();

            for (int kk = 0; kk < kfoldsize; kk++) {
                counter = kfold * kfoldsize + kk;
                if (counter < allRatings.size()) {
                    folds.add(allRatings.get(counter));
                    //System.out.println(allRatings.get(counter).getId() + "====");
                    ratingsRepository.deleteById(allRatings.get(counter).getId());
                } else break;

            }


            for (int i = 0; i < folds.size(); i++) {
                Rating ra = folds.get(i);
                Double recsystemScore = 0.0, elasticScore = 0.0;


                List<Pair<String, Double>> blueprintScores = getScoreByURs(ra.getUserRequirements());
                for (int j = 0; j < blueprintScores.size(); j++) {
                    if (blueprintScores.get(j).getKey().equals(ra.getBlueprintId())) {
                        //System.out.println("BlueprintID : " + blueprintScores.get(j).getKey() + " with Score : " + blueprintScores.get(j).getValue());
                        recsystemScore = blueprintScores.get(j).getValue();
                    }
                }


                List<Pair<String, Double>> blueprintElasticScores = getElasticsearchScores(ra.getUserRequirements());
                for (int j = 0; j < blueprintElasticScores.size(); j++) {
                    if (blueprintElasticScores.get(j).getKey().equals(ra.getBlueprintId())) {
                        //System.out.println("BlueprintID : " + blueprintElasticScores.get(j).getKey() + " with ElasticScore : " + blueprintElasticScores.get(j).getValue());
                        elasticScore = blueprintElasticScores.get(j).getValue();

                    }
                }


                System.out.println((kfold * kfoldsize + i) + "," + ra.getId() + "," + ra.getUserId() + "," + ra.getBlueprintId() +
                        "," + ra.getRating() + "," + recsystemScore + "," + elasticScore);


/*
            System.out.println(i + "," + ra.getId() + ","+ra.getUserId() + "," + ra.getBlueprintId() +
                    "," + ra.getRating() + "," + elasticScore );

*/

            }

            deleteAllRatings();
            //System.out.println("----------------" + ratingsRepository.count());
            createBlueprints_Requirements_Ratings();
            System.out.println("================" + ratingsRepository.count());


        }

        return 50;
    }

}