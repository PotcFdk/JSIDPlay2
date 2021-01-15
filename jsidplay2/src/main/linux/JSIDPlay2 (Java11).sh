#!/bin/bash
cd ~/Downloads/${project.artifactId}-${project.version}-java11-*
export LD_LIBRARY_PATH=.
java --module-path . --add-modules javafx.controls,javafx.web,javafx.fxml --add-opens java.base/java.io=org.apache.tomcat.embed.core -jar ${project.artifactId}-${project.version}.jar
