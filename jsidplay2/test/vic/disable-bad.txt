D011TEST:
* from a mail on 22.12.2005 from Andreas Boose *

...
PROFESSIONAL-ASS starten
RETURN
N
D011VT
G
R
E
F7 gedrückt halten, bis der Scroll einsetzt
F7 wiederholt drücken, bis das rechte der gelben F den rechten Rand erreicht hat und damit alle G aus der F Reihe verschwunden sind.
Jetzt ist cycle=54 und damit der untere Codezweig aktiv.
5 mal cursor down, damit der $ff cursor auf der Doppelreihe E steht.
Die Linie zwischen den beiden E ist der idle state, d.h auch der cursor ist durch diese Linie getrennt.
39 mal cursor right, damit der cursor ganz rechts steht.

Und hier sehen wir den Fehler: Der obere und untere cursor berührt sich dieser Stelle, da schon in diesem Zyklus der display state eingeschaltet wird. Das ist bei meinem C64 nicht der Fall, der ist in diesem Zyklus noch im idle state. Beim C64 kann ich mit F5 einen Zyklus zurückschalten und an dieser Stelle wird aus dem idle state der display state. Damit ist bewiesen, dass bei VICE an dieser Stelle der display state genau einen Zyklus zu früh eingeschaltet wird.
...


disable-bad:
Exposes a VICE bug also visible in demo "angle +1".
A badline is forced in cycle n and then immidiately disabled where n is in {54,10}.
The first line should display normal A where the B in the second line has has almost
double height.
