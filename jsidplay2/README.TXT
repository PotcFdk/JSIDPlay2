mvn clean package
=================
creates:
target/deploy		-	webstart version, javafx
target/install		-	local PC version, javafx
target/netsiddev	-	network sid device
target/c64jukebox	-	c64jukebox applet version

mvn clean install
=================
creates: everything mentioned above
and uploads to web-server

Set properties:
in Eclipse: Preferences/Run/Debug StringSubstitution
or at command line: using -D

e.g.:
-Dftp.username=XXX
-Dftp.password=XXX
-Dftp.server=ftp.kenchis.t15.org
-Dftp.remotedir=/javafx
-Dserver.url=http://kenchis.t15.org/jsidplay2/javafx/
