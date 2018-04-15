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

e.g. SSH copy
-DserverId=
-DserverUrl=scp://ken:<password>@192.168.1.1/home/ken
-DremoteDir=public_html

Optional:
-Dgb64.mdbfile=<pathToGameBase64.mdb>
-Dhvsc.zip=<pathToHVSC.zip>
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

Java WebStart pitfalls:
=======================
Common:
Java Control Panel:
1. The server URL (starting with file:// or http://) must be configured in the Java Control Panel's security panel: exception list for local testing of the WebStart application
2. Also the security settings must be lowered to medium. 
see src/main/webstart/Java Control Panel.jpg
3. In pom.xml change codebase to webservers URL, where the app is uploaded to. For local tests it must be the local file URL e.g. "/home/ken/workspace/jsidplay2/target/deploy/online/webstart" (Windows goes like "file:///d:/<path>")
In every jar of the app in the META-INF/MANIFEST.MF the codebase must exactly match this server's URL.
4. In every jar's META-INF/MANIFEST.MF there must be at least Implementation-Vendor, Implementation-Title, Codebase and Permissions
5. Every jar MUST be signed with exactly ONE certificate. Self-made certificates expire after 6 month. If one JAR is already signed, signature should be removed to simplify things.
   (Officially and as an alternative it is suggested to create different deployments for each certificate which is not the way i did it).
6. Info: Debugging and fiddling out Java WebStart errors is awful, error messages appear in ugly dialog boxes and security warnings frightens users from installing it!
Over and out!

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

How to debug Java WebStart applications:
set JAVAWS_VM_ARGS="-Xdebug -Dnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=8000"
javaws http://haendel.ddns.net/~ken/online/webstart/JSIDPlay2.jnlp

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

