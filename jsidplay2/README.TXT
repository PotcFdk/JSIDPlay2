mvn clean install
=================
target/standalone	-	local PC version
target/deploy		-	website version
target/jfx/native	-	native launchers or installers

mvn clean deploy
=================
does mvn clean install
and uploads target/deploy to web-server

Set the following properties in Eclipse: Preferences/Run/Debug StringSubstitution
or at command line: using -D

e.g. SSH copy
-DserverId=
-DserverUrl=scp://ken:<password>@192.168.1.1/home/ken
-DremoteDir=public_html

Optional:
-Dgb64.mdbfile=<pathToGameBase64.mdb>
-Dhvsc.7z=<pathToHVSC.7z>
-Dzip7.home=<pathToPacker7zip>
-Dcgsc.7z=<pathToCGSZ.7z>
-Dhvmec.bz2=<pathToHVMEC.tar.bz2>
-Dupx.home=<pathToUPX>

Use OpenJDK For Linux Users:
============================
OpenJDK + OpenJFX + PulseAudio Mixer is required!
Execute the following commands:
sudo apt-get install openjdk-8-jdk
sudo apt-get install openjfx
sudo apt-get install pavucontrol

For support of double click on the JAR to start JSIDPlay2:
Right click on jsidplay-<version>.jar
Open with...
Choose a different one...
/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java -jar
X Remember file type assignment

In Eclipse Preferences/Java/Installed JREs
Add:
/usr/lib/jvm/java-8-openjdk-amd64/jre
X Use as default Java

Eclipse JavaFX support:
=======================
1. Install e(fx)clipse
2. To get a content assistant on javafx css files:
Right click on your project -> properties -> Java build path -> libraries -> add library -> javaFX SDK
To edit .fxml files with FXML Editor:
Right click on .fxml file: Open With -> Other... -> FXML Editor (Check "Use it for all '*.fxml' files") -> OK
To specify text resource inside .fxml file use:
<?scenebuilder-preview-i18n-resource GameBasePage.properties?>
To use style sheets inside .fxml file use:
<Scene stylesheets="@JSidPlay2.css"> ...
To use JavaFX Preview view in Eclipse, change root tag in fxml file (uncomment commented out alternative version)
Eventually uncomment line in C64VBox.java (see TODO marker), still not sure, why some of the layouts need that.

How to Create a HTTPS certificate easily:
=========================================
https://certbot.eff.org/lets-encrypt/ubuntutrusty-apache

Tips:
=====

Shrink jsiddevice:
upx.exe --lzma --best --ultra-brute jsiddevice.exe

find out available JavaFX style classes of a node in the scene graph:
	private void dump(Node n, int depth) {
		for (int i = 0; i < depth; i++)
			System.out.print("  ");
		System.out.println(n);
		if (n instanceof Parent)
			for (Node c : ((Parent) n).getChildrenUnmodifiable())
				dump(c, depth + 1);
	}

Chameleon Command-Line:
=======================
sudo gedit /lib/udev/rules.d/100-local-permissions.rules:
SUBSYSTEMS=="usb" ATTRS{idVendor}=="18d8" ATTRS{idProduct}=="201d" MODE:="0777" SYMLINK+="Chameleon"

RESET (reset.txt consists of exactly one zero byte)
./chacocmd  --addr  0x80000000 --writemem ./reset.txt

SEND PROGRAM
./chacocmd  --addr  0x0801 --writemem radiantx_spiral_silicon_towers.mem 

TODO:
=====
libresidfp introduced a new envelopegenerator and a new filter8580 recently.
(Nov. 2016)

Install HTTPS for Apache2 (letsencrypt):
========================================
sudo apt-get install software-properties-common
sudo add-apt-repository ppa:certbot/certbot
sudo apt-get update
sudo apt-get install python-certbot-apache
sudo certbot --apache

Check Certification renewal (letsencrypt):
==========================================
sudo certbot renew --dry-run

Re-use certificate of apache2 received by letsencrypt for JSIDPlay2 AppServer HTTPS connections:
================================================================================================
(source: https://www.wissel.net/blog/2018/03/letsencrypt-java-keystore.html)

# create one big certificate file
sudo cat /etc/letsencrypt/live/haendel.ddns.net/{cert.pem,chain.pem,fullchain.pem,privkey.pem} > fullchain.pem

# convert certificate file to PKCS12 required by keytool
openssl pkcs12 -export -out fullchain.pkcs12 -in fullchain.pem

# Create empty keystore
keytool -genkey -keyalg RSA -alias sfdcsec -keystore sfdcsec.ks
keytool -delete -alias sfdcsec -keystore sfdcsec.ks

# Import certificate into keystore
keytool -v -importkeystore -srckeystore fullchain.pkcs12 -destkeystore sfdcsec.ks -deststoretype JKS

# Change alias name
keytool -keystore sfdcsec.ks -changealias -alias 1 -destalias jsidplay2

# Launch JSIDPlay2 AppServer with HTTPS enabled:
java -server -classpath jsidplay2-3.9.jar ui.servlets.JSIDPlay2Server \
		--appServerKeystore sfdcsec.jks \
		--appServerManagerPassword jsidplay2 \
		--appServerKeystorePassword jsidplay2 \
		--appServerConnectors HTTPS
