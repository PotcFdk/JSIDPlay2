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
-Dftp.remotedir=/jsidplay2/javafx

Optional:
-Dgb64.mdbfile=<pathToGameBase64.mdb>
-Dhvsc.zip=<pathToHVSC.zip>
-Dzip7.home=<pathToPacker7zip>
-Dcgsc.7z=<pathToCGSZ.7z>
-Dhvmec.bz2=<pathToHVMEC.tar.bz2>

Java WebStart pitfalls:
=======================
Common:
Java Control Panel:
1. The server URL (starting with file://) must be configured in the Java Control Panel's security panel: exception list for local testing of the WebStart application
2. Also the security settings must be lowered to medium. 
see src/main/webstart/Java Control Panel.jpg
3. In pom.xml change codebase to webservers URL, where the app is uploaded to. For local tests it must be the local file URL e.g. "file:///d:/workspace/jsidplay2/target/deploy/online/webstart"
In every jar of the app in the META-INF/MANIFEST.MF the codebase must exactly match this server's URL.
4. In every jar's META-INF/MANIFEST.MF there must beat least Implementation-Vendor, Implementation-Title, Codebase and Permissions
see src/main/webstart/addToManifest.txt
5. Every jar MUST be signed with exactly ONE certificate. Self-made certificates expire after 6 month. If one JAR is already signed, signature should be removed to simplify things.
   (Alternatively it is suggested to create different deployments for each certificate which is not the way i will do it).
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
"c:\Program Files\Java\jdk1.7.0_07\bin\javaws.exe" http://kenchis.t15.org/jsidplay2/jsidplay2.jnlp
	