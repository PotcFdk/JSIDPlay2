
	.pc=$0801

	.word basic
	.word 0000
	.byte $9e
	.text "2061"
	.byte 0
basic:	.word 0

	ldx #$05
loop:
	lda l1,X
	sta $0277,X
	dex
	bpl loop

	lda #$06
	sta $C6
	lda #$03
	sta $0331
	lda #$3C
	sta $0330

	ldx #$2A
loop2:
	lda prg,X
	sta $0102,X
	dex
	bpl loop2

	ldx #$15
loop3:
	lda prg2-1,X
	sta $033B,X
	dex
	bne loop3
	ldx #$04
loop4:
	lda prg3,X
	sta $03FB,X
	dex
	bne loop4
	rts

prg:
	ldy #$00
	sty $C0
	lda $D011
	and #$EF
	sta $D011
loop5:
	dex
	bne loop5
	dey
	bne loop5
	sei
	rts

	lda #$10
loop6:
	bit $DC0D
	beq loop6
	lda $DD0D
	stx $DD07
	pha
	lda #$19
	sta $DD0F
	pla
	lsr
	lsr
	rts

prg2:
	sta $90
loop7:
	jsr $035D
	lda $AB
	cmp #$02
	beq loop8
	cmp #$01
	bne loop7
loop8:
	jsr $0384
	lda $BD

prg3:
	eor $F4
	lda $BD
	rts

l1:
.byte $4C, $CF, $0D
.byte $52
.byte $D5, $0D
