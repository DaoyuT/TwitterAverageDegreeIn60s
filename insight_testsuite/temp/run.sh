#!/bin/bash

javac -d ./bin -classpath ./libs/json-simple-1.1.1.jar ./src/com/daoyu/*.java
java -classpath ./libs/json-simple-1.1.1.jar:./bin com.daoyu.TweeterAverageDegree