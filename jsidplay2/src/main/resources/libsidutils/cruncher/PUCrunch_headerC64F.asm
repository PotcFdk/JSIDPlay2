	.pc=cmdLineVars.get("pc").asNumber()

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
.label ftIBufferSize=*+1
	ldx #$34
!:
	lda data3-1,x
	sta target3-1,x
	dex
	bne !-
.label ftStackSize=*+1
	ldx #$d6
!:
	lda data-1,x
	sta mem3-1,x
	dex
	bne !-
	ldy #cmdLineVars.get("ftSizePages").asNumber()
!:
	dex
src:
	lda.absx cmdLineVars.get("ftSizeAddr").asNumber()+end
dst:
	sta.absx cmdLineVars.get("ftEndAddr").asNumber()
	txa
	bne !-
	dec dst+2,x
	dec src+2,x
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
	cpx #cmdLineVars.get("ftMaxGamma").asNumber()+1
	bne l1
	beq !+
t3:
	ldx #$07
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
	beq !+
	dex
	rts
!:
	inc target2+2
	dex
	rts
loop4:
	ldy mem
	ldx #cmdLineVars.get("ftEscBits").asNumber()
	jsr t1
	sta mem
	tya
!:
	ldx #8-cmdLineVars.get("ftEscBits").asNumber()
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
	asl mem3
	bne !+
	jsr target3
!:
	bcc loop2
	asl mem3
	bne !+
	jsr target3
!:
	bcc loop4
	iny
	jsr t2
	sta $2d
	cmp #1 << cmdLineVars.get("ftMaxGamma").asNumber()
	bcc !+
	ldx #8-cmdLineVars.get("ftMaxGamma").asNumber()
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
	beq start
loop:
	jsr t2
	cmp #[2 << cmdLineVars.get("ftMaxGamma").asNumber()]-1
	beq !+
	sbc #$00
	ldx #$00
	jsr t1
loop2:
	sta $2e
	jsr t3
	adc target2+1
	ldx $2d
	sta src2+1
	lda target2+2
	sbc $2e
	sta src2+2
	inx
src2:
	lda $aaaa,y
	sta (target2+1),y
	iny
	dex
	bne src2
	dey
	tya
	sec
	adc target2+1
	sta target2+1
	bcc loop9
	inc target2+2
loop9:
	jmp start
!:
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
.byte cmdLineVars.get("rleValue1").asNumber()
.byte cmdLineVars.get("rleValue2").asNumber()
.byte cmdLineVars.get("rleValue3").asNumber()
.byte cmdLineVars.get("rleValue4").asNumber()
.byte cmdLineVars.get("rleValue5").asNumber()
.byte cmdLineVars.get("rleValue6").asNumber()
.byte cmdLineVars.get("rleValue7").asNumber()
.byte cmdLineVars.get("rleValue8").asNumber()
.byte cmdLineVars.get("rleValue9").asNumber()
.byte cmdLineVars.get("rleValue10").asNumber()
.byte cmdLineVars.get("rleValue11").asNumber()
.byte cmdLineVars.get("rleValue12").asNumber()
.byte cmdLineVars.get("rleValue13").asNumber()
.byte cmdLineVars.get("rleValue14").asNumber()
.byte cmdLineVars.get("rleValue15").asNumber()
}
end: