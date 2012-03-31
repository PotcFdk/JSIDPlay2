FAQ - JSIDPlay2 - Music Player & C64 SID Chip Emulator

1. What is JSIDPlay2?
JSIDPlay2 is a software that makes it possible to listen to SID music of the Commodore 64 (C64)
a very popular Home Computer of the 80's. Beside that, it has evolved to a full featured Commodore 64 emulator.

2. What is a SID?
SID (Sound Interface Device)is the name of a sound chip and part of the Commodore 64.
It is responsible for sound playback and a synthesizer which can be programmed in Basic and Assembler
to produce sound output.

3. What is SID music?
SID music (or SID tune) is a program running on the C64, that produces sound.
SID is also a file extension. Emulators such as JSIDPlay2 are able to load SID music and
play the songs contained in a SID tune.

4. Where can I get SID music?
- High Voltage SID Collection (HVSC) - the biggest and most popular SID music collection
http://www.hvsc.de/
- Compute's Gazette SID Collection (CGSC) - A collection of COMPUTE!'s Sidplayer music containing tons of stereo files
http://www.btinternet.com/~pweighill/music/
- Stone Oakvalley's Authentic SID Collection (SOASC=) - A recording project containing SID music both in SID and MP3 format.
http://www.6581-8580.com

5. What is the system requirement to use JSIDPlay2?
JSIDPlay2 is a Java program and should run on every Java enabled platform,
that is at least Windows, Linux and Mac OS X. Java version 1.6 is required as a minimum to run JSIDPlay2.
Because the emulation is cycle exact, a fast PC is also required. I recommend to use at least a double core CPU
for the highest quality settings. CPU usage may vary depending on the settings of the emulator.
You also need a soundcard (16 bit sampling at 44.1KHz-96KHz). Sound output is mono or stereo (for specific tunes).

6. Which music formats does JSIDPlay2 support?
SID - HVSC format (containing one or multiple songs)
MUS - CGSC stereo format (voices 1-3)
STR - CGSC stereo format (voices 4-6)
DAT - SID music data
PRG - C64 program files (a C64 program)
P00 - C64 program files (a C64 program)
Because JSIDPlay2 is a complete C64 emulator, it supports additional peripherals like the Floppy C1541,
the Datasette C1530 and plugged-in Cartridges. These are emulated using the following file extensions
as a replacement of the Floppy disk, the tape and the cartridge.
D64 - Disk image
G64 - Disk image
NIB - Disk Image (read-omly)
TAP - Tape image
T64 - Tape image
REU - Ram Expansion Unit memory dump
CRT - Cartridge memory dump
In some rare cases SID music is part of a Floppy disk or a tape or a cartridge, which must be inserted first,
but normally just loading a single file should do the job just fine.
ZIP - Every emulation format can be contained within a ZIP file. We treat ZIPs like a directory
Note: The file chooser now has a preview for disk directory contents. A pseudo directory is shown
      for cartridges (name and size in KB), Tapes (size in bytes and name of the tape entries),
      cartridges (name and size in KB) and tunes (tune details).

7. What sound output JSIDPlay2 supports?
Normally a sound card is used to play music, but you can choose the following things alternatively:
- Buy a HardSID4U soundcard for your PC (www.hardsid.com) and choose that for sound output. Connection is via USB cable.
  Be aware, that 32-bit driver in combinaton with 32-bit Java is supported, only!
- Listen to MP3 sound file (Yes, we support playback of MP3 even the collection in playlists!)
- Listen a tune and write a WAV file to your harddisk at the same time.
- Listen a tune and write a MP3 file to your harddisk at the same time.
- Switch between an MP3 recording and the emulation while playback to judge the emulators quality.

8. How can I use JSIDPlay2, what sort of software is it?
JSIDPlay2 is software which can be started as
- an application with a multi-language (English and German) user interface (UI) to control
  the player using the mouse and keyboard.
- a console player controlled by keyboard- an applet to be embedded in a web-page with or without a UI.
- a Network SID Device to be used with ACID64 (http://www.acid64.com) altogether with HardSID4U sound-cards (http://www.hardsid.com).
So you got one program, that can be used in multiple ways.

9. How to start JSIDPlay2?
Important Note: Installation is strongly recommended on drive C for Windows.
Since configured contents (HVSC, CGSC, etc.) must be stored on the same drive.
Background is this: The configuration file does not store drive letters for file names. This makes
it possible to run JSIDPlay2 on an USB stick using different drive letters each time.

How to start JSIDPlay2? That depends on what you want to start.
- For the UI version:
Type "jsidplay2.exe" (Windows)
or Type "java -jar jsidlay2.jar" (Linux, Mac OS X)
- For the console player:
Type "jsidplay2_console.exe" (Windows)
or Type "java -jar jsidlay2_console.jar" (Linux, Mac OS X)
- For the Applet version embed these HTML tags to your web-site and sign all jars
or grant them proper access rights.
  - Applet with UI:
<APPLET width="1024" height="768" align="left"
        archive="hsqldb.jar,jinput.jar,photos.jar,jump3r.jar,swixml.jar,jsidplay2.jar,ScreenRecorder.jar" code="applet.JSIDPlay2.class">
</APPLET>
  - Hidden Applet (without UI) playing a SID:
<APPLET name="jsidplay" code="c64jukebox.SIDPlay" archive="c64jukebox.jar" WIDTH=0 HEIGHT=0 MAYSCRIPT >
</APPLET>
<A href="javascript:document.jsidplay.playSID('http://www.example.org/example.sid', 1)">Click to play example.sid (Song 1)</A>

10. How compatible is JSIDPlay2?
JSIDPlay2 is known to be a very accurate C64 emulator.
It emulates all components of a C64 and some important peripherals.
The emulation is cycle exact and passes many test programs.
- CPU
We now pass the entire Lorentz suite. The CPU compatibility should be very good.
- CIA
We pass Lorenz suite's CIA tests, and various VICE testprograms. The CIA compatibility should be very good.
- VIC
We have a reasonably good, cycle-exact simulation of the VIC, and pass some very complicated VICE testprograms
such as the irqdma suite. However, some sprite tests like those used by various emutesters,
and some inline video mode changes are buggy.
- C1541
The disk drive is very compatible. All chips are emulated cycle exact, although a few loaders deny to work.
- ReSID 1.0 beta. Sound work is always ongoing.
- Cartridges
We have added support for various classic cartridges and the CRT format. We can support normal 8k and 16k,
and some freezer cartridges.
Cartridge support is limited to Action Replay, Atomic Power, Comal80, Epyx FastLoader, Expert Cartridge,
Final Cartridge, Final Cartridge III (87, 88), Rexx Datentechnik, Zaxxon and Easyflash.
REU is supportet as well (16 MB), just check out the excellent BlueREU demo by Crest.

11. What does that mean: java.lang.UnsatisfiedLinkError: no jinput-dx8 in java.library.path?
JSIDPlay2 has joystick support, but that is not directly supported in Java.
It loads a native library at runtime to do that.
This release ships with libraries for Windows, Linux and Mac OS X, other OSs will show this error message.

12. What else is contained in JSIDPlay2?
- Games (GameBase64) has being integrated to browse for games and playing them.
  You need to be online to make use of that feature. The database will be downloaded on-demand,
  just watch the progress bar and be patient.
- Demos can be watched in a separate panel. Directory and screenshot are shown here.
  Demo panel contents is downloaded from our projects web-site by clicking "Auto Online Configuration",
  just watch the progress bar and be patient. Manual configuration is possible as well.
  Double-click in the tree view starts the image.
- HVMEC (High Voltage Music Engine Collection) - A collection of music editors and trackers.
  Same usage as in the Demo panel described above.
- Magazines (Some excellent disk magazines)
  Same usage as in the Demo panel described above.
- Printer page (emulated printer is MPS803). Everything that is printed apears on that panel.
======================================================================
Credits:

Credits go first to the original authors for doing such a great sidplay2 software!

Dag Lem	            reSID emulation engine
Michael Schwendt	SidTune library, Sid2Wav support
                        and basis for xSID...
Simon White	        Sidplay2 music player library v2
Antti Lankila	    SID chip Distortion Simulation efforts (resid-fp)
                        and continuous development of the whole emulator
======================================================================
License:

This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  
======================================================================
