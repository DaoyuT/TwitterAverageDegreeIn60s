package com.daoyu;

import java.util.ArrayList;
import java.util.List;

public class Tweet {
	
	public long created_at;
	public List<String> hashtagList;
	
	public Tweet(long _created_at, List<String> list){
		created_at = _created_at;
		hashtagList = new ArrayList<String>(list);
	}
}
