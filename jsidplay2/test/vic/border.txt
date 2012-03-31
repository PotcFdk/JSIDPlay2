
All PRGs compiled with acme
Run them with
LOAD"BORDER-....PRG",8,1
SYS2064


border-<n>:
-----------
Opens right border in cycle 56 of line n and following lines.
While n=250 is early enough to let the border open, n>=251 will make the
bottom border flipflop set and so the border isn't open.
n=251 shows a bug in VICE-2.0 and earlier.

border-bm-idle:
---------------
Opens right border in the line before VIC-II changes from Hires bitmap to
idle state after last display line.
The open border seems to prevents the VIC-II from switching to
black as it usually does in idle state. Exploits a bug in VICE that is
also visible in Krestology/Crest.

border-bm-ysh(2):
-----------------
Creates idle lines within display area and opens side border. This exposes
bug in VICE showing that black idle color starts in display area while
the left open border still shows bitmap background color starting in the
open right border in the line before.

border-mcbm:
------------
Another test showing that idle data in mc bitmap mode should be displayed
the "mc way" with double sized pixels which VICE doesn't do.
