README.txt
----------
Box Check "Type" tries to determine the type of your box.

The results are presented on the form:
 "<machine> <glue> <vic-ii> <sid> <cia1> <cia2>"

The test is run a number of times.  If the results are 100% repeatable the
box will be green.   If the results are slightly flaky the box will be
brown/orange.  If the results are very flaky it will say "FAILED".
(R/S RESTORE + RUN to redo the test)

Typical breadbox:
  "C64 DL 6569 6581 6526 6526"

Typical C64C:
  "C64C IC 8565 8580 6526A 6526A"

Typical C128:
  "C128 DL 8566 8580 6526A 6526A"

The main code is at $c000-$c1c1 and returns a two byte value in Acc and X
which will end up at $cf00 & $cf01 followed by the string representation.

Acc/$cf00:
  Bit 3 = 8566
  Bit 2 = 6569R1
  Bit 1 = Glue Logic (0 = discrete, 1 = custom IC)
  Bit 0 = VIC-II model (0 = 6569, 1 = 8565)

X/$cf01:
  Bit 5 = SID present
  Bit 4 = SID model (0 = 6581, 1 = 8580)
  Bit 3 = CIA2 present
  Bit 2 = CIA2 model (0 = 6526, 1 = 6526A)
  Bit 1 = CIA1 present
  Bit 0 = CIA1 model (0 = 6526, 1 = 6526A)

Thanks to nojoopa for ideas and to Rubi and MOS6569 for C64C testing.

/Daniel Kahlin (aka tlr)

LEGEND
------

<machine>
- C64
- C64C
- C128

Aggregated guess of machine type.
A question mark after machine denotes an unlikely combination, like a
6581 sid in a C64C and so on...
---

<glue>
  DL = Discrete Logic (C64/C128)
  IC = Custom IC (C64C)

Type of glue logic implementation.
---

<vic-ii>
  6569R1 = Very old VIC (5 lumas)
  6569   = Old VIC (9 lumas)
  8565   = New VIC (C64C)
  8566   = New VIC (C128)

Model of VIC-II chip.
---

<sid>
  6581 = Old SID (C64)
  8580 = New SID (C64C/C128)

Model of SID chip.
---

<cia[1|2]>
  6526  = Old CIA (C64)
  6526A = New CIA (C64C/C128)

Model of CIA chips.
---

eof
