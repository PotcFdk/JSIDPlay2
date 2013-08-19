mvn clean install
=================
target/standalone	-	local PC version
target/deploy		-	website version

mvn clean deploy
=================
does mvn clean install
and uploads target/deploy to web-server

Set the following properties in Eclipse: Preferences/Run/Debug StringSubstitution
or at command line: using -D

e.g.
-Dftp.username=XXX
-Dftp.password=XXX
-Dftp.server=ftp.kenchis.t15.org
-Dftp.remotedir=/javafx
-Dserver.url=http://kenchis.t15.org/jsidplay2/javafx/