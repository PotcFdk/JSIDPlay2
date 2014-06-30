	.pc=cmdLineVars.get("pc").asNumber()

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
	lda data-1,x
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
	dec tgt+2
	dec src+2
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
	cmp #1 << cmdLineVars.get("ftMaxGamma").asNumber()
	bcc !+
	ldx #8-cmdLineVars.get("ftMaxGamma").asNumber()
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
	cmp #[2 << cmdLineVars.get("ftMaxGamma").asNumber()]-1
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
	cpx #cmdLineVars.get("ftMaxGamma").asNumber()+1
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