#!/bin/bash

while true; do
  git pull
  mvn clean compile assembly:single
  java -jar .\target\slack-bot-1.0-SNAPSHOT-jar-with-dependencies.jar

  if [ $? -eq 1 ]
  then
    # restart
  else
    # exit
    break
  fi
done
