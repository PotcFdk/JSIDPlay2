package libpsid64;

public interface IPsidDrv {
	/*
;   psid64 - create a C64 executable from a PSID file
;   Copyright (C) 2001-2003  Roland Hermans <rolandh@users.sourceforge.net>
;
;   This program is free software; you can redistribute it and/or modify
;   it under the terms of the GNU General Public License as published by
;   the Free Software Foundation; either version 2 of the License, or
;   (at your option) any later version.
;
;   This program is distributed in the hope that it will be useful,
;   but WITHOUT ANY WARRANTY; without even the implied warranty of
;   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;   GNU General Public License for more details.
;
;   You should have received a copy of the GNU General Public License
;   along with this program; if not, write to the Free Software
;   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
;
;
;   The relocating PSID driver is based on a reference implementation written
;   by Dag Lem, using Andre Fachat's relocating cross assembler, xa. The
;   original driver code was introduced in VICE 1.7.
;
;   Please note that this driver code is optimized to squeeze the minimal
;   driver (without screen support) in just two memory pages. For this reason
;   it contains some strange branches to gain a few bytes. Look out for side
;   effects when updating this code!

	.fopt 0, "psiddrv.o65", 0
	.fopt 3, "Dag Lem <resid@nimrod.no>", 0

	.align 256

#ifdef SCREEN
#define CLOCK_OFFSET			489
#define SCROLLER_OFFSET			840
#endif
	jmp cold
	jmp setiomap

	; Parameters
init	rts
	.word 0
play	rts
	.word 0
playmax	.byte 0
speed	.word 0, 0
irqvec	.byte 0
initiomap .byte 0
playiomap .byte 0
#ifdef SCREEN
stil	.byte 0
d011	.byte $1b
#endif // SCREEN

	; Variables
playnum	.byte 0
video	.byte 0
random	.byte 0
#ifdef SCREEN
prevjoy	.byte 0
clkcounter .byte 0
#ifdef SMOOTH_SCROLL
d016	.byte $08
#endif // SMOOTH_SCROLL
#endif // SCREEN

	;Constants
#ifdef SCREEN
clkoffs	.byte 0,1,3,4,6,7
#endif // SCREEN

	; User interrupt vectors
irqusr	.word irqret
brkusr	.word brkjob
nmiusr	.word nmijob

	; IRQ handler
irqjob	lda $d020			; save values
	pha
	lda $dc00
	pha
#ifdef SCREEN
	lda #$fd			; test for shift lock or shift left
	sta $dc00
	lda $dc01
	bmi noshift
	lda #6				; show rastertime used by play
	sta $d020
#endif // SCREEN
noshift	lda $01
	pha
	lda playiomap
	sta $01
	lda #0
	jsr play
	pla
	sta $01
#ifdef SCREEN
	lda #$ff
	sta $dc00
	lda $dc00			; test for joystick's fire button
	and #$10
	beq ffwd
#endif // SCREEN
	lda #$7f			; test for arrow left key
	sta $dc00
	lda $dc01
	and #2
	bne done
ffwd	inc $d020			; flash $d020 during fast forward
#ifdef SCREEN
	jsr clock
#endif // SCREEN
	jmp noshift
done	pla
	sta $dc00
	pla
	sta $d020
	lda $d019
	sta $d019
irqret	lda $dc0d
	pla
	tay
	pla
	tax
	pla
nmijob	rti

	; Cold start
	; Parameters passed in from boot loader
cold	pla
	sta random	; seed for random number generator
	pla
	sta video	; pal/ntsc flag
	pla
start	sta playnum	; song number
restart	sei
	cld
	ldx #$ff
	txs
	lda $01		; enable I/O without changing basic and kernal rom bank
	and #3
	bne haveio
	lda #$01
haveio	ora #$34
	sta $01
	jsr stop
#ifdef SCREEN
	ldx #0
	stx $dc03
	stx $dd03
	dex
	stx $dc02
	lda #<dd00			; relocation always in words
	sta $dd00
	lda #<d018			; relocation always in words
	sta $d018
	lda #$3f
	sta $dd02

	; Display current song number with leading spaces
	lda #>screen_songnum
	beq nosongnum
	ldx playnum
	inx
	txa
	ldy #0
	ldx #$30			; '0'
	cmp #100
	bcc lt100
ldiv100	sbc #100
	inx
	cmp #100
	bcs ldiv100
	stx screen_songnum		; here always y == 0
	iny
	ldx #$30
	bne adiv10
lt100	cmp #10
	bcc lt10
ldiv10	sbc #10
	inx
adiv10	cmp #10
	bcs ldiv10
	pha
	txa
	sta screen_songnum,y
	iny
	pla
lt10	ora #$30
	sta screen_songnum,y
	lda #$29			; ')'
spfill	sta screen_songnum+1,y
	lda #$20			; ' ' (used to erase old song number)
	iny
	cpy #3
	bcc spfill
nosongnum

	ldx #5				; reset clock
	lda #$30			; '0'
rstclk	ldy clkoffs,x
	sta screen+CLOCK_OFFSET,y
	dex
	bpl rstclk
	lda #0
	sta clkcounter

#endif // SCREEN

	; Set interrupt vectors
	ldx irqvec
	bmi vicras
store03	lda irqusr,x
	sta $0314,x
	dex
	bpl store03

	; Set VICII raster to line 0
vicras
#ifdef SCREEN
	lda d011
#else // !SCREEN
	lda #$0b			; blank screen
#endif // SCREEN
	ldx #$00
	sta $d011
	stx $d012

	; Set CIA 1 Timer A to 60Hz
	lda video
	beq ntsc
pal	lda #$25
	ldx #$40
	bne timer
ntsc	lda #$95
	ldx #$42
timer	sta $dc04
	stx $dc05

	; Get song number
	ldy playnum
	cpy playmax
	bcc songset
	ldy #$00
songset	tya
	pha

	; RSIDs use default environment of CIA1 only
        lda initiomap
        beq ciainit

	; Get shift number for speed bit
	cpy #32
	bcc shftset
	ldy #31

	; Find speed for current song number
shftset	lda #$00
	tax
	sec
shift	rol
	bcc nxtshft
	rol
	inx
nxtshft	dey
	bpl shift
	and speed,x
	bne ciainit

	; Enable VICII raster interrupt
vicinit	lda #$81
	sta $d01a
	bne doinit

	; Enable CIA 1 timer A interrupt
ciainit	lda #$81
	sta $dc0d

	; always enable timer A for random numbers
doinit	lda #$01
	sta $dc0e

	; If play address, override default irq vector so
	; we reach our routine to handle play routine
	lda playiomap
	beq noplay
	lda #<irqjob
	sta $0314

	; Set I/O map and call song init routine
noplay	lda #$2f
	sta $00
	lda initiomap
	bne setbank
	; Only release interrupt mask for real
	; C64 tunes (initiomap = 0) thus
	; providing a more realistic environment
	lda #$37
	; cli dosen't come into effect until
	; after the sta!
	cli
setbank	sta $01

	; get song number
	pla
	ldx random
rndwait	ldy #0
	dex
	bne rndwait
	jsr init
setiomap
	lda initiomap
	beq idle
	lda playiomap
	beq run
	lda #$37
	sta $01
	bne run

brkjob	ldx #$ff
	txs
run	cli
idle	jsr delay
#ifdef SCREEN
	lda d011
	and #$10
	beq noscr
	jsr colorline
	jsr colortext
	jsr scroller
noscr
	jsr clock
#endif // SCREEN
	php
	sei
	jsr keyboard
#ifdef SCREEN
	jsr joystick
	lda #$7f			; check for reset (control-cbm-delete)
	sta $dc00
	lda $dc01
	and #$24
	bne noreset			; control and cbm key not pressed
	ldx #$fe
	stx $dc00
	lda $dc01
	lsr
	bcs noreset			; delete key not pressed
	jsr stop
	lda #$37			; set bankreg
	sta $01
	sta $8004			; break a possible CBM80 vector
	jmp ($fffc)			; reset
noreset
#endif // SCREEN
	plp
	jmp idle

stop	.(
	ldx #0				; stop NMI and IRQ interrupts
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
	sta $d404			; set test bit to shut down SID voices
	sta $d40b
	sta $d412
	txa				; clear SID registers
	ldx #$17
clrsid	sta $d400,x
	dex
	bpl clrsid
#ifdef SCREEN
	stx clkcounter
#endif // SCREEN
	lda #$0f			; maximum volume
	sta $d418
	rts
	.)

delay	.(
#ifdef SCREEN
l1	inc random			; random number generator
	lda $d012
	bpl l1				; waits until raster > $80
l2
#ifdef SMOOTH_SCROLL
	ldx d016			; smooth scroll
	sec
	sbc #$c3
	cmp #$20
	bcc l3
	ldx #8
l3	stx $d016
#endif // SMOOTH_SCROLL
	lda $d012
	bmi l2				; waits until raster < $80 or > $100
#ifdef SMOOTH_SCROLL
	lda #8
	sta $d016
#endif // SMOOTH_SCROLL
	rts
#else // !SCREEN
	ldx #14				; wait approx. 1 frame. accuracy is not
l1	dey				; important as we only have to deal with
	bne l1				; keyboard handling
	dex
	bne l1
	rts
#endif // SCREEN
	.)

keyboard	.(
	ldx key
	bmi nopress
	lda keyrow,x			; check if the key is still pressed
	sta $dc00
	lda $dc01
	and keycol,x
	beq exit			; wait until key has been released
nopress	lda playmax
#ifdef SCREEN
	clc
	adc #3				; inst/del, + and - key
#endif // SCREEN
	cmp #numkeys
	bcc maxnum
	lda #numkeys-1
maxnum	tax
loop	lda keyrow,x
	sta $dc00
	lda $dc01
	and keycol,x
	beq found
	dex
	bpl loop
	stx key
exit	rts
found	stx key
	txa
	bne tglscr
	jmp stop			; run/stop key pressed
tglscr
#ifdef SCREEN
	dex
	bne incsong
	lda d011			; inst/del key pressed
	eor #$10
	sta d011
	sta $d011
	rts
incsong	
	dex
	bne decsong
	ldx playnum			; + key pressed
	inx
	cpx playmax
	bcc xstart
	ldx #0
	beq xstart			; bra
decsong
	dex
	bne newsong
	ldx playnum			; - key pressed
	bne maxsong
	ldx playmax
maxsong dex
	jmp xstart
newsong
#endif // SCREEN
	dex
xstart	txa
	jmp start			; start new song

key	.byte $ff

keyrow	.byte $7f	;run/stop

#ifdef SCREEN
	.byte $fe	;inst/del
	.byte $df	;+
	.byte $df	;-
#endif // SCREEN

	.byte $7f	;1
	.byte $7f	;2
	.byte $fd	;3
	.byte $fd	;4
	.byte $fb	;5
	.byte $fb	;6
	.byte $f7	;7
	.byte $f7	;8
	.byte $ef	;9
	.byte $ef	;0
	.byte $fd	;a
	.byte $f7	;b
	.byte $fb	;c
	.byte $fb	;d
	.byte $fd	;e
	.byte $fb	;f
	.byte $f7	;g
	.byte $f7	;h
	.byte $ef	;i
	.byte $ef	;j
	.byte $ef	;k
	.byte $df	;l
	.byte $ef	;m
	.byte $ef	;n
	.byte $ef	;o
	.byte $df	;p
	.byte $7f	;q
	.byte $fb	;r
	.byte $fd	;s
	.byte $fb	;t
	.byte $f7	;u
	.byte $f7	;v
	.byte $fd	;w
	.byte $fb	;x
	.byte $f7	;y
	.byte $fd	;z
numkeys = * - keyrow

keycol	.byte $80	;run/stop

#ifdef SCREEN
	.byte $01	;inst/del
	.byte $01	;+
	.byte $08	;-
#endif // SCREEN

	.byte $01	;1
	.byte $08	;2
	.byte $01	;3
	.byte $08	;4
	.byte $01	;5
	.byte $08	;6
	.byte $01	;7
	.byte $08	;8
	.byte $01	;9
	.byte $08	;0
	.byte $04	;a
	.byte $10	;b
	.byte $10	;c
	.byte $04	;d
	.byte $40	;e
	.byte $20	;f
	.byte $04	;g
	.byte $20	;h
	.byte $02	;i
	.byte $04	;j
	.byte $20	;k
	.byte $04	;l
	.byte $10	;m
	.byte $80	;n
	.byte $40	;o
	.byte $02	;p
	.byte $40	;q
	.byte $02	;r
	.byte $20	;s
	.byte $40	;t
	.byte $40	;u
	.byte $80	;v
	.byte $02	;w
	.byte $80	;x
	.byte $02	;y
	.byte $10	;z
	.)


#ifdef SCREEN

; A bit of CIA register $DC00 (Data Port A) is zero when the joystick connected
; to port two is moved in a certain direction or the fire button is pressed. By
; using the previous state of the joystick it is easy to determine the beginning
; of a new move. In the table below, A is the previous state of $DC00 and B is
; the current state of $DC00.
;
; A  B  (A xor B) and A
; -  -  ---------------
; 0  0  0
; 0  1  0
; 1  0  1
; 1  1  0

joystick .(
	lda #$ff
	sta $dc00
	lda $dc00
	tax
	eor prevjoy
	and prevjoy			; set bits indicate start of new move
	stx prevjoy
	lsr
	bcc tdown
	ldx playnum			; joystick moved up
	inx
	cpx playmax
	bcc xstart
	ldx #0
	beq xstart			; bra
tdown
	lsr
	bcc tleft
	ldx playnum			; joystick moved down
	bne maxsong
	ldx playmax
maxsong dex
xstart	txa
	jmp start			; start new song
tleft
	lsr
	bcc tright
	jmp stop			; joystick moved left
tright
	lsr
	bcc exit
	jmp restart			; joystick moved right
exit
	rts
	.)


clock	.(				; update the clock
	bit clkcounter
	bmi exit
	ldx #5
	lda #2
	dec clkcounter
	bpl cl1
	sta clkcounter
	ldy video
	bne cl1
cl2	lda #1
cl1	ldy clkoffs,x
	clc
	adc screen+CLOCK_OFFSET,y
	sta screen+CLOCK_OFFSET,y
	cmp clkcmp,x
	bcc exit
	lda #$30			; '0'
	sta screen+CLOCK_OFFSET,y
	dex
	bpl cl2				; c=1
exit
	rts

clkcmp	.byte $3a,$3a,$36,$3a,$3a,$3a
	.)


colorline .(				; moving color line effect
	ldy $d800+4
	ldx #0
l1	lda $d800+5,x
	sta $d800+4,x
	inx
	cpx #31
	bcc l1
	lda $d800+75
	sta $d800+35
	lda $d800+115
	sta $d800+75
	ldx #30
l2	lda $d800+84,x
	sta $d800+85,x
	dex
	bpl l2
	lda $d800+44
	sta $d800+84
	sty $d800+44
	rts
	.)


colortext .(				; flashing color text effect
	inc counter
	lda counter
	lsr
	and #15
	tay
	lda txtcol,y
	ldx #37
l1	sta $d800+961,x
	iny
	dex
	bpl l1
	rts

counter	.byte 0

txtcol	.byte 9,2,4,10,7,13,1,13,7,10,4,2,9,0,0,0
	.)

scroller .(

	ldy stil
	beq exit
#ifdef SMOOTH_SCROLL
	ldx #38
colscr1	lda $d800+SCROLLER_OFFSET,x
	sta $d801+SCROLLER_OFFSET,x
	dex
	bpl colscr1
	dec d016
	dec d016
#else // SMOOTH_SCROLL
	dec counter
#endif // SMOOTH_SCROLL
	bpl exit
#ifdef SMOOTH_SCROLL
	lda #6
	sta d016
	inc counter
	lda counter
	and #7
	tax
	lda scrcol,x
	sta $d800+SCROLLER_OFFSET
#else // SMOOTH_SCROLL
	lda #5
	sta counter
#endif // SMOOTH_SCROLL

	ldx #0
scroll	lda screen+SCROLLER_OFFSET+1,x
	sta screen+SCROLLER_OFFSET,x
	inx
	cpx #39
	bcc scroll

msgpos	= *+1
newchar	ldx eot
eot	= *+1
	cpx #$ff
	bne okchar
	inx				; restart scroll text
	stx msgpos
	sty msgpos+1
	beq newchar			; bra
okchar	stx screen+SCROLLER_OFFSET+39
	inc msgpos
	bne exit
	inc msgpos+1
exit	rts

counter	.byte 0

#ifdef SMOOTH_SCROLL
scrcol	.byte 5,5,5,3,13,1,13,3
#endif // SMOOTH_SCROLL
	.)

#endif // SCREEN 
	 */
    static final byte psid_driver[] = {
    	(byte) 0x01, (byte) 0x00, (byte) 0x6F, (byte) 0x36, (byte) 0x35, (byte) 0x00, (byte) 0x03, (byte) 0x10, 
    	(byte) 0x00, (byte) 0x10, (byte) 0xFB, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x0E, (byte) 0x00, (byte) 0x70, (byte) 0x73, (byte) 0x69, (byte) 0x64, 
    	(byte) 0x64, (byte) 0x72, (byte) 0x76, (byte) 0x2E, (byte) 0x6F, (byte) 0x36, (byte) 0x35, (byte) 0x00, 
    	(byte) 0x1C, (byte) 0x03, (byte) 0x44, (byte) 0x61, (byte) 0x67, (byte) 0x20, (byte) 0x4C, (byte) 0x65, 
    	(byte) 0x6D, (byte) 0x20, (byte) 0x3C, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x69, (byte) 0x64, 
    	(byte) 0x40, (byte) 0x6E, (byte) 0x69, (byte) 0x6D, (byte) 0x72, (byte) 0x6F, (byte) 0x64, (byte) 0x2E, 
    	(byte) 0x6E, (byte) 0x6F, (byte) 0x3E, (byte) 0x00, (byte) 0x00, (byte) 0x4C, (byte) 0x5E, (byte) 0x10, 
    	(byte) 0x4C, (byte) 0x08, (byte) 0x11, (byte) 0x60, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x55, (byte) 0x10, (byte) 0x18, (byte) 0x11, 
    	(byte) 0x5D, (byte) 0x10, (byte) 0xAD, (byte) 0x20, (byte) 0xD0, (byte) 0x48, (byte) 0xAD, (byte) 0x00, 
    	(byte) 0xDC, (byte) 0x48, (byte) 0xA5, (byte) 0x01, (byte) 0x48, (byte) 0xAD, (byte) 0x13, (byte) 0x10, 
    	(byte) 0x85, (byte) 0x01, (byte) 0xA9, (byte) 0x00, (byte) 0x20, (byte) 0x09, (byte) 0x10, (byte) 0x68, 
    	(byte) 0x85, (byte) 0x01, (byte) 0xA9, (byte) 0x7F, (byte) 0x8D, (byte) 0x00, (byte) 0xDC, (byte) 0xAD, 
    	(byte) 0x01, (byte) 0xDC, (byte) 0x29, (byte) 0x02, (byte) 0xD0, (byte) 0x06, (byte) 0xEE, (byte) 0x20, 
    	(byte) 0xD0, (byte) 0x4C, (byte) 0x25, (byte) 0x10, (byte) 0x68, (byte) 0x8D, (byte) 0x00, (byte) 0xDC, 
    	(byte) 0x68, (byte) 0x8D, (byte) 0x20, (byte) 0xD0, (byte) 0xAD, (byte) 0x19, (byte) 0xD0, (byte) 0x8D, 
    	(byte) 0x19, (byte) 0xD0, (byte) 0xAD, (byte) 0x0D, (byte) 0xDC, (byte) 0x68, (byte) 0xA8, (byte) 0x68, 
    	(byte) 0xAA, (byte) 0x68, (byte) 0x40, (byte) 0x68, (byte) 0x8D, (byte) 0x16, (byte) 0x10, (byte) 0x68, 
    	(byte) 0x8D, (byte) 0x15, (byte) 0x10, (byte) 0x68, (byte) 0x8D, (byte) 0x14, (byte) 0x10, (byte) 0x78, 
    	(byte) 0xD8, (byte) 0xA2, (byte) 0xFF, (byte) 0x9A, (byte) 0xA5, (byte) 0x01, (byte) 0x29, (byte) 0x03, 
    	(byte) 0xD0, (byte) 0x02, (byte) 0xA9, (byte) 0x01, (byte) 0x09, (byte) 0x34, (byte) 0x85, (byte) 0x01, 
    	(byte) 0x20, (byte) 0x28, (byte) 0x11, (byte) 0xAE, (byte) 0x11, (byte) 0x10, (byte) 0x30, (byte) 0x09, 
    	(byte) 0xBD, (byte) 0x17, (byte) 0x10, (byte) 0x9D, (byte) 0x14, (byte) 0x03, (byte) 0xCA, (byte) 0x10, 
    	(byte) 0xF7, (byte) 0xA9, (byte) 0x0B, (byte) 0xA2, (byte) 0x00, (byte) 0x8D, (byte) 0x11, (byte) 0xD0, 
    	(byte) 0x8E, (byte) 0x12, (byte) 0xD0, (byte) 0xAD, (byte) 0x15, (byte) 0x10, (byte) 0xF0, (byte) 0x06, 
    	(byte) 0xA9, (byte) 0x25, (byte) 0xA2, (byte) 0x40, (byte) 0xD0, (byte) 0x04, (byte) 0xA9, (byte) 0x95, 
    	(byte) 0xA2, (byte) 0x42, (byte) 0x8D, (byte) 0x04, (byte) 0xDC, (byte) 0x8E, (byte) 0x05, (byte) 0xDC, 
    	(byte) 0xAC, (byte) 0x14, (byte) 0x10, (byte) 0xCC, (byte) 0x0C, (byte) 0x10, (byte) 0x90, (byte) 0x02, 
    	(byte) 0xA0, (byte) 0x00, (byte) 0x98, (byte) 0x48, (byte) 0xAD, (byte) 0x12, (byte) 0x10, (byte) 0xF0, 
    	(byte) 0x1E, (byte) 0xC0, (byte) 0x20, (byte) 0x90, (byte) 0x02, (byte) 0xA0, (byte) 0x1F, (byte) 0xA9, 
    	(byte) 0x00, (byte) 0xAA, (byte) 0x38, (byte) 0x2A, (byte) 0x90, (byte) 0x02, (byte) 0x2A, (byte) 0xE8, 
    	(byte) 0x88, (byte) 0x10, (byte) 0xF8, (byte) 0x3D, (byte) 0x0D, (byte) 0x10, (byte) 0xD0, (byte) 0x07, 
    	(byte) 0xA9, (byte) 0x81, (byte) 0x8D, (byte) 0x1A, (byte) 0xD0, (byte) 0xD0, (byte) 0x05, (byte) 0xA9, 
    	(byte) 0x81, (byte) 0x8D, (byte) 0x0D, (byte) 0xDC, (byte) 0xA9, (byte) 0x01, (byte) 0x8D, (byte) 0x0E, 
    	(byte) 0xDC, (byte) 0xAD, (byte) 0x13, (byte) 0x10, (byte) 0xF0, (byte) 0x05, (byte) 0xA9, (byte) 0x1D, 
    	(byte) 0x8D, (byte) 0x14, (byte) 0x03, (byte) 0xA9, (byte) 0x2F, (byte) 0x85, (byte) 0x00, (byte) 0xAD, 
    	(byte) 0x12, (byte) 0x10, (byte) 0xD0, (byte) 0x03, (byte) 0xA9, (byte) 0x37, (byte) 0x58, (byte) 0x85, 
    	(byte) 0x01, (byte) 0x68, (byte) 0xAE, (byte) 0x16, (byte) 0x10, (byte) 0xA0, (byte) 0x00, (byte) 0xCA, 
    	(byte) 0xD0, (byte) 0xFB, (byte) 0x20, (byte) 0x06, (byte) 0x10, (byte) 0xAD, (byte) 0x12, (byte) 0x10, 
    	(byte) 0xF0, (byte) 0x0F, (byte) 0xAD, (byte) 0x13, (byte) 0x10, (byte) 0xF0, (byte) 0x09, (byte) 0xA9, 
    	(byte) 0x37, (byte) 0x85, (byte) 0x01, (byte) 0xD0, (byte) 0x03, (byte) 0xA2, (byte) 0xFF, (byte) 0x9A, 
    	(byte) 0x58, (byte) 0x20, (byte) 0x67, (byte) 0x11, (byte) 0x08, (byte) 0x78, (byte) 0x20, (byte) 0x70, 
    	(byte) 0x11, (byte) 0x28, (byte) 0x4C, (byte) 0x1C, (byte) 0x11, (byte) 0xA2, (byte) 0x00, (byte) 0x8E, 
    	(byte) 0x1A, (byte) 0xD0, (byte) 0xAD, (byte) 0x19, (byte) 0xD0, (byte) 0x8D, (byte) 0x19, (byte) 0xD0, 
    	(byte) 0xA9, (byte) 0x7F, (byte) 0x8D, (byte) 0x0D, (byte) 0xDC, (byte) 0x8D, (byte) 0x0D, (byte) 0xDD, 
    	(byte) 0xAD, (byte) 0x0D, (byte) 0xDC, (byte) 0xAD, (byte) 0x0D, (byte) 0xDD, (byte) 0xA9, (byte) 0x08, 
    	(byte) 0x8D, (byte) 0x0E, (byte) 0xDC, (byte) 0x8D, (byte) 0x0E, (byte) 0xDD, (byte) 0x8D, (byte) 0x0F, 
    	(byte) 0xDC, (byte) 0x8D, (byte) 0x0F, (byte) 0xDD, (byte) 0x8D, (byte) 0x04, (byte) 0xD4, (byte) 0x8D, 
    	(byte) 0x0B, (byte) 0xD4, (byte) 0x8D, (byte) 0x12, (byte) 0xD4, (byte) 0x8A, (byte) 0xA2, (byte) 0x17, 
    	(byte) 0x9D, (byte) 0x00, (byte) 0xD4, (byte) 0xCA, (byte) 0x10, (byte) 0xFA, (byte) 0xA9, (byte) 0x0F, 
    	(byte) 0x8D, (byte) 0x18, (byte) 0xD4, (byte) 0x60, (byte) 0xA2, (byte) 0x0E, (byte) 0x88, (byte) 0xD0, 
    	(byte) 0xFD, (byte) 0xCA, (byte) 0xD0, (byte) 0xFA, (byte) 0x60, (byte) 0xAE, (byte) 0xB0, (byte) 0x11, 
    	(byte) 0x30, (byte) 0x0E, (byte) 0xBD, (byte) 0xB1, (byte) 0x11, (byte) 0x8D, (byte) 0x00, (byte) 0xDC, 
    	(byte) 0xAD, (byte) 0x01, (byte) 0xDC, (byte) 0x3D, (byte) 0xD6, (byte) 0x11, (byte) 0xF0, (byte) 0x1E, 
    	(byte) 0xAD, (byte) 0x0C, (byte) 0x10, (byte) 0xC9, (byte) 0x25, (byte) 0x90, (byte) 0x02, (byte) 0xA9, 
    	(byte) 0x24, (byte) 0xAA, (byte) 0xBD, (byte) 0xB1, (byte) 0x11, (byte) 0x8D, (byte) 0x00, (byte) 0xDC, 
    	(byte) 0xAD, (byte) 0x01, (byte) 0xDC, (byte) 0x3D, (byte) 0xD6, (byte) 0x11, (byte) 0xF0, (byte) 0x07, 
    	(byte) 0xCA, (byte) 0x10, (byte) 0xEF, (byte) 0x8E, (byte) 0xB0, (byte) 0x11, (byte) 0x60, (byte) 0x8E, 
    	(byte) 0xB0, (byte) 0x11, (byte) 0x8A, (byte) 0xD0, (byte) 0x03, (byte) 0x4C, (byte) 0x28, (byte) 0x11, 
    	(byte) 0xCA, (byte) 0x8A, (byte) 0x4C, (byte) 0x67, (byte) 0x10, (byte) 0xFF, (byte) 0x7F, (byte) 0x7F, 
    	(byte) 0x7F, (byte) 0xFD, (byte) 0xFD, (byte) 0xFB, (byte) 0xFB, (byte) 0xF7, (byte) 0xF7, (byte) 0xEF, 
    	(byte) 0xEF, (byte) 0xFD, (byte) 0xF7, (byte) 0xFB, (byte) 0xFB, (byte) 0xFD, (byte) 0xFB, (byte) 0xF7, 
    	(byte) 0xF7, (byte) 0xEF, (byte) 0xEF, (byte) 0xEF, (byte) 0xDF, (byte) 0xEF, (byte) 0xEF, (byte) 0xEF, 
    	(byte) 0xDF, (byte) 0x7F, (byte) 0xFB, (byte) 0xFD, (byte) 0xFB, (byte) 0xF7, (byte) 0xF7, (byte) 0xFD, 
    	(byte) 0xFB, (byte) 0xF7, (byte) 0xFD, (byte) 0x80, (byte) 0x01, (byte) 0x08, (byte) 0x01, (byte) 0x08, 
    	(byte) 0x01, (byte) 0x08, (byte) 0x01, (byte) 0x08, (byte) 0x01, (byte) 0x08, (byte) 0x04, (byte) 0x10, 
    	(byte) 0x10, (byte) 0x04, (byte) 0x40, (byte) 0x20, (byte) 0x04, (byte) 0x20, (byte) 0x02, (byte) 0x04, 
    	(byte) 0x20, (byte) 0x04, (byte) 0x10, (byte) 0x80, (byte) 0x40, (byte) 0x02, (byte) 0x40, (byte) 0x02, 
    	(byte) 0x20, (byte) 0x40, (byte) 0x40, (byte) 0x80, (byte) 0x02, (byte) 0x80, (byte) 0x02, (byte) 0x10, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x13, (byte) 0x82, 
    	(byte) 0x02, (byte) 0x82, (byte) 0x02, (byte) 0x82, (byte) 0x0E, (byte) 0x82, (byte) 0x07, (byte) 0x82, 
    	(byte) 0x15, (byte) 0x82, (byte) 0x1B, (byte) 0x82, (byte) 0x04, (byte) 0x82, (byte) 0x04, (byte) 0x82, 
    	(byte) 0x14, (byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x05, (byte) 0x82, (byte) 0x13, (byte) 0x82, 
    	(byte) 0x15, (byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x09, (byte) 0x82, (byte) 0x17, (byte) 0x82, 
    	(byte) 0x16, (byte) 0x82, (byte) 0x05, (byte) 0x22, (byte) 0x09, (byte) 0x82, (byte) 0x0B, (byte) 0x82, 
    	(byte) 0x08, (byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x05, (byte) 0x82, (byte) 0x0F, (byte) 0x82, 
    	(byte) 0x05, (byte) 0x82, (byte) 0x04, (byte) 0x82, (byte) 0x4B, (byte) 0x82, (byte) 0x05, (byte) 0x82, 
    	(byte) 0x09, (byte) 0x82, (byte) 0x05, (byte) 0x82, (byte) 0x0A, (byte) 0x82, (byte) 0x09, (byte) 0x82, 
    	(byte) 0x08, (byte) 0x82, (byte) 0x04, (byte) 0x82, (byte) 0x06, (byte) 0x82, (byte) 0x05, (byte) 0x82, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };
}
