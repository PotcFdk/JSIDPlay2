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
	ldx #$BF
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
	jsr t4
	lsr
	bcc loop3
	jsr t4
	lsr
	bcc loop4
	iny
	jsr t2
	sta $2d
	cmp #1<<cmdLineVars.get("ftMaxGamma").asNumber()
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