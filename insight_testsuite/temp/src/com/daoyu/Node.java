package com.daoyu;

import java.util.HashMap;
import java.util.Map;

public class Node {
	public String name;
	public Map<String, Integer> neighbours = new HashMap<String, Integer>();
	
	public Node(String _name) {
		name = _name;
	}
}
