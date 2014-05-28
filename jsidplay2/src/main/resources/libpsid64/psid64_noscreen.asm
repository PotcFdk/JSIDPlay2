.pc = cmdLineVars.get("pc").asNumber()

	.align 256

	jmp cold
	jmp setiomap

	// Parameters
init:	rts
	.word 0
play:	rts
	.word 0
playmax:	.byte 0
speed:	.word 0, 0
irqvec:	.byte 0
initiomap: .byte 0
playiomap: .byte 0

	// Variables
playnum:	.byte 0
video:	.byte 0
random:	.byte 0

	//Constants

	// User interrupt vectors
irqusr:	.word irqret
brkusr:	.word brkjob
nmiusr:	.word nmijob

	// IRQ handler
irqjob:	lda $d020			// save values
	pha
	lda $dc00
	pha
noshift:	lda $01
	pha
	lda playiomap
	sta $01
	lda #0
	jsr play
	pla
	sta $01
	lda #$7f			// test for arrow left key
	sta $dc00
	lda $dc01
	and #2
	bne done
ffwd:	inc $d020			// flash $d020 during fast forward
	jmp noshift
done:	pla
	sta $dc00
	pla
	sta $d020
	lda $d019
	sta $d019
irqret:	lda $dc0d
	pla
	tay
	pla
	tax
	pla
nmijob:	rti

	// Cold start
	// Parameters passed in from boot loader
cold:	pla
	sta random	// seed for random number generator
	pla
	sta video	// pal/ntsc flag
	pla
start:	sta playnum	// song number
restart:	sei
	cld
	ldx #$ff
	txs
	lda $01		// enable I/O without changing basic and kernal rom bank
	and #3
	bne haveio
	lda #$01
haveio:	ora #$34
	sta $01
	jsr stop

	// Set interrupt vectors
	ldx irqvec
	bmi vicras
store03:	lda irqusr,x
	sta $0314,x
	dex
	bpl store03

	// Set VICII raster to line 0
vicras:
	lda #$0b
	ldx #$00
	sta $d011
	stx $d012

	// Set CIA 1 Timer A to 60Hz
	lda video
	beq ntsc
pal:	lda #$25
	ldx #$40
	bne timer
ntsc:	lda #$95
	ldx #$42
timer:	sta $dc04
	stx $dc05

	// Get song number
	ldy playnum
	cpy playmax
	bcc songset
	ldy #$00
songset:	tya
	pha

	// RSIDs use default environment of CIA1 only
        lda initiomap
        beq ciainit

	// Get shift number for speed bit
	cpy #32
	bcc shftset
	ldy #31

	// Find speed for current song number
shftset:	lda #$00
	tax
	sec
shift:	rol
	bcc nxtshft
	rol
	inx
nxtshft:	dey
	bpl shift
	and speed,x
	bne ciainit

	// Enable VICII raster interrupt
vicinit:	lda #$81
	sta $d01a
	bne doinit

	// Enable CIA 1 timer A interrupt
ciainit:	lda #$81
	sta $dc0d

	// always enable timer A for random numbers
doinit:	lda #$01
	sta $dc0e

	// If play address, override default irq vector so
	// we reach our routine to handle play routine
	lda playiomap
	beq noplay
	lda #<irqjob
	sta $0314

	// Set I/O map and call song init routine
noplay:	lda #$2f
	sta $00
	lda initiomap
	bne setbank
	// Only release interrupt mask for real
	// C64 tunes (initiomap = 0) thus
	// providing a more realistic environment
	lda #$37
	// cli dosen't come into effect until
	// after the sta!
	cli
setbank:	sta $01

	// get song number
	pla
	ldx random
rndwait:	ldy #0
	dex
	bne rndwait
	jsr init
setiomap:
	lda initiomap
	beq idle
	lda playiomap
	beq run
	lda #$37
	sta $01
	bne run

brkjob:	ldx #$ff
	txs
run:	cli
idle:	jsr delay
	php
	sei
	jsr keyboard
	plp
	jmp idle

stop:	{
	ldx #0				// stop NMI and IRQ interrupts
	stx $d01a
	lda $d019
	sta $d019
	lda #$7f
	sta $dc0d
	sta $dd0d
	lda $dc0d
	lda $dd0d
	lda #8
	sta $dc0e
	sta $dd0e
	sta $dc0f
	sta $dd0f
	sta $d404			// set test bit to shut down SID voices
	sta $d40b
	sta $d412
	txa				// clear SID registers
	ldx #$17
clrsid:	sta $d400,x
	dex
	bpl clrsid
	lda #$0f			// maximum volume
	sta $d418
	rts
	}

delay:	{
	ldx #14				// wait approx. 1 frame. accuracy is not
l1:	dey				// important as we only have to deal with
	bne l1				// keyboard handling
	dex
	bne l1
	rts
	}

keyboard:	{
	ldx key
	bmi nopress
	lda keyrow,x			// check if the key is still pressed
	sta $dc00
	lda $dc01
	and keycol,x
	beq exit			// wait until key has been released
nopress:	lda playmax
	cmp #numkeys-keyrow
	bcc maxnum
	lda #numkeys-keyrow-1
maxnum:	tax
loop:	lda keyrow,x
	sta $dc00
	lda $dc01
	and keycol,x
	beq found
	dex
	bpl loop
	stx key
exit:	rts
found:	stx key
	txa
	bne tglscr
	jmp stop			// run/stop key pressed
tglscr:
	dex
xstart:	txa
	jmp start			// start new song

key:	.byte $ff

keyrow:	.byte $7f	//run/stop

	.byte $7f	//1
	.byte $7f	//2
	.byte $fd	//3
	.byte $fd	//4
	.byte $fb	//5
	.byte $fb	//6
	.byte $f7	//7
	.byte $f7	//8
	.byte $ef	//9
	.byte $ef	//0
	.byte $fd	//a
	.byte $f7	//b
	.byte $fb	//c
	.byte $fb	//d
	.byte $fd	//e
	.byte $fb	//f
	.byte $f7	//g
	.byte $f7	//h
	.byte $ef	//i
	.byte $ef	//j
	.byte $ef	//k
	.byte $df	//l
	.byte $ef	//m
	.byte $ef	//n
	.byte $ef	//o
	.byte $df	//p
	.byte $7f	//q
	.byte $fb	//r
	.byte $fd	//s
	.byte $fb	//t
	.byte $f7	//u
	.byte $f7	//v
	.byte $fd	//w
	.byte $fb	//x
	.byte $f7	//y
	.byte $fd	//z
numkeys:

keycol:	.byte $80	//run/stop

	.byte $01	//1
	.byte $08	//2
	.byte $01	//3
	.byte $08	//4
	.byte $01	//5
	.byte $08	//6
	.byte $01	//7
	.byte $08	//8
	.byte $01	//9
	.byte $08	//0
	.byte $04	//a
	.byte $10	//b
	.byte $10	//c
	.byte $04	//d
	.byte $40	//e
	.byte $20	//f
	.byte $04	//g
	.byte $20	//h
	.byte $02	//i
	.byte $04	//j
	.byte $20	//k
	.byte $04	//l
	.byte $10	//m
	.byte $80	//n
	.byte $40	//o
	.byte $02	//p
	.byte $40	//q
	.byte $02	//r
	.byte $20	//s
	.byte $40	//t
	.byte $40	//u
	.byte $80	//v
	.byte $02	//w
	.byte $80	//x
	.byte $02	//y
	.byte $10	//z
	}
