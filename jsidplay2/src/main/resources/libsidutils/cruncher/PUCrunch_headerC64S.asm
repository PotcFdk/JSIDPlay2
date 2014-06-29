/*
java -jar "d:\Downloads\C64\C64 Kickassembler\KickAss.jar" -binfile d:\workspace\jsidplay2\src\main\resources\libsidutils\cruncher\PUCrunch_headerC64S.asm :ftBEndHi=170 :ftOverlap=0 :ftOverlapAddr=43690 :ftStackSize=224 :ftReloc=8 :ftSizePages=170 :ftSizeAddr= :ftSizeAddr=43690 :ftEndAddr=65280 :ftEscValue=0 :ftOutposAddr=43690 :ftEscBits=02 :ftEsc8Bits=06 :ftEscBits2=02 :ft1MaxGamma=64 :ft8MaxGamma=02 :ft2MaxGamma=127 :ftExtraBits=0 :ftMemConfig=55 :ftCli=88 :ftExec=43690 :ftInpos=43690 :ftMaxGamma=7
*/
	.pc=$0801

	.word basic
	.word 239
	.byte $9e
	.text "2061"
	.byte 0
basic:	.word 0
	sei
	lda #$38
	sta $01
	lda #$aa
	sta $2d
	lda #cmdLineVars.get("ftBEndHi").asNumber()
	sta $2e
.label ftStackSize=*+1
	ldx #$E0
!:
	lda.absx [<data-1] | [cmdLineVars.get("ftReloc").asNumber() << 8]
	sta mem3-1,x
	dex
	bne !-
	ldy #cmdLineVars.get("ftSizePages").asNumber()
!:
	dex
src:
	lda.absx cmdLineVars.get("ftSizeAddr").asNumber()+end
tgt:
	sta.absx cmdLineVars.get("ftEndAddr").asNumber()
	txa
	bne !-
	dec.abs [<tgt+2] | [cmdLineVars.get("ftReloc").asNumber() << 8]
	dec.abs [<src+2] | [cmdLineVars.get("ftReloc").asNumber() << 8]
	dey
	bne !-
ftDeCall:
	jmp start
	
data:
.pseudopc $f7 {
mem3:
.byte $80
mem:
.byte cmdLineVars.get("ftEscValue").asNumber()
target2:
	sta.abs cmdLineVars.get("ftOutposAddr").asNumber()
	inc target2+1
	bne !+
	inc target2+2
!:
	dex
	rts
loop4:
	ldy mem
	ldx #cmdLineVars.get("ftEscBits").asNumber()
	jsr t1
	sta mem
	tya
!:
	ldx #cmdLineVars.get("ftEsc8Bits").asNumber()
	jsr t1
	jsr target2
start:
	ldy #$00
	tya
	ldx #cmdLineVars.get("ftEscBits").asNumber()
	jsr t1
	cmp mem
	bne !-
	jsr t2
	sta $2f
	lsr
	bne loop
	jsr loop2
	bcc loop3
	jsr loop2
	bcc loop4
	iny
	jsr t2
	sta $2f
	cmp #cmdLineVars.get("ft1MaxGamma").asNumber()
	bcc !+
	ldx #cmdLineVars.get("ft8MaxGamma").asNumber()
	jsr loop5
	sta $2f
	jsr t2
	tay
!:
	jsr t2
	tax
	lda data2-1,x
	cpx #$10
	bcc !+
	txa
	ldx #$04
	jsr loop5
!:
	ldx $2f
	inx
!:
	jsr target2
	bne !-
	dey
	bne !-
loop7:
	beq start
loop:
	jsr t2
	cmp #cmdLineVars.get("ft2MaxGamma").asNumber()
	beq loop9
	sbc #$00
	ldx #cmdLineVars.get("ftExtraBits").asNumber()
	jsr t1
loop3:
	sta $30
	ldx #$08
	jsr loop5
	adc target2+1
	ldx $2f
	sta $2f
	lda target2+2
	sbc $30
	sta $30
	inx
!:
	lda ($2f),y
	iny
	jsr target2
	bne !-
	beq loop7
loop9:
	lda #cmdLineVars.get("ftMemConfig").asNumber()
	sta $01
	.byte cmdLineVars.get("ftCli").asNumber()
	jmp.abs cmdLineVars.get("ftExec").asNumber()
loop2:
	asl mem3
	bne loop8
	pha
target3:
	lda.abs cmdLineVars.get("ftInpos").asNumber()
	rol
	sta mem3
	inc target3+1
	bne !+
	inc target3+2
!:
	pla
loop8:
	rts
t2:
	inx
	txa
!:
	jsr loop2
	bcc !+
	inx
	cpx #cmdLineVars.get("ftMaxGamma").asNumber()
	bne !-
	beq !+
loop5:
	jsr loop2
	rol
!:
	dex
t1:
	bne loop5
	clc
	rts
data2:
.byte 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
}
end: