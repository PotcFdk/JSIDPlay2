.pc = cmdLineVars.get("pc").asNumber()

	.align 256

.var CLOCK_OFFSET=489
.var SCROLLER_OFFSET=840
	jmp cold
	jmp setiomap

	// Parameters
init:		.byte cmdLineVars.get("initCmd").asNumber()
			.word cmdLineVars.get("initAddr").asNumber()
play:		.byte cmdLineVars.get("playCmd").asNumber()
			.word cmdLineVars.get("playAddr").asNumber()
playmax:	.byte cmdLineVars.get("songs").asNumber()
speed:		.word cmdLineVars.get("speed").asNumber() & $ffff
			.word [cmdLineVars.get("speed").asNumber() >> 16] & $ffff
irqvec:		.byte cmdLineVars.get("irqVec").asNumber()
initiomap:	.byte cmdLineVars.get("initIOMap").asNumber()
playiomap:	.byte cmdLineVars.get("playIOMap").asNumber()
stil:		.byte cmdLineVars.get("stilPage").asNumber()
d011:		.byte $1b

	// Variables
playnum:	.byte 0
video:	.byte 0
random:	.byte 0
prevjoy:	.byte 0
clkcounter: .byte 0
d016:	.byte $08

	//Constants
clkoffs:	.byte 0,1,3,4,6,7

	// User interrupt vectors
irqusr:	.word irqret
brkusr:	.word brkjob
nmiusr:	.word nmijob

	// IRQ handler
irqjob:	lda $d020			// save values
	pha
	lda $dc00
	pha
	lda #$fd			// test for shift lock or shift left
	sta $dc00
	lda $dc01
	bmi noshift
	lda #6				// show rastertime used by play
	sta $d020
noshift:	lda $01
	pha
	lda playiomap
	sta $01
	lda #0
	// inc $d020
	jsr play
	// dec $d020
	pla
	sta $01
	lda #$ff
	sta $dc00
	lda $dc00			// test for joystick's fire button
	and #$10
	beq ffwd
	lda #$7f			// test for arrow left key
	sta $dc00
	lda $dc01
	and #2
	bne done
ffwd:	inc $d020			// flash $d020 during fast forward
	jsr clock
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
	ldx #0
	stx $dc03
	stx $dd03
	dex
	stx $dc02
	lda #<cmdLineVars.get("dd00").asNumber()			// relocation always in words
	sta $dd00
	lda #<cmdLineVars.get("d018").asNumber()			// relocation always in words
	sta $d018
	lda #$3f
	sta $dd02

	// Display current song number with leading spaces
	lda #>cmdLineVars.get("screen_songnum").asNumber()
	beq nosongnum
	ldx playnum
	inx
	txa
	ldy #0
	ldx #$30			// '0'
	cmp #100
	bcc lt100
ldiv100:	sbc #100
	inx
	cmp #100
	bcs ldiv100
	stx cmdLineVars.get("screen_songnum").asNumber()		// here always y == 0
	iny
	ldx #$30
	bne adiv10
lt100:	cmp #10
	bcc lt10
ldiv10:	sbc #10
	inx
adiv10:	cmp #10
	bcs ldiv10
	pha
	txa
	sta cmdLineVars.get("screen_songnum").asNumber(),y
	iny
	pla
lt10:	ora #$30
	sta cmdLineVars.get("screen_songnum").asNumber(),y
	lda #$29			// ')'
spfill:	sta cmdLineVars.get("screen_songnum").asNumber()+1,y
	lda #$20			// ' ' (used to erase old song number)
	iny
	cpy #3
	bcc spfill
nosongnum:

	ldx #5				// reset clock
	lda #$30			// '0'
rstclk:	ldy clkoffs,x
	sta cmdLineVars.get("screen").asNumber()+CLOCK_OFFSET,y
	dex
	bpl rstclk
	lda #0
	sta clkcounter

	// Set interrupt vectors
	ldx irqvec
	bmi vicras
store03:	lda irqusr,x
	sta $0314,x
	dex
	bpl store03

	// Set VICII raster to line 0
vicras:
	lda d011
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
	lda d011
	and #$10
	beq noscr
	jsr colorline
	jsr colortext
	jsr scroller
noscr:
	jsr clock
	php
	sei
	jsr keyboard
	jsr joystick
	lda #$7f			// check for reset (control-cbm-delete)
	sta $dc00
	lda $dc01
	and #$24
	bne noreset			// control and cbm key not pressed
	ldx #$fe
	stx $dc00
	lda $dc01
	lsr
	bcs noreset			// delete key not pressed
	jsr stop
	lda #$37			// set bankreg
	sta $01
	sta $8004			// break a possible CBM80 vector
	jmp ($fffc)			// reset
noreset:
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
	stx clkcounter
	lda #$0f			// maximum volume
	sta $d418
	rts
	}

delay:	{
l1:	inc random			// random number generator
	lda $d012
	bpl l1				// waits until raster > $80
l2:
	ldx d016			// smooth scroll
	sec
	sbc #$c3
	cmp #$20
	bcc l3
	ldx #8
l3:	stx $d016
	lda $d012
	bmi l2				// waits until raster < $80 or > $100
	lda #8
	sta $d016
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
	clc
	adc #3				// inst/del, + and - key
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
	bne incsong
	lda d011			// inst/del key pressed
	eor #$10
	sta d011
	sta $d011
	rts
incsong:	
	dex
	bne decsong
	ldx playnum			// + key pressed
	inx
	cpx playmax
	bcc xstart
	ldx #0
	beq xstart			// bra
decsong:
	dex
	bne newsong
	ldx playnum			// - key pressed
	bne maxsong
	ldx playmax
maxsong: dex
	jmp xstart
newsong:
	dex
xstart:	txa
	jmp start			// start new song

key:	.byte $ff

keyrow:	.byte $7f	//run/stop

	.byte $fe	//inst/del
	.byte $df	//+
	.byte $df	//-

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

	.byte $01	//inst/del
	.byte $01	//+
	.byte $08	//-

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


// A bit of CIA register $DC00 (Data Port A) is zero when the joystick connected
// to port two is moved in a certain direction or the fire button is pressed. By
// using the previous state of the joystick it is easy to determine the beginning
// of a new move. In the table below, A is the previous state of $DC00 and B is
// the current state of $DC00.
//
// A  B  (A xor B) and A
// -  -  ---------------
// 0  0  0
// 0  1  0
// 1  0  1
// 1  1  0

joystick: {
	lda #$ff
	sta $dc00
	lda $dc00
	tax
	eor prevjoy
	and prevjoy			// set bits indicate start of new move
	stx prevjoy
	lsr
	bcc tdown
	ldx playnum			// joystick moved up
	inx
	cpx playmax
	bcc xstart
	ldx #0
	beq xstart			// bra
tdown:
	lsr
	bcc tleft
	ldx playnum			// joystick moved down
	bne maxsong
	ldx playmax
maxsong: dex
xstart:	txa
	jmp start			// start new song
tleft:
	lsr
	bcc tright
	jmp stop			// joystick moved left
tright:
	lsr
	bcc exit
	jmp restart			// joystick moved right
exit:
	rts
	}


clock:	{				// update the clock
	bit clkcounter
	bmi exit
	ldx #5
	lda #2
	dec clkcounter
	bpl cl1
	sta clkcounter
	ldy video
	bne cl1
cl2:	lda #1
cl1:	ldy clkoffs,x
	clc
	adc cmdLineVars.get("screen").asNumber()+CLOCK_OFFSET,y
	sta cmdLineVars.get("screen").asNumber()+CLOCK_OFFSET,y
	cmp clkcmp,x
	bcc exit
	lda #$30			// '0'
	sta cmdLineVars.get("screen").asNumber()+CLOCK_OFFSET,y
	dex
	bpl cl2				// c=1
exit:
	rts

clkcmp:	.byte $3a,$3a,$36,$3a,$3a,$3a
	}


colorline: {				// moving color line effect
	ldy $d800+4
	ldx #0
l1:	lda $d800+5,x
	sta $d800+4,x
	inx
	cpx #31
	bcc l1
	lda $d800+75
	sta $d800+35
	lda $d800+115
	sta $d800+75
	ldx #30
l2:	lda $d800+84,x
	sta $d800+85,x
	dex
	bpl l2
	lda $d800+44
	sta $d800+84
	sty $d800+44
	rts
	}


colortext: {				// flashing color text effect
	inc counter
	lda counter
	lsr
	and #15
	tay
	lda txtcol,y
	ldx #37
l1:	sta $d800+961,x
	iny
	dex
	bpl l1
	rts

counter:	.byte 0

txtcol:	.byte 9,2,4,10,7,13,1,13,7,10,4,2,9,0,0,0
	}

scroller: {

	ldy stil
	beq exit
	ldx #38
colscr1:	lda $d800+SCROLLER_OFFSET,x
	sta $d801+SCROLLER_OFFSET,x
	dex
	bpl colscr1
	dec d016
	dec d016
	bpl exit
	lda #6
	sta d016
	inc counter
	lda counter
	and #7
	tax
	lda scrcol,x
	sta $d800+SCROLLER_OFFSET

	ldx #0
scroll:	lda cmdLineVars.get("screen").asNumber()+SCROLLER_OFFSET+1,x
	sta cmdLineVars.get("screen").asNumber()+SCROLLER_OFFSET,x
	inx
	cpx #39
	bcc scroll

.var msgpos	= *+1
newchar:	ldx eot+1
eot:
	cpx #$ff
	bne okchar
	inx				// restart scroll text
	stx msgpos
	sty msgpos+1
	beq newchar			// bra
okchar:	stx cmdLineVars.get("screen").asNumber()+SCROLLER_OFFSET+39
	inc msgpos
	bne exit
	inc msgpos+1
exit:	rts

counter:	.byte 0

scrcol:	.byte 5,5,5,3,13,1,13,3
	}
