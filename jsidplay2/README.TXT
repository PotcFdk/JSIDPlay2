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


Tips:
=====

Shrink jsiddevice:
upx --best --ultra-brute jsiddevice.exe

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
	