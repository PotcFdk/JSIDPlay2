header:
	.pc=cmdLineVars.get("start").asNumber()-26

.byte $01
.word cmdLineVars.get("start").asNumber()
.word cmdLineVars.get("end").asNumber()
.text cmdLineVars.get("name")

.byte $AB, $AB, $AB, $AB, $AB

prg:
.byte $AB, $AB
.byte $AB, $AB
.byte $AB, $AB,$AB
loop:
.byte $AB, $AB,$AB
.byte $AB, $AB
.byte $AB, $AB
.byte $AB, $AB
.byte $AB, $AB,$AB
.byte $AB, $AB
.byte $AB, $AB,$AB
.byte $AB, $AB
.byte $AB

.byte $ED, $03
	sta $AE
	jsr $03ED
	sta $AF
	ldy #$BC
loop2:
	jsr $03ED
	dey
	bne loop2
	beq loop3
	jsr $03BF
loop8:
	jsr $03ED
	sty $93
	pha
	lda #$04
	sta $01
	pla
	sta ($C3),Y
	eor $D7
	sta $D7
	lda #$07
	sta $01
	inc $C3
	bne loop4
	inc $C4
loop4:
	lda $C3
	cmp $AE
	lda $C4
	sbc $AF
	bcc loop8
	jsr $03ED
	jsr $0102
	iny
loop3:
	sty $C0
	cli
	clc
	lda #$00
	sta $02A0
	jmp $FC93
	jsr $F817
	jsr $0102
	sty $D7
	lda #$07
	sta $DD06
	ldx #$01
loop5:
	jsr $0116
	rol $BD
	lda $BD
	cmp #$02
	bne loop5
	ldy #$09
loop6:
	jsr $03ED
	cmp #$02
	beq loop6
loop7:
	cpy $BD
	bne loop5
end:
	jsr $03ED
	dey
	bne loop7
	rts
	lda #$08
	sta $A3
	jsr $0116
	rol $BD
	inc $D020
	dec $A3
.byte $D0
