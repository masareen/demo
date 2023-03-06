package com.example.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.StatisticsModel;

@Repository
public interface StatisticsModelRepository extends MongoRepository<StatisticsModel, String>{

	
	
	
}
