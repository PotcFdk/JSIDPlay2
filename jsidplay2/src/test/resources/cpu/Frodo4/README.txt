DADB test program should run code in the color ram. This exposes a CPU-VIC
interaction as the high nybble of every byte read from color RAM is stale
data from the VIC's read on the same cycle.

DE00ALL executes a program in the open I/O region de00-dffff.

Pressing space should cause screen color to change, when these programs work.
