package com.example.demo.client;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;


import org.jetbrains.annotations.NotNull;



import java.io.FileWriter;

import java.io.OutputStream;
import java.net.HttpURLConnection;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Executors;


public class SkierClient {

	private static final int MAX_SKIER_ID = 100000;
	private static final int MAX_RESORT_ID = 10;
	private static final int MAX_LIFT_ID = 40;
	private static final int SEASON_ID = 2022;
	private static final int DAY_ID = 1;
	private static final int MAX_TIME = 360;
	private static final int NUM_THREADS = 32;
	private static final int REQ_PER_THREAD = 1000;
	private static final int NUM_REQUESTS = 10000;
	private static final Random RANDOM = new Random();
	static long successfulRequests = 0;
	private static List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
	private static long minLatency = Long.MAX_VALUE;
	private static long maxLatency = Long.MIN_VALUE;

	static int count = NUM_REQUESTS;
	static long latency_sum = 0;
	private static int failedRequests;


	public static void main(String[] args) {
		String uri = "mongodb+srv://kausthuba:Mjkkrv96@cluster0.xw9jdgl.mongodb.net/Dss_project?retryWrites=true&w=majority";
		ConnectionString conns =  new ConnectionString(uri);
		MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(conns)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("Dss_project");
        database.getCollection("stats");   // createCollection("stats");
        
       
		

		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
		long start = System.currentTimeMillis();
		for (int i = 0; i < NUM_THREADS; i++) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						failedRequests = 0;
						for (int j = 1; j <= REQ_PER_THREAD; j++) {

							synchronized (this) {
								if (count - NUM_THREADS>= 0) {
									String requestBody = generateRequestBody();
									sendPostRequest(requestBody);
									System.out.println("count :" + count);
									count--;
									if (count == 0) {
										Collections.sort(latencies);
										System.out.println(latencies + " " + latencies.size());
										System.out.println("Successful Requests:" + successfulRequests);
										System.out.println("Failed Requests:" + failedRequests);
										long end = System.currentTimeMillis();
										long total_time = end - start;
										System.out.println("Total Time:" + total_time);
										double meanResponseTime = (double) latency_sum / NUM_REQUESTS;
										double medianResponseTime = calculateMedian(latencies);
										double throughput = (double) (successfulRequests + failedRequests)
												/ ((double) total_time / 1000);
										double p99ResponseTime = calculatePercentile(latencies, 0.99);
										for (long latency : latencies) {
											maxLatency = Math.max(maxLatency, latency);
											minLatency = Math.min(minLatency, latency);
										}
										System.out.println("Mean Response Time: " + meanResponseTime + " ms");
										System.out.println("Median Response Time: " + medianResponseTime + " ms");
										System.out.println("Throughput: " + throughput + " requests/second");
										System.out.println("99th Percentile Response Time: " + p99ResponseTime + " ms");
										System.out.println("Min Response Time: " + minLatency + " ms");
										System.out.println("Max Response Time: " + maxLatency + " ms");
										
										
										Document document = new Document();
										document.append("mean",meanResponseTime);
										document.append("median", medianResponseTime);
										document.append("throughput", throughput);
										document.append("nintyNinthPercentile", p99ResponseTime);
										document.append("minResponseTime",minLatency);
										document.append("maxResponseTime", maxLatency);
										database.getCollection("stats").insertOne(document);
										

									}

								}
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				
			});

		}
		executor.shutdown();
	}

	// calculate Median
	private static double calculateMedian(List<Long> latencies) {
		int size = latencies.size();
		if (size % 2 == 0) {
			return (latencies.get(size / 2 - 1) + latencies.get(size / 2)) / 2.0;
		} else {
			return latencies.get(size / 2);
		}
	}

	// calculate Percentile
	private static double calculatePercentile(List<Long> latencies, double percentile) {
		int index = (int) Math.ceil(percentile * latencies.size());
		return latencies.get(index - 1);
	}

	private static synchronized void sendPostRequest(String requestBody) throws Exception {
		long startTime = System.currentTimeMillis(); // take a timestamp before sending the POST
		int skierID = RANDOM.nextInt(MAX_SKIER_ID) + 1;
		int resortID = RANDOM.nextInt(MAX_RESORT_ID) + 1;

		String base_url = "http://localhost:8080/api/skiers/" + String.valueOf(resortID) + "/seasons/"
				+ String.valueOf(SEASON_ID) + "/days/" + String.valueOf(DAY_ID) + "/skiers/" + String.valueOf(skierID);
		URL url = new URL(base_url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");

		con.setDoOutput(true);

		OutputStream os = con.getOutputStream();
		os.write(requestBody.getBytes());

		os.flush();
		os.close();
		
		//Handling Error
		int responseCode = con.getResponseCode();
		System.out.println(responseCode);
		if(responseCode >=400 && responseCode <=599) {
			int count = 1;
			while(count<=5) {
				failedRequests++;
				System.out.print("The request was not sucessful. Attempts made:" + count);
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/json");

				con.setDoOutput(true);

				OutputStream output = con.getOutputStream();
				output.write(requestBody.getBytes());
				System.out.println("request body" + requestBody);
				output.flush();
				output.close();
				responseCode = con.getResponseCode();
				if(responseCode == 201) {
					break;
				}
				else {
					count++;
				}
			}

		}
		long endTime = System.currentTimeMillis(); // take another timestamp when the response is received
		long latency = endTime - startTime; // calculate the latency
		latencies.add(latency);
		latency_sum += latency;
		// Construct the record string
		String record = String.format("%s,POST,%d,%d", startTime, latency, responseCode);
		FileWriter writer = new FileWriter("records.csv", true);
		writer.write(record + "\n");
		writer.close();

		if (responseCode != 201) {
			throw new Exception("Failed to send POST request with response code: " + responseCode);
		} else {
			System.out.println("Executed post");
			successfulRequests++;
		}
	}

	private static synchronized @NotNull String generateRequestBody() {

		int liftID = RANDOM.nextInt(MAX_LIFT_ID) + 1;
		int time = RANDOM.nextInt(MAX_TIME) + 1;
		StringBuilder sb = new StringBuilder();
		sb.append("{").append("\"liftID\":").append(String.valueOf(liftID)).append(",").append("\"time\":")
				.append(String.valueOf(time)).append("}");
		return sb.toString();
	}

}
