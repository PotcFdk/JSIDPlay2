Common Use Cases of JSIDPlay2:

1. Listen to SID tunes of the HVSC or CGSC
Music collections are organized in a tab called HVSC or CGSC depending on the
collection name just click it.
Steps to accomplish:
- Download the music collection
- Extract all files and folders to your hard disk in a folder of your choice
- Configure JSIDPlay2 to use that collection clicking on the "Browse..." button on the HVSC/CGSC tab
- Play a tune (just double click on it)

2. Create a playlist of your favorite tunes
Favorites are organized similar to browser tabs in the Favorites tab.
- Select the tunes (left mouse button) from your music collection with your mouse in the HVSC/CGSC tab
- Open the context menu (right mouse button) to add it to a favorites list
- Optionally open the context menu on the column header to display additional information (Name, Author, etc.)

3. Play tunes without user interaction
Favorite tunes can be played in normal or random order within one or all favorites list(s)
- Play one of your favorite tunes (double click it)
- Activate the "Playback On/Off" combo box and choose for example the radio button "random (one playlist)"
- Click on "Respect Song Length" check box to let JSIDPlay2 switch to the next song
if the current song has ended.
- Optionally activate the check box "Single Song" to play only the start song per tune,
because a tune can contain multiple songs (usually appended SFX of a game).

4. Watch a demo on the video screen
Demos can be loaded in any form, that is a disk image, a tape image or a single program file.
As a floppy is part of this emulator it is possible to load demos that
loads further data from disk, nonetheless if they make use of fastloaders.
- On the Video tab click on the disk drive and optionally activate the drive sound, which
is a pre-recorded sound to give some feedback of floppy disk activity.
On the status bar you can see the current track number changing while loading from disk:
E.g. "FloppyTrack: 018"
- "Insert disk..." or "Insert tape..." by pressing the related button and choose your disk or tape image file,
that is a *.d64 or *.t64 file for instance.
- Press the "Reset" button
Click on the video screen to activate the keyboard.
Now type the Load command as desired:
Load "$",8              - Load directory of a floppy disk
List                    - List contents of a previously loaded directory
Load "*",8,1            - Load the first entry of a floppy disk
Load                    - Load a file from tape
Run                     - Start program (it is possible that the program does start automatically)
Now enjoy the fine quality of the PAL video screen and the sound quality of the emulation.

5. Compare sound quality to a recorded tune in MP3 format.
Load a tune for example double click it on the HVSC tab.
In the context menu click on the download button of the correct SID model (e.g. "Download URL (6581R2)").
When the tune re-starts automatically you can click the radio button "Emu" or "Recording" in the sound settings to switch between the emulated sound output
or the recorded tune (default playback type).
Eventually check the Console tab for error messages.
Optionally, it is possible to select a recording on your file system.
Just click the browse button, that shows up clicking on the "Compare to MP3/WAV recording" combo box.
Also a proxy (hostname/port) can be configured on the Sound Settings tab.

6. Use JSIDDevice as a Network SID Device.
JSIDDevice can be used for playback with ACID64 (http://www.acid64.com/).
Install and launch ACID64 using the latest version.
Now you are able to switch between the emulation and the real SID chip (HardSID4U sound-card) in ACID64 on the fly.
Instead of launching the whole player JSIDPlay2 you must use the slim JSIDDevice
"jsiddevice-3.0.jar",
which shows up as a little icon in the system tray.
Now you can use ACID64 with it.
In the system tray JSIDDevice has a context menu to read the credits or to exit the Network SID Device.

7. As a goodie JSIDPlay2 ships with a java version of lame 3.98.4 (MP3 encoder/decoder framework).
You can call "java -jar jump3r-1.0.jar" to make use of it. It is fully compatible.

8. Quick Configuration
An "Auto Online Configuration" in the HVSC, CGSC, HVMEC, Demos, Magazines and Games makes it possible
to use prepared contents from our projects web-site without the need of a manual download.
However you can modify the contents by yourself. It is stored in the default temp directory:
<user_home>/.jsidplay2 (e.g. C:/users/Ken/.jsidplay2)
