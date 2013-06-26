Limon REU wave player

VICE users:
1. insert image into 16MB REU by: settings\cartridge/io settings\REU settings...\...
2. set SID type to 8580 XXX (reSID-fp) with sample method "resampling" or "fast resampling" by:
   settings\SID settings...\...
3. load & run the player

1541 Ultimate users (firmware higher than 2.0) :
1. place those 2 files on your media
2. load REU image by chosing: Load into REU
3. load & run the player

NOTES;
The sound quality is 44100Hz 8bit.
Player with bars on real 64 will add some noise to samples.
Player will not work properly on Ultimate with firmware 2.0 and older (REU bug) .
When using VICE "resampling" quantization method will be probably to much for PC with one core CPU.
You can run Your own wave file converting it to 44100Hz 8bit and padding to 16 777 216 bytes by $80 at the end (padding not needed on Ultimate) .
The playback routine is rather quiet, so pump up the volume.

Data/De-Koder/Tropyx

