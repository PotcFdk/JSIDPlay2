MMC64 Bios Upgrade to V1.10
(C)2007 by Oliver Achten

To upgrade your MMC64 to the newest version, place
the file "mmc64v1b.upd" into your SYSTEM64 directory
and hit the f5 key. Follow the instructions carefully!

**************
* CHANGELOG: *
**************

CHANGES FROM V1.04
------------------
Finally, a big update this time:

- FAT32 support! 

Yes, I managed to squeeze FAT32 support into the MMC64 
Bios. Now you don't have to worry about cluster sizes 
anymore with big memory cards. Currently, cards up to 
4GB are supported. Cards >4GB have yet to be released
and require a change of the MMC/SD command set anyway.

- New plugin system:

FAT32 support required me to rework the plugin system.
When a FAT16 formatted card is used, all plugins work as
usual. Using FAT32 formatted cards, old plugins won't
work anymore, only new, so called "multifat" plugins will
work, which are designed to support BOTH FAT16 and FAT32.
With the release of this bios, most plugins written by me
are re-released in the new format.

- Fixed loading of plugins with long filenames

- Fixed support for very small cards.

- Fixed some problems with FAT buffering.

- Fixed a bug in the volume name display routine.

- Instead of showing a computed size value, the browser
  displays the card's model ID, which always includes
  the size.

CHANGES FROM V1.03
------------------
Small release this time

- Sidplayer fixes

- New feature: multiple plugin support!
	Press <L-SHIFT> + <RETURN> to load alternative plugin,
	which has the ".ALT" file extension.
	
	Example: if you want to use 2 D64 plugins, place
	D64PLGIN.BIN into the SYSTEM64 directory, rename
	the 2nd plugin to D64PLGIN.ALT and copy it into
	your SYSTEM64 directory.
	
	Now press: <RETURN> to use D64PLGIN.BIN
	           <L-SHIFT> + <RETURN> to use D64PLGIN.ALT
	
	This way, you can have both D64 reader and the D64
	kernal plugin. It works with all plugins.

CHANGES FROM V1.01
------------------
NOTE: V1.02 was not oficially released

- Removed kernal D64 writer out of the ROM to be loaded
as a plugin. This makes place for new BIOS features.

- New plugin system! the following key shortcuts will
attempt to load plugins for various tasks:

H - helpme plugin (displays a helpme message)
D - delete plugin (deletes a selected file)
M - makedir plugin (creates a new directory)

This plugin system is going to be expanded in the future.

- Rewritten SID player. Player data is now hidden
"under" the SID I/O area, which makes more tunes playable.

- Plugin system is finally case insesitive. Now it makes no
difference if you have a SYSTEM64 or a SyStEm64 folder. ;)

- Number of directories/files and directory/file position
of the cursor is now shown in the status bar for better 
navigation.

- Sidplayer can now be loaded as a SID plugin. The Sidwatcher
plugin from fieserWolF /Metalvotze can now be used regularily.

- Stupid NMI bug fixed.

CHANGES FROM V1.00
------------------
- just a minor update to fix a bug affecting
	the new D64 Reader plugin

CHANGES FROM V0.98
------------------
NOTE: v0.99 was not oficially released

- Improved SidPlayer compatibility
- Fixed occasional crashing in some RSID
	files when a key is pressed
- Accelerated start of *.prg files
- Removed size bug in file loader
- Added buffering of cursor position when 
  leaving a sub-directory
- Added jumping to start/end of files/dirs
	using the f1/f7 key.
- Fixed some problems with the D64 writer

CHANGES FROM V0.97
------------------
- Improved card size recognition
	(thanx to tnt/beyond force!)
- Volume name is now correctly recognized
- Card removal detection (quits filebrowser)
- Extended filebrowser window again
- Implemented basic error correction

CHANGES FROM V0.96
------------------
- Long filename support (yeah!)

NOTE: when creating the system64 directory, please
create it using ONLY lowercase letters in windows, since
other combinations results in the creation of a LFN for
the system64 directory, which will NOT be found by the
search code. The same goes for plugin filenames.

- Extended filebrowser window (20 instead of 17 lines)
- Increased SID player compatibility a bit
- Fixed problems with some C128 computers
- Improved MMC/SD compatibility
- Fixed bug in D64 writer

CHANGES FROM V0.95
------------------
- Fixed again a stupid reset bug

CHANGES FROM V0.94
------------------
- Fixed a very naughty undocumented FAT16 behaviour, which
  caused imcompatibility with some cards
- Added a short delay to prevent corruption of the
  card initialization process
- Reset behaviour refined. Now you can safely reset the 
  computer while data is still being read from the card.

CHANGES FROM V0.93
------------------

- Fixed MMC reset bug
- Fixed buffer problem in bootroutine