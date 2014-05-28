.var MAX_BLOCKS = 4

	.pc=$0801

	.word basic
	.word 2004
	.byte $9e
	.text "2061"
	.byte 0
basic:	.word 0

	lda #cmdLineVars.get("songNum").asNumber()
	jmp start

.var r1_src	= *

// Parameters
src:	.word end+cmdLineVars.get("size").asNumber()		// $00f7
dest:	.word cmdLineVars.get("dst").asNumber()				// $00f9
counter:.byte cmdLineVars.get("numPages").asNumber()		// $00fb
zp:		.word cmdLineVars.get("startAfterMoving").asNumber()// $00fc
numblk:	.byte cmdLineVars.get("numBlocks").asNumber()		// $00fe
chars:	.byte cmdLineVars.get("charPage").asNumber()		// $00ff
player:	.word cmdLineVars.get("driverPage").asNumber()		// $0100
stopvec:.word cmdLineVars.get("stopVec").asNumber()			// $0102
blocks:	.fill 4*MAX_BLOCKS, 0								// $0104

memmove:	{
	lda $102
	sta $0328
	lda $102+1
	sta $0329
	// Move all blocks to highest memory area
	ldy #0
l1:	dec $f7+1
	dec $f9+1
l2:	lda ($f7),y
	sta ($f9),y
	iny
	bne l2
	dec $fb
	bne l1

	// Move the blocks to the correct locations
	ldx $fe
loop:	lda $0104,x
	sta $f9
	lda $0104+MAX_BLOCKS,x
	sta $f9+1
	ldy #0
	lda $0104+3*MAX_BLOCKS,x
	beq skip1
	sta $fb
copy1:	lda ($fc),y
	sta ($f9),y
	iny
	bne copy1
	inc $fc+1
	inc $f9+1
	dec $fb
	bne copy1
skip1:	lda $0104+2*MAX_BLOCKS,x
	beq skip2
	sta $fb
copy2:	lda ($fc),y
	sta ($f9),y
	iny
	cpy $fb
	bne copy2
	tya
	clc
	adc $fc
	sta $fc
	bcc skip2
	inc $fc+1
skip2:	dex
	bpl loop

	// Copy character ROM data for screens $4000-$8000 or $C000-$D000
	lda $ff
	beq nochars
	sta $f9+1
	lda #$d8
	sta $fc+1
	ldy #0
	sty $fc
	sty $f9
	ldx #7
	dec 1				// $33
copy3:	lda ($fc),y
	sta ($f9),y
	iny
	bne copy3
	inc $fc+1
	inc $f9+1
	dex
	bpl copy3

nochars:	lda #$37
	sta 1
	jmp ($100)
	}
.var r1_len	= *-r1_src

start:	{
	sei
	ldx #$ff
	txs
	cld
	pha				// Store default song number
	lda $02a6
	and #1
	pha				// Store pal/ntsc flag
	lda $a2
	pha				// Store seed for random number generator

	lda #$37
	sta 1
	jsr $ff84

	// Initialize VIC-II chip
	ldx #$2d
vicinit:	lda vicdata,x
	sta $d000,x
	dex
	bpl vicinit

	// Initialize color memory
	ldy #0
	tya
colinit:	sta $d800,y
	sta $d900,y
	sta $da00,y
	sta $db00,y
	iny
	bne colinit

	ldx #29				// set colors of program name
	lda #15
prgcol:	sta $d800+45,x
	dex
	bpl prgcol
	ldx #14
l1:	lda linecol,x
	sta $d804,x
	dex
	bpl l1

	ldy #240			// set color of information lines
l2:	ldx #39
	lda #1
l3:	cpx #7
	beq l4
	bcs l5
	lda #13
	.byte $2c			// bit $xxxx
l4:	lda #7
l5:	dey
	sta $d800+160,y
	sta $d800+280,y
	dex
	bpl l3
	tya
	bne l2

	lda #12				// set color of area for STIL text
stilcol:	sta $d800+520,y
	iny
	bne stilcol

	lda #$34
	sta 1
	ldy #0
copyr1:	lda r1_src,y
	sta $f7,y
	iny
	cpy #r1_len
	bne copyr1
	jmp $0114


vicdata:.byte $00,$00,$00,$00,$00,$00,$00,$00
	.byte $00,$00,$00,$00,$00,$00,$00,$00
	.byte $00,$6b,$37,$00,$00,$00,$08,$00
	.byte $14,$0f,$00,$00,$00,$00,$00,$00
	.byte $00,$00,$01,$02,$03,$04,$00,$01
	.byte $02,$03,$04,$05,$06,$07

linecol:	.byte 9,11,8,12,10,15,7,1,13,7,3,12,14,4,6

	}
end: