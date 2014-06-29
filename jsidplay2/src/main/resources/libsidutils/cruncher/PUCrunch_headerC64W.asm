/*
java -jar "d:\Downloads\C64\C64 Kickassembler\KickAss.jar" -binfile d:\workspace\jsidplay2\src\main\resources\libsidutils\cruncher\PUCrunch_headerC64W.asm :ftFastDisable=238 :ftBEndHi=170 :ftOverlap=0 :ftOverlapAddr=43690 :ftStackSize=191 :ftReloc=8 :ftSizePages=170 :ftSizeAddr= :ftSizeAddr=43690 :ftEndAddr=65280 :ftEscValue=0 :ftOutposAddr=43690 :ftEscBits=02 :ftEsc8Bits=06 :ftEscBits2=02 :ft1MaxGamma=64 :ft8MaxGamma=02 :ft2MaxGamma=127 :ftExtraBits=0 :ftMemConfig=55 :ftCli=88 :ftExec=43690 :ftInpos=43690 :ftMaxGamma=7 :ftIBufferSize=59 :ftFastDisable=1
*/
	.pc=$0801

	.word basic
	.word 239
	.byte $9e
	.text "2061"
	.byte 0
basic:	.word 0
	sei
	.if (cmdLineVars.get("ftFastDisable").asNumber()==1) inc $d030 else bit $d030
	lda #$38
	sta $01
	ldx #cmdLineVars.get("ftOverlap").asNumber()
!:
	lda.absx cmdLineVars.get("ftOverlapAddr").asNumber()+end
	sta $4b,x
	dex
	bpl !-
.label ftIBufferSize=*+1
	ldx #$3B
!:
	lda.absx [<data3-1] | [cmdLineVars.get("ftReloc").asNumber() << 8]
	sta target3-1,x
	dex
	bne !-
.label ftStackSize=*+1
	ldx #$BF
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

data3:
.pseudopc $0200 {
target3:
	pha
	lda.abs cmdLineVars.get("ftInpos").asNumber()
	rol
	sta.zp mem3
	inc target3+2
	bne !+
	inc target3+3
	bne !+
	lda #$4b
	sta target3+2
!:
	pla
	rts
t2:
	inx
	txa
l1:
	asl.zp mem3
	bne !+
	jsr target3
!:
	bcc !+
	inx
	cpx #cmdLineVars.get("ftMaxGamma").asNumber()
	bne l1
	beq !+
t3:
	ldx #$07
t4:
	inx
t5:
	asl.zp mem3
	bne l2
	jsr target3
l2:
	rol
!:
	dex
t1:
	bne t5
	clc
	rts
}

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
	sta $2d
	lsr
	bne loop
	jsr t4
	lsr
	bcc loop3
	jsr t4
	lsr
	bcc loop4
	iny
	jsr t2
	sta $2d
	cmp #cmdLineVars.get("ft1MaxGamma").asNumber()
	bcc !+
	ldx #cmdLineVars.get("ft8MaxGamma").asNumber()
	jsr t5
	sta $2d
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
	jsr t5
!:
	ldx $2d
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
	sta $2e
	jsr t3
	adc target2+1
	ldx $2d
	sta $2d
	lda target2+2
	sbc $2e
	sta $2e
	inx
!:
	lda ($2d),y
	iny
	jsr target2
	bne !-
	beq loop7
loop9:
	lda #cmdLineVars.get("ftMemConfig").asNumber()
	sta $01
	.if (cmdLineVars.get("ftFastDisable").asNumber()==1) dec $d030 else bit $d030
	lda target2+1
	sta $2d
	lda target2+2
	sta $2e
	.byte cmdLineVars.get("ftCli").asNumber()
	jmp.abs cmdLineVars.get("ftExec").asNumber()
data2:
.byte 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
}
end: