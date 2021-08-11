#!/bin/bash
export LD_LIBRARY_PATH=.
java --module-path . --add-modules javafx.controls,javafx.web,javafx.fxml --add-opens java.base/java.io=org.apache.tomcat.embed.core --add-opens java.base/java.util.function=jcommander -jar ./${project.artifactId}-${project.version}.jar "$@"
