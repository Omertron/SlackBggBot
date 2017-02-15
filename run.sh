#!/bin/bash
git reset --hard HEAD
git pull
mvn clean compile assembly:single
java -classpath .\target\* com.omertron.slackbot.SlackBot

