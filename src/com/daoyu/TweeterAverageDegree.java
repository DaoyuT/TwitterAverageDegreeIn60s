package com.daoyu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class TweeterAverageDegree {
	public static final String TWITTER_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
	public static final String INPUT_FILE_NAME = "/tweet_input/tweets.txt";
	public static final String OUTPUT_FILE_NAME = "/tweet_output/output.txt";
	public static final long ONE_MINUTE_IN_MILLIS = 60000;
	
	public static String dir;
	public static JSONParser parser;
	public static File inputFile;
	public static File outputFile;
	public static SimpleDateFormat simpleDateFormat;
	public static DecimalFormat outputFormatter;
	
	public static long currTime;
	public static long oneMinAgo;
	public static long numOfNodes = 0;
	public static long sumOfDegrees = 0;
	public static double averageDegree = 0;
	public static Queue<Tweet> tweetQueue;
	public static Map<String, Node> graph;
	
	public static void main(String[] args) {
		TweeterAverageDegree tweeterAverageDegree = new TweeterAverageDegree();
		tweeterAverageDegree.start();
	}
	
	public TweeterAverageDegree(){
		dir = System.getProperty("user.dir");
		parser = new JSONParser();
		inputFile = new File(dir + INPUT_FILE_NAME);
		outputFile = new File(dir + OUTPUT_FILE_NAME);
		simpleDateFormat = new SimpleDateFormat(TWITTER_DATE_FORMAT, Locale.ENGLISH);
		tweetQueue = new PriorityQueue<Tweet>(new Comparator<Tweet>() {
		  public int compare(Tweet t1, Tweet t2) {
		    if (t1.created_at < t2.created_at) {
		      return -1;
		    } else if (t1.created_at > t2.created_at) {
		      return 1;
		    } else {
		      return 0;
		    }
		  }
    });
		graph = new HashMap<String, Node>();
		outputFormatter = new DecimalFormat("0.00");
	}
	
	@SuppressWarnings({ "unchecked" })
	public void start(){
		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(inputFile));
			bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
			boolean isFirstTweet = true;
			int skippedTweets = 0;
			for(String line; (line = bufferedReader.readLine()) != null; ) {
				JSONObject tweetJson = (JSONObject) parser.parse(line);
				String created_at = (String) tweetJson.get("created_at");
				if ("".equals(created_at) || created_at == null) {
					// it is not a tweet
				  skippedTweets++;
					continue;
				}
				Date currTweetDate = simpleDateFormat.parse(created_at);
				long currTweetTime= currTweetDate.getTime();
				if (isFirstTweet) {
					currTime = currTweetTime;
					oneMinAgo = currTweetTime - ONE_MINUTE_IN_MILLIS;
          isFirstTweet = false;
				} else {
	        bufferedWriter.newLine();
				}
				if (currTweetTime >= oneMinAgo) {
					// current tweet is not outdated
				  if (currTweetTime > currTime) {
	          // current tweet created the newest time
				    currTime = currTweetTime;
	          oneMinAgo = currTweetTime - ONE_MINUTE_IN_MILLIS;
	          updateGraph();
				  }
					// current tweet is within the 60s window
					JSONObject entities = (JSONObject) tweetJson.get("entities");
					JSONArray hashtagList = (JSONArray) entities.get("hashtags");
					if (hashtagList.size() >= 2) {
						Iterator<JSONObject> hashtagIterator = hashtagList.iterator();
						List<String> list = new ArrayList<String>();
						while (hashtagIterator.hasNext()) {
			        String hashtag = (String) hashtagIterator.next().get("text");
			        if (!list.contains(hashtag)) {
	              list.add(hashtag);
			        }
		        }
						if (list.size() >= 2) {
	            Tweet tweet = new Tweet(currTweetTime, list);
	            addEdges(tweet);
	            tweetQueue.offer(tweet);
						}
					}
	        if (numOfNodes != 0) {
	          averageDegree = 1.0 * sumOfDegrees / numOfNodes;
	        }
				}
        bufferedWriter.write(outputFormatter.format(averageDegree));
			}
			//System.out.println(skippedTweets);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
				bufferedReader.close();
				bufferedWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    }
	}
  
  private void updateGraph() {
    while (!tweetQueue.isEmpty() && tweetQueue.peek().created_at < oneMinAgo) {
      Tweet tweet = tweetQueue.poll();
      evictEdges(tweet);
    }
  }
	
	private void addEdges(Tweet tweet) {
		for (int i = 0; i < tweet.hashtagList.size() - 1; i++) {
		  for (int j = i + 1; j < tweet.hashtagList.size(); j++) {
		    String hashtag1 = tweet.hashtagList.get(i);
		    String hashtag2 = tweet.hashtagList.get(j);
		    Node node1 = null, node2 = null;
        if (graph.containsKey(hashtag1)) {
          node1 = graph.get(hashtag1);
        } else {
          node1 = new Node(hashtag1);
          graph.put(hashtag1, node1);
          numOfNodes++;
        }
        if (graph.containsKey(hashtag2)) {
          node2 = graph.get(hashtag2);
        } else {
          node2 = new Node(hashtag2);
          graph.put(hashtag2, node2);
          numOfNodes++;
        }
        if (!node1.neighbours.containsKey(node2.name) && !node2.neighbours.containsKey(node1.name)) {
          node1.neighbours.put(node2.name, 1);
          node2.neighbours.put(node1.name, 1);
          sumOfDegrees += 2;
        } else {
          node1.neighbours.put(node2.name, node1.neighbours.get(node2.name) + 1);
          node2.neighbours.put(node1.name, node2.neighbours.get(node1.name) + 1);
        }
		  }
		}
	}
	
  private void evictEdges(Tweet tweet) {
    for (int i = 0; i < tweet.hashtagList.size() - 1; i++) {
      for (int j = i + 1; j < tweet.hashtagList.size(); j++) {
        Node node1 = graph.get(tweet.hashtagList.get(i));
        Node node2 = graph.get(tweet.hashtagList.get(j));
        node1.neighbours.put(node2.name, node1.neighbours.get(node2.name) - 1);
        node2.neighbours.put(node1.name, node2.neighbours.get(node1.name) - 1);
        if (node1.neighbours.get(node2.name) == 0 && node2.neighbours.get(node1.name) == 0) {
          node1.neighbours.remove(node2.name);
          node2.neighbours.remove(node1.name);
          sumOfDegrees -= 2;
        }
        if (node1.neighbours.size() == 0) {
          graph.remove(node1.name);
          numOfNodes--;
        }
        if (node2.neighbours.size() == 0) {
          graph.remove(node2.name);
          numOfNodes--;
        }
      }
    }
  }
}
