package com.christouandr7.elasticsearchrecsystem.repository;
import com.christouandr7.elasticsearchrecsystem.models.Rating;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.ArrayList;

public interface RatingsRepository extends ElasticsearchRepository<Rating, String> {

    Rating findById(String id);

    ArrayList<Rating> findAllById(String id);

    ArrayList<Rating> findAllByBlueprintId(String blueprintId);


    void deleteById(String id);

}
