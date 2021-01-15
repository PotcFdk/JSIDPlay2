#!/bin/bash
cd ~/Downloads/${project.artifactId}-${project.version}-java11-linux
export LD_LIBRARY_PATH=~/Downloads/${project.artifactId}-${project.version}-java11-linux
java --module-path ~/Downloads/${project.artifactId}-${project.version}-java11-linux --add-modules javafx.controls,javafx.web,javafx.fxml --add-opens java.base/java.io=org.apache.tomcat.embed.core -jar ${project.artifactId}-${project.version}.jar
