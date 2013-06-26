; ------------------------------------------------------
; 64er Proficorner 5/91 - FLD-Special / Single Pixel FLD
; ------------------------------------------------------

; Konvertiert und angepasst an ACME von spider jerusalem
 
; CHANGES:	Charsetlogo und Screentabelle aus Demo hinzugefügt.
;		Basicstart hinzugefügt


!to "single_pixel_fld.prg",cbm

*= $0801
	!byte $0c,$08,$00,$00,$9e,$20,$34,$30,$39,$36,$00,$00

;Anwendung des $d011-Effektes:

; Single-Pixel-FLD

;(c)Copyright 1991 by LUBBER of PADUA


;Der Original-Zeichensatz muss ab
; $3000 liegen

;~~~~ WICHTIG! ~~~~

;Die ersten 5 normalen und die ersten
;5 reversen Zeichen duerfen NICHT
;benutzt sein ! Sonst werden sie fuer
;den Effekt zerstoert !

;(Da dort die Screenzeilen abgelegt
; werden)


;Die Original-Screentabelle muss ab
;$2c00 liegen (Maximal 16 Zeilen hoch!)


*= $1000  					;Code nach $1000

		lda #8     			;Aktuelle Farbe auf
		sta 646    			;orange=schwarz in Multicolor
           				;setzen
		jsr $e544  			;Bildschirm loeschen
           				;und Color-ram mit
           				;aktueller Farbe fuellen

		lda #$40
		sta hibyte1+2
		lda #$48
		sta hibyte2+2
		lda #$50
		sta hibyte3+2
		lda #$58           	;Highbytes fuer
		sta hibyte4+2      	;Umrechnungsroutine
		lda #$60           	;erneuern
		sta hibyte5+2
		lda #$68           	;Nur wichtig,wenn
		sta hibyte6+2      	;die Routine oefters
		lda #$70           	;von neuem gestartet
		sta hibyte7+2      	;werden soll,ohne
		lda #$78           	;dass man den Code
		sta hibyte8+2      	;neu reinladen muss.


		lda #$ff
		tax
fillff1
		sta $37f8,x
		inx            		;Das 255.Zeichen des
		cpx #8         		;Zeichensatzes wird mit
		bne fillff1    		;$ff-Bytes gefuellt

		lda #$ff
		ldx #0
fillff2
		sta $2c00+600,x 		;Die letzte Cursor-Zeile
		inx             		;des Original-Bildes
		cpx #40         		;wird mit $ff-Bytes
		bne fillff2     		;gefuellt

		lda #0
		sta $fb
		lda #$30   			;Adresse des Zeichensatzes
		sta $fc    			;in die Register ($fb)


						;Zuerst wird der Original-Zeichensatz
						;fuer den Effect 'zurechtgeschoben'.

		ldx #0
		ldy #0
charbegin
		lda ($fb),y

hibyte1
		sta $4000,x   		;Dieser Zeichens. enthaelt
		iny           		;am Ende alle 1.Bytes
		lda ($fb),y   		;eines  einzelnen Zeichens

hibyte2
		sta $4800,x    		;Dieser alle zweiten
		iny            		;Bytes
		lda ($fb),y

hibyte3
		sta $5000,x    		;Dieser alle dritten
		iny
		lda ($fb),y

hibyte4       				; . ..
		sta $5800,x	
		iny
		lda ($fb),y

hibyte5
		sta $6000,x   		;....
		iny
		lda ($fb),y

hibyte6
		sta $6800,x
		iny
		lda ($fb),y

hibyte7
		sta $7000,x
		iny
		lda ($fb),y

hibyte8
		sta $7800,x 		;... und dieser alle
              				;8.Bytes

		iny         		;schon 256 Bytes (= 32
              				;komplette Zeichen)

		bne keinuebertrag1
         					;nein ? dann keinuebertrag1
         					;Original-
		inc $fc  			;Zeichensatz-Adressenhigh-byte
         					;um 1 erhoehen

keinuebertrag1
		txa
		clc         		;Zeiger der neuen Zeichens.
		adc #8      		; um 8
		tax         		;erhoehen
		bne notrag  		;da nur das 1.Byte der neuen
            				;Zeichensaetze benutzt
            				;werden

		inc hibyte1+2  		;wenn x-reg. wieder auf 0
		inc hibyte2+2  		;dann high-bytes erhoehen.
		inc hibyte3+2
		inc hibyte4+2
		inc hibyte5+2
		inc hibyte6+2
		inc hibyte7+2
		inc hibyte8+2

notrag
		lda $fc
		cmp #$38     		;schon ganzer Original-
             				;Zeichensatz bearbeitet ???

		bne charbegin  		;nein ?? dann  charbegin


						;Jetzt alle Zeilen der Original-
						;tabelle in die verschiedenen
						;Screens verschieben

		ldx #0
transfer
		lda $2c00,x
		sta $4000,x
		lda $2c00+40,x
		sta $4400,x
		lda $2c00+80,x
		sta $4800,x
		lda $2c00+120,x
		sta $4c00,x
		lda $2c00+160,x
		sta $5000,x
		lda $2c00+200,x
		sta $5400,x
		lda $2c00+240,x
		sta $5800,x
		lda $2c00+280,x
		sta $5c00,x
		lda $2c00+320,x
		sta $6000,x
		lda $2c00+360,x
		sta $6400,x
		lda $2c00+400,x
		sta $6800,x
		lda $2c00+440,x
		sta $6c00,x
		lda $2c00+480,x
		sta $7000,x
		lda $2c00+520,x
		sta $7400,x
		lda $2c00+560,x
		sta $7800,x
		lda $2c00+600,x      	;16 zeilen
		sta $7c00,x          	;wurden verschoben
		inx
		cpx #40
		bne transfer

		sei
		lda  #$7f
		sta  $dc0d
		lda  #$00
		sta  $dc0e
		lda  #$f1    		;Interrupt-Register
		sta  $d01a
		lda  #50     		;auf Rasterinterrupt
		sta  $d012
		lda  #$1b    		;vorbereiten
		sta  $d011   		;bei Rasterzeile 50
		lda  #<irqneu
		sta  $0314    		;IRQ-Pointer setzen
		lda  #>irqneu
		sta  $0315

		lda #24
		sta $d016   		;Multicolor einschalten

		lda #0
		sta $d020   		;Rahmenfarbe
            				;auf Schwarz
		lda #6
		sta $d021   		;Hintergrundfarbe
            				;auf Blau
		lda #14
		sta $d022   		;Mutlicolorfarben
		lda #15     		;auf hellblau und hell-
		sta $d023   		;grau setzen


		cli

back

    		jmp back  			;Endlosschleife

irqneu
		bit 0
		bit 0    			;Wartezeit

		lda  #150
		sta  $dd00     		;Auf die Bank ab $4000
               			;umschalten

		ldy  #0
loop
		lda  #$12         	;nacheinander
		sta  $d011        	;die werte
		lda  chartab,y    	;$12,$13,$14,$15
                  			;$16,$17,$10,$11
		sta  $d018        	;setzen um den FLD-
                  			;effekt zu erzielen
		iny
		nop               	;Rasterzeile austimen
		nop
		lda  #$13
		sta  $d011
		lda  chartab,y   		;nacheinander die $d018
		sta  $d018       		;werte aus der Tabelle
		iny              		;holen
		nop
		nop
		lda  #$14
		sta  $d011
		lda  chartab,y
		sta  $d018
		iny
		nop
		nop
		lda  #$15
		sta  $d011
		lda  chartab,y
		sta  $d018
		iny
		nop
		nop
		lda  #$16
		sta  $d011
		lda  chartab,y
		sta  $d018
		iny
		nop
		nop
		lda  #$17
		sta  $d011
		lda  chartab,y
		sta  $d018
		iny
		nop
		nop
		lda  #$10
		sta  $d011
		lda  chartab,y
		sta  $d018
		iny
		bit 0
		lda  #$11
		sta  $d011
		lda  chartab,y
		sta  $d018

		iny
		cpy  #136   		;Schon der 136.wert aus der
            				;Tabelle gelesen ???
           				;(Der FLD-Bereich ist dann
           				;136/8=17 Cursor-zeilen hoch)

		bne  loop   		;nein ?? dann loop

		lda #$72   			;Unteren Teil des Bildschirms
		sta $d011  			;unsichtbar machen

		lda #$fe
		ldx #0
loeschtab      				;$d018-Tabelle mit
		sta chartab,x   		;$fe-bytes fuellen.
		inx             		;Sie simulieren
		cpx #136        		;ein FLD mit ge-
		bne loeschtab   		;loeschtem $3fff

sinzeiger
		ldx #0

		ldy #0
setpixels
		lda #0         		;pixelnummer
		asl            		;mal 2
		sta chartab,y  		;in tabelle setzen
		cmp #$90       		;schon 72 pixel ???
               				;72/8=9 Cursor-Zeilen
               				;(Hoehe des Original
               				; Bildes)
		beq fertig     		;ja ?? dann fertig

		iny            		;zeiger zum setzen
		tya            		;der chartab erhoehen
		clc            		;und mit sinuswert
		adc sinus,x    		;addieren
		tay
		inx
		inc setpixels+1  		;pixelnummer erhoehn

		jmp setpixels  		;wiederholen

fertig
		lda #0
		sta setpixels+1  		;anfangspixel wieder
                 			;auf 0

		inc sinzeiger+1  		;sinuszeiger erhoehen
		lda sinzeiger+1
		cmp #100         		;schon tabelle durchge-
		bne keinsinend   		;laufen ????

		lda #0           		;ja ??
		sta sinzeiger+1  		;dann sinuszeiger auf 0

keinsinend
		lda #1
		sta $d019
		jmp $febc     		;Ende der IRQ-Routine


chartab
	!fi 136    				;Speicher mit 136 $00-bytes
         					;fuellen

sinus   					;Sinustabelle fuer
         					;den 'kuenstlichen' FLD

	!fi 70    				;70 $00-Bytes

						; jetzt kommen die Abstaende zwischen
						; den Pixelzeilen

	!byte 1,1,2,2,3,3,4,4,4,4,4,4,4,4,4,3,3,2,2,1,1

	!fi 80   				;80 $00-Bytes
	
; ----------------------------------- TABELLEN

*= $2c00


!byte $05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05
!byte $05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05
!byte $05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$06,$07,$08
!byte $09,$0a,$0b,$06,$07,$08,$09,$0a,$0c,$0d,$0e,$0f,$10,$0a,$0c,$0d
!byte $0e,$11,$12,$13,$14,$15,$07,$08,$09,$0a,$0b,$05,$05,$05,$05,$05
!byte $05,$05,$05,$05,$05,$16,$17,$18,$19,$1a,$1b,$16,$17,$18,$19,$1a
!byte $1c,$1d,$1e,$1f,$19,$1a,$1c,$1d,$20,$21,$22,$23,$24,$25,$17,$18
!byte $19,$1a,$1b,$26,$05,$05,$05,$05,$05,$05,$05,$05,$27,$28,$29,$2a
!byte $2b,$2c,$2d,$28,$29,$2a,$2e,$2c,$2d,$2f,$29,$2a,$2e,$2c,$2d,$2f
!byte $29,$2a,$2e,$2c,$30,$31,$29,$2a,$2e,$2c,$2d,$32,$05,$05,$05,$05
!byte $05,$05,$05,$05,$33,$34,$35,$36,$37,$38,$39,$34,$35,$36,$3a,$38
!byte $3b,$34,$35,$36,$3c,$38,$3b,$34,$35,$36,$3c,$38,$3b,$34,$35,$36
!byte $3a,$38,$3b,$3d,$05,$05,$05,$05,$05,$05,$05,$05,$33,$3e,$3f,$40
!byte $41,$42,$43,$3e,$3f,$40,$44,$45,$46,$3e,$3f,$40,$47,$45,$46,$3e
!byte $3f,$40,$47,$45,$46,$3e,$3f,$40,$44,$45,$46,$3d,$05,$05,$05,$05
!byte $05,$05,$05,$05,$33,$48,$49,$4a,$4b,$05,$33,$48,$49,$4a,$4c,$4d
!byte $4e,$48,$49,$4f,$50,$4d,$51,$48,$49,$4f,$50,$4d,$51,$48,$49,$4a
!byte $4c,$4d,$4e,$3d,$05,$05,$05,$05,$05,$05,$05,$05,$33,$52,$53,$54
!byte $4b,$05,$33,$52,$53,$54,$55,$56,$57,$52,$53,$58,$59,$5a,$5b,$52
!byte $53,$58,$59,$5a,$5b,$52,$53,$54,$55,$56,$57,$3d,$05,$05,$05,$05
!byte $05,$05,$05,$05,$33,$5c,$5d,$5e,$4b,$05,$33,$5c,$5d,$5e,$5f,$5d
!byte $60,$5c,$5d,$61,$62,$63,$64,$5c,$5d,$61,$62,$63,$64,$5c,$5d,$5e
!byte $5f,$5d,$60,$65,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05
!byte $05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05
!byte $05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05,$05
!byte $00,$00,$00,$00,$00,$00,$01,$02,$03,$04,$05,$06,$07,$08,$06,$09
!byte $0a,$06,$04,$05,$06,$09,$0b,$0c,$09,$0a,$06,$07,$0d,$06,$0e,$0f
!byte $10,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$11,$12
!byte $13,$14,$15,$16,$17,$18,$16,$19,$1a,$1b,$14,$15,$16,$17,$18,$16
!byte $19,$1a,$1b,$17,$1c,$1d,$1e,$1f,$00,$00,$00,$00,$00,$00,$00,$00
!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
!byte $00,$00

*= $3000


!byte $05,$05,$05,$05,$05,$05,$05,$05,$18,$3c,$66,$7e,$66,$66,$66,$00
!byte $7c,$66,$66,$7c,$66,$66,$7c,$00,$3c,$66,$60,$60,$60,$66,$3c,$00
!byte $78,$6c,$66,$66,$66,$6c,$78,$00,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff
!byte $ff,$ff,$ff,$ff,$ff,$fd,$fd,$f7,$ff,$fd,$f7,$df,$7c,$f3,$cf,$cc
!byte $d5,$7f,$f0,$0f,$f3,$cc,$03,$0c,$55,$ff,$03,$00,$fc,$fc,$33,$cf
!byte $ff,$5f,$f7,$3d,$0f,$c3,$f0,$c1,$ff,$ff,$ff,$ff,$7f,$df,$df,$f7
!byte $fd,$fd,$fd,$fd,$7d,$dd,$dd,$f5,$55,$ff,$c0,$f0,$ff,$ff,$ff,$fc
!byte $55,$ff,$00,$00,$fc,$cc,$3f,$c0,$55,$ff,$00,$00,$33,$cc,$03,$cc
!byte $55,$ff,$03,$00,$fc,$cc,$33,$cf,$55,$ff,$03,$03,$03,$c7,$13,$c7
!byte $55,$7f,$70,$7c,$7f,$7f,$7f,$7f,$55,$ff,$00,$00,$0f,$cc,$f3,$c0
!byte $55,$ff,$00,$01,$34,$f1,$34,$c0,$5f,$df,$df,$df,$df,$dd,$dd,$d7
!byte $f7,$f7,$df,$dc,$df,$df,$7c,$73,$30,$f0,$30,$c0,$c0,$c0,$c0,$00
!byte $31,$05,$04,$17,$17,$17,$17,$17,$73,$f0,$fc,$fc,$7f,$7f,$7c,$73
!byte $31,$cc,$31,$c0,$30,$00,$00,$00,$37,$77,$7d,$5d,$5d,$5d,$5f,$17
!byte $35,$75,$7d,$5d,$5d,$5d,$5f,$17,$f3,$cc,$fc,$fc,$cc,$f0,$fc,$fc
!byte $0c,$c0,$0c,$30,$03,$c0,$00,$00,$31,$05,$c4,$17,$17,$d7,$17,$17
!byte $0c,$c3,$0c,$00,$03,$00,$00,$00,$13,$07,$c7,$17,$17,$17,$17,$17
!byte $7f,$7f,$7f,$7f,$7f,$7f,$7c,$73,$30,$cc,$33,$c0,$00,$00,$00,$00
!byte $f1,$05,$05,$05,$35,$01,$04,$05,$d7,$d7,$df,$dc,$df,$df,$fc,$f3
!byte $ff,$ff,$ff,$ff,$ff,$ff,$7f,$7f,$ff,$ff,$ff,$fd,$fd,$fd,$fd,$fd
!byte $7f,$7f,$7f,$f3,$cc,$fc,$fd,$f0,$01,$04,$41,$04,$11,$44,$15,$44
!byte $17,$17,$17,$57,$17,$67,$1b,$67,$7c,$7f,$7f,$7c,$73,$7f,$ff,$00
!byte $10,$44,$01,$44,$11,$04,$11,$55,$17,$67,$1b,$67,$59,$5a,$19,$46
!byte $7c,$7f,$7f,$7f,$7c,$73,$7c,$70,$fc,$f0,$cc,$fc,$fc,$fc,$fd,$f0
!byte $05,$46,$19,$46,$59,$5a,$19,$46,$ff,$ff,$ff,$f3,$cc,$fc,$fd,$f0
!byte $7f,$7f,$7f,$df,$df,$df,$df,$df,$fd,$fd,$fd,$fd,$fd,$fd,$fd,$fd
!byte $cd,$f0,$cd,$c1,$c1,$c1,$d1,$c5,$55,$55,$55,$55,$65,$95,$65,$99
!byte $1b,$6b,$6b,$6b,$6b,$ab,$6b,$ab,$00,$05,$05,$45,$15,$49,$15,$49
!byte $54,$55,$55,$65,$59,$95,$66,$99,$5a,$5a,$5a,$9a,$6b,$ab,$6b,$af
!byte $70,$70,$70,$f0,$54,$15,$15,$49,$5a,$5a,$5a,$9a,$5a,$9a,$6a,$9a
!byte $70,$70,$70,$70,$74,$75,$71,$74,$df,$df,$df,$df,$df,$df,$df,$df
!byte $d2,$c1,$d2,$c5,$d2,$c5,$d2,$c6,$66,$aa,$6a,$a6,$6a,$aa,$aa,$aa
!byte $6b,$ab,$6b,$ab,$ab,$ab,$a7,$53,$16,$44,$11,$ff,$55,$7f,$7f,$7f
!byte $6a,$9a,$6b,$ff,$55,$ff,$ff,$ff,$ad,$bd,$f5,$5d,$fd,$fd,$fd,$fd
!byte $16,$44,$11,$f4,$71,$75,$75,$75,$6a,$99,$6a,$aa,$9a,$aa,$aa,$6a
!byte $5a,$9a,$6a,$9a,$6a,$aa,$aa,$aa,$71,$74,$71,$74,$71,$75,$75,$75
!byte $d6,$d6,$d6,$d6,$c6,$c1,$f3,$ff,$a9,$a9,$a7,$a7,$5f,$fc,$f1,$10
!byte $c3,$c3,$c7,$1b,$27,$5b,$27,$5b,$7f,$7f,$7f,$7f,$7f,$7f,$7f,$7f
!byte $74,$73,$7f,$7c,$70,$70,$70,$70,$da,$f6,$36,$3d,$0d,$4f,$13,$41
!byte $aa,$aa,$aa,$a9,$a4,$50,$f0,$f2,$c3,$c3,$c7,$1b,$27,$5b,$24,$40
!byte $74,$73,$7f,$7c,$70,$f0,$c0,$c1,$aa,$aa,$aa,$aa,$ab,$63,$c3,$c7
!byte $cc,$c1,$c1,$c1,$c6,$d1,$d6,$d6,$45,$11,$54,$95,$56,$99,$66,$a9
!byte $67,$6b,$6b,$ab,$6b,$ab,$ab,$ab,$70,$70,$70,$70,$71,$74,$71,$75
!byte $54,$51,$45,$95,$65,$99,$a6,$9a,$49,$16,$5a,$9a,$6a,$9a,$6a,$aa
!byte $10,$44,$54,$99,$66,$99,$a6,$aa,$01,$04,$51,$65,$56,$a9,$9a,$6a
!byte $14,$51,$45,$95,$65,$9a,$a6,$9a,$9b,$6f,$9d,$ad,$ad,$bd,$b5,$b5
!byte $d6,$d6,$d6,$d6,$d5,$d5,$ff,$55,$a6,$aa,$aa,$aa,$55,$55,$ff,$55
!byte $ab,$ab,$ab,$ab,$6b,$5b,$ff,$55,$75,$75,$75,$75,$75,$75,$7f,$55
!byte $aa,$aa,$aa,$aa,$5a,$56,$ff,$55,$9a,$aa,$aa,$aa,$55,$55,$ff,$55
!byte $a6,$aa,$aa,$aa,$55,$57,$ff,$55,$aa,$aa,$ab,$af,$7d,$f7,$5f,$ff
!byte $f5,$dd,$dd,$7d,$fd,$fd,$fd,$fd,$df,$df,$df,$df,$df,$df,$df,$5f
!byte $00,$00,$00,$00,$08,$08,$09,$1f,$00,$00,$00,$00,$00,$20,$f8,$4c
!byte $00,$00,$00,$00,$00,$00,$60,$c0,$1c,$38,$7c,$cc,$0e,$06,$07,$03
!byte $06,$06,$0c,$0c,$18,$71,$c3,$86,$00,$00,$00,$00,$00,$f0,$f8,$10
!byte $e0,$c0,$60,$30,$18,$3e,$7f,$a6,$00,$00,$00,$00,$00,$00,$08,$10
!byte $00,$00,$00,$00,$00,$00,$61,$c3,$00,$00,$00,$00,$00,$00,$f4,$38
!byte $07,$0d,$10,$40,$00,$00,$00,$00,$8d,$8e,$cc,$66,$33,$19,$04,$02
!byte $19,$09,$0d,$04,$13,$ff,$79,$00,$83,$81,$a1,$c0,$70,$39,$8f,$40
!byte $30,$30,$b0,$98,$6e,$e7,$31,$08,$66,$27,$36,$13,$1d,$3c,$e6,$01
!byte $0c,$04,$86,$02,$c3,$e7,$3c,$00,$00,$00,$00,$00,$80,$80,$c0,$21
!byte $00,$00,$00,$00,$00,$00,$90,$9b,$80,$00,$00,$00,$04,$08,$19,$12
!byte $10,$00,$00,$00,$10,$1c,$b0,$55,$02,$00,$00,$00,$00,$48,$0c,$a7
!byte $00,$00,$00,$00,$00,$30,$c6,$0d,$00,$00,$00,$00,$00,$0c,$10,$26
!byte $08,$00,$00,$10,$08,$0c,$04,$06,$02,$00,$00,$00,$00,$00,$00,$c3
!byte $c3,$99,$91,$91,$9f,$99,$c3,$ff,$e7,$c3,$99,$81,$99,$99,$99,$ff
!byte $83,$99,$99,$83,$99,$99,$83,$ff,$c3,$99,$9f,$9f,$9f,$99,$c3,$ff
!byte $87,$93,$99,$99,$99,$93,$87,$ff,$00,$00,$00,$04,$02,$03,$01,$11
!byte $00,$00,$00,$02,$11,$09,$08,$98,$00,$00,$00,$00,$00,$80,$80,$c0
!byte $0c,$01,$03,$06,$00,$00,$00,$00,$96,$9a,$0b,$09,$00,$00,$00,$00
!byte $53,$11,$19,$09,$00,$00,$00,$00,$c4,$37,$8a,$85,$80,$00,$00,$00
!byte $bd,$12,$62,$cc,$00,$00,$00,$00,$07,$02,$03,$03,$00,$00,$00,$00
!byte $26,$34,$66,$c3,$00,$00,$00,$00,$bd,$50,$59,$ae,$00,$00,$00,$00
!byte $b0,$e0,$b0,$88,$04,$00,$01,$00,$40,$00,$20,$10,$00,$00,$00,$00
!byte $83,$99,$99,$83,$87,$93,$99,$ff,$c3,$99,$9f,$c3,$f9,$99,$c3,$ff
!byte $81,$e7,$e7,$e7,$e7,$e7,$e7,$ff,$99,$99,$99,$99,$99,$99,$c3,$ff
!byte $99,$99,$99,$99,$99,$c3,$e7,$ff,$9c,$9c,$9c,$94,$80,$88,$9c,$ff
!byte $99,$99,$c3,$e7,$c3,$99,$99,$ff,$99,$99,$99,$c3,$e7,$e7,$e7,$ff
!byte $81,$f9,$f3,$e7,$cf,$9f,$81,$ff,$c3,$cf,$cf,$cf,$cf,$cf,$c3,$ff
!byte $f3,$ed,$cf,$83,$cf,$9d,$03,$ff,$c3,$f3,$f3,$f3,$f3,$f3,$c3,$ff
!byte $ff,$e7,$c3,$81,$e7,$e7,$e7,$e7,$ff,$ef,$cf,$80,$80,$cf,$ef,$ff
!byte $ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$e7,$e7,$e7,$e7,$ff,$ff,$e7,$ff
!byte $99,$99,$99,$ff,$ff,$ff,$ff,$ff,$99,$99,$00,$99,$00,$99,$99,$ff
!byte $e7,$c1,$9f,$c3,$f9,$83,$e7,$ff,$9d,$99,$f3,$e7,$cf,$99,$b9,$ff
!byte $c3,$99,$c3,$c7,$98,$99,$c0,$ff,$f9,$f3,$e7,$ff,$ff,$ff,$ff,$ff
!byte $f3,$e7,$cf,$cf,$cf,$e7,$f3,$ff,$cf,$e7,$f3,$f3,$f3,$e7,$cf,$ff
!byte $ff,$99,$c3,$00,$c3,$99,$ff,$ff,$ff,$e7,$e7,$81,$e7,$e7,$ff,$ff
!byte $ff,$ff,$ff,$ff,$ff,$e7,$e7,$cf,$ff,$ff,$ff,$81,$ff,$ff,$ff,$ff
!byte $ff,$ff,$ff,$ff,$ff,$e7,$e7,$ff,$ff,$fc,$f9,$f3,$e7,$cf,$9f,$ff
!byte $c3,$99,$91,$89,$99,$99,$c3,$ff,$e7,$e7,$c7,$e7,$e7,$e7,$81,$ff
!byte $c3,$99,$f9,$f3,$cf,$9f,$81,$ff,$c3,$99,$f9,$e3,$f9,$99,$c3,$ff
!byte $f9,$f1,$e1,$99,$80,$f9,$f9,$ff,$81,$9f,$83,$f9,$f9,$99,$c3,$ff
!byte $c3,$99,$9f,$83,$99,$99,$c3,$ff,$81,$99,$f3,$e7,$e7,$e7,$e7,$ff
!byte $c3,$99,$99,$c3,$99,$99,$c3,$ff,$c3,$99,$99,$c1,$f9,$99,$c3,$ff
!byte $ff,$ff,$e7,$ff,$ff,$e7,$ff,$ff,$ff,$ff,$e7,$ff,$ff,$e7,$e7,$cf
!byte $f1,$e7,$cf,$9f,$cf,$e7,$f1,$ff,$ff,$ff,$81,$ff,$81,$ff,$ff,$ff
!byte $8f,$e7,$f3,$f9,$f3,$e7,$8f,$ff,$c3,$99,$f9,$f3,$e7,$ff,$e7,$ff
!byte $ff,$ff,$ff,$00,$00,$ff,$ff,$ff,$f7,$e3,$c1,$80,$80,$e3,$c1,$ff
!byte $e7,$e7,$e7,$e7,$e7,$e7,$e7,$e7,$ff,$ff,$ff,$00,$00,$ff,$ff,$ff
!byte $ff,$ff,$00,$00,$ff,$ff,$ff,$ff,$ff,$00,$00,$ff,$ff,$ff,$ff,$ff
!byte $ff,$ff,$ff,$ff,$00,$00,$ff,$ff,$cf,$cf,$cf,$cf,$cf,$cf,$cf,$cf
!byte $f3,$f3,$f3,$f3,$f3,$f3,$f3,$f3,$ff,$ff,$ff,$1f,$0f,$c7,$e7,$e7
!byte $e7,$e7,$e3,$f0,$f8,$ff,$ff,$ff,$e7,$e7,$c7,$0f,$1f,$ff,$ff,$ff
!byte $3f,$3f,$3f,$3f,$3f,$3f,$00,$00,$3f,$1f,$8f,$c7,$e3,$f1,$f8,$fc
!byte $fc,$f8,$f1,$e3,$c7,$8f,$1f,$3f,$00,$00,$3f,$3f,$3f,$3f,$3f,$3f
!byte $00,$00,$fc,$fc,$fc,$fc,$fc,$fc,$ff,$c3,$81,$81,$81,$81,$c3,$ff
!byte $ff,$ff,$ff,$ff,$ff,$00,$00,$ff,$c9,$80,$80,$80,$c1,$e3,$f7,$ff
!byte $9f,$9f,$9f,$9f,$9f,$9f,$9f,$9f,$ff,$ff,$ff,$f8,$f0,$e3,$e7,$e7
!byte $3c,$18,$81,$c3,$c3,$81,$18,$3c,$ff,$c3,$81,$99,$99,$81,$c3,$ff
!byte $e7,$e7,$99,$99,$e7,$e7,$c3,$ff,$f9,$f9,$f9,$f9,$f9,$f9,$f9,$f9
!byte $f7,$e3,$c1,$80,$c1,$e3,$f7,$ff,$e7,$e7,$e7,$00,$00,$e7,$e7,$e7
!byte $3f,$3f,$cf,$cf,$3f,$3f,$cf,$cf,$e7,$e7,$e7,$e7,$e7,$e7,$e7,$e7
!byte $ff,$ff,$fc,$c1,$89,$c9,$c9,$ff,$00,$80,$c0,$e0,$f0,$f8,$fc,$fe
!byte $ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$0f,$0f,$0f,$0f,$0f,$0f,$0f,$0f
!byte $ff,$ff,$ff,$ff,$00,$00,$00,$00,$00,$ff,$ff,$ff,$ff,$ff,$ff,$ff
!byte $ff,$ff,$ff,$ff,$ff,$ff,$ff,$00,$3f,$3f,$3f,$3f,$3f,$3f,$3f,$3f
!byte $33,$33,$cc,$cc,$33,$33,$cc,$cc,$fc,$fc,$fc,$fc,$fc,$fc,$fc,$fc
!byte $ff,$ff,$ff,$ff,$33,$33,$cc,$cc,$00,$01,$03,$07,$0f,$1f,$3f,$7f
!byte $fc,$fc,$fc,$fc,$fc,$fc,$fc,$fc,$e7,$e7,$e7,$e0,$e0,$e7,$e7,$e7
!byte $ff,$ff,$ff,$ff,$f0,$f0,$f0,$f0,$e7,$e7,$e7,$e0,$e0,$ff,$ff,$ff
!byte $ff,$ff,$ff,$07,$07,$e7,$e7,$e7,$ff,$ff,$ff,$ff,$ff,$ff,$00,$00
!byte $ff,$ff,$ff,$e0,$e0,$e7,$e7,$e7,$e7,$e7,$e7,$00,$00,$ff,$ff,$ff
!byte $ff,$ff,$ff,$00,$00,$e7,$e7,$e7,$e7,$e7,$e7,$07,$07,$e7,$e7,$e7
!byte $3f,$3f,$3f,$3f,$3f,$3f,$3f,$3f,$1f,$1f,$1f,$1f,$1f,$1f,$1f,$1f
!byte $f8,$f8,$f8,$f8,$f8,$f8,$f8,$f8,$00,$00,$ff,$ff,$ff,$ff,$ff,$ff
!byte $00,$00,$00,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$00,$00,$00
!byte $fc,$fc,$fc,$fc,$fc,$fc,$00,$00,$ff,$ff,$ff,$ff,$0f,$0f,$0f,$0f
!byte $f0,$f0,$f0,$f0,$ff,$ff,$ff,$ff,$e7,$e7,$e7,$07,$07,$ff,$ff,$ff
!byte $0f,$0f,$0f,$0f,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff,$ff
!byte $ff,$ff
	
