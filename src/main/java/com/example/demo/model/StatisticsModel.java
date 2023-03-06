package com.example.demo.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document("stats")
public class StatisticsModel {
	double mean;
	double median;
	double throughput;
	double nintyNinthPercentile;
	double minResponseTime;
	double maxResponseTime;
	public double getMean() {
		return mean;
	}
	public void setMean(double mean) {
		this.mean = mean;
	}
	public double getMedian() {
		return median;
	}
	public void setMedian(double median) {
		this.median = median;
	}
	public double getThroughput() {
		return throughput;
	}
	public void setThroughput(double throughput) {
		this.throughput = throughput;
	}
	public double getNintyNinthPercentile() {
		return nintyNinthPercentile;
	}
	public void setNintyNinthPercentile(double nintyNinthPercentile) {
		this.nintyNinthPercentile = nintyNinthPercentile;
	}
	public double getMinResponseTime() {
		return minResponseTime;
	}
	public void setMinResponseTime(double minRersponseTime) {
		this.minResponseTime = minRersponseTime;
	}
	public double getMaxResponseTime() {
		return maxResponseTime;
	}
	public void setMaxResponseTime(double maxResponseTime) {
		this.maxResponseTime = maxResponseTime;
	}
	
	
	
}
