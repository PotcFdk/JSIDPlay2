1090 rem *** laufwerkfehler analyse
1091 :
1100 open15,o,15:input#15,a,b$,c,d:close15:c$=str$(c):cc$=right$(c$,len(c$)-1)
1105 d$=str$(d):dd$=right$(d$,len(d$)-1):c$=cc$:d$=dd$
1110 ifa=0thener$="Erfolgreich ausgefuehrt"
1111 ifa=1thener$="Datei wurde geloescht"
1112 ifa=20thener$="Blockheader unauffindbar Tr."+c$+" S."+d$
1113 ifa=21thener$="SYNC-Markierung unauffindbar Tr."+c$+" S."+d$
1114 ifa=22thener$="Block-Header unauffindbar Tr."+c$+" S."+d$
1115 ifa=23thener$="Falsche Pruefsumme Tr."+c$+" S."+d$
1116 ifa=24thener$="Decodierfehler Tr."+c$+" S."+d$
1117 ifa=25thener$="Verifizierungsfehler"
1118 ifa=26thener$="Diskette hat Schreibschutz"
1119 ifa=27thener$="Pruefsumme falsch Tr."+c$+" S."+d$
1120 ifa=28thener$="Datenblock zu lang"
1121 ifa=29thener$="Falsche ID"
1122 ifa>=30anda<=39thener$="Falscher Befehl"
1123 ifa=50thener$="Record nicht verfuegbar"
1124 ifa=51thener$="Record zu lang"
1125 ifa=52thener$="Datei zu gross gewaehlt"
1126 ifa=60thener$="Datei noch nicht geschlossen"
1127 ifa=61thener$="Datei noch nicht geoeffnet"
1128 ifa=62thener$="Datei unauffindbar"
1129 ifa=63thener$="Datei existiert bereits"
1130 ifa=64thener$="Falscher Dateityp"
1131 ifa=65thener$="Block bereits belegt"
1132 ifa=66thener$="Illegaler Track "+c$+" und Sektor "+d$
1133 ifa=67thener$="Illegaler Track "+c$+" oder Sektor "+d$
1134 ifa=70thener$="Kanal nicht mehr frei"
1135 ifa=71thener$="BAM nicht lesbar Tr."+c$+" S."+d$
1136 ifa=72thener$="Diskette voll"
1137 ifa=73thener$=b$
1138 ifa=74thener$="Keine Diskette in Laufwerk"
1140 return