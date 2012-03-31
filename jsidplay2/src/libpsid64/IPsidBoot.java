package libpsid64;

public interface IPsidBoot {
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

r1_org	= $00f7

#define MAX_BLOCKS			4

	.word $0801

	* = $0801

	.word basic
	.word 2004
	.byte $9e,"2061",0
basic	.word 0


song	= *+1
	lda #0
	jmp start

r1_src	= *
	* = r1_org

	; Parameters
src	.word song			; $00f7
dest	.word 0				; $00f9
counter	.byte 0				; $00fb
zp	.word 0				; $00fc
numblk	.byte 0				; $00fe
chars	.byte 0				; $00ff
player	.word 0				; $0100
stopvec .word 0
blocks	.dsb 4*MAX_BLOCKS, 0

memmove	.(
	lda stopvec
	sta $0328
	lda stopvec+1
	sta $0329
	; Move all blocks to highest memory area
	ldy #0
l1	dec src+1
	dec dest+1
l2	lda (src),y
	sta (dest),y
	iny
	bne l2
	dec counter
	bne l1

	; Move the blocks to the correct locations
	ldx numblk
loop	lda blocks,x
	sta dest
	lda blocks+MAX_BLOCKS,x
	sta dest+1
	ldy #0
	lda blocks+3*MAX_BLOCKS,x
	beq skip1
	sta counter
copy1	lda (zp),y
	sta (dest),y
	iny
	bne copy1
	inc zp+1
	inc dest+1
	dec counter
	bne copy1
skip1	lda blocks+2*MAX_BLOCKS,x
	beq skip2
	sta counter
copy2	lda (zp),y
	sta (dest),y
	iny
	cpy counter
	bne copy2
	tya
	clc
	adc zp
	sta zp
	bcc skip2
	inc zp+1
skip2	dex
	bpl loop

	; Copy character ROM data for screens $4000-$8000 or $C000-$D000
	lda chars
	beq nochars
	sta dest+1
	lda #$d8
	sta zp+1
	ldy #0
	sty zp
	sty dest
	ldx #7
	dec 1				; $33
copy3	lda (zp),y
	sta (dest),y
	iny
	bne copy3
	inc zp+1
	inc dest+1
	dex
	bpl copy3

nochars	lda #$37
	sta 1
	jmp (player)
	.)

r1_len	= *-r1_org
	* = r1_src+r1_len


start	.(
	sei
	ldx #$ff
	txs
	cld
	pha				; Store default song number
	lda $02a6
	and #1
	pha				; Store pal/ntsc flag
	lda $a2
	pha				; Store seed for random number generator

	lda #$37
	sta 1
	jsr $ff84

	; Initialize VIC-II chip
	ldx #$2d
vicinit	lda vicdata,x
	sta $d000,x
	dex
	bpl vicinit

	; Initialize color memory
	ldy #0
	tya
colinit	sta $d800,y
	sta $d900,y
	sta $da00,y
	sta $db00,y
	iny
	bne colinit

	ldx #29				; set colors of program name
	lda #15
prgcol	sta $d800+45,x
	dex
	bpl prgcol
	ldx #14
l1	lda linecol,x
	sta $d804,x
	dex
	bpl l1

	ldy #240			; set color of information lines
l2	ldx #39
	lda #1
l3	cpx #7
	beq l4
	bcs l5
	lda #13
	.byte $2c			; bit $xxxx
l4	lda #7
l5	dey
	sta $d800+160,y
	sta $d800+280,y
	dex
	bpl l3
	tya
	bne l2

	lda #12				; set color of area for STIL text
stilcol	sta $d800+520,y
	iny
	bne stilcol

#ifndef SMOOTH_SCROLL
	ldx #19
	ldy #0
stilco2	lda #1
	cpx #3
	bcs skip
	lda scrollcol,x
skip	sta $d800+840,x
	sta $d800+860,y
	iny
	dex
	bpl stilco2
#endif // SMOOTH_SCROLL

	lda #$34
	sta 1

#if r1_len<=$80
	ldy #r1_len-1
copyr1	lda r1_src,y
	sta r1_org,y
	dey
	bpl copyr1
#else
	ldy #0
copyr1	lda r1_src,y
	sta r1_org,y
	iny
	cpy #r1_len
	bne copyr1
#endif
	jmp memmove


vicdata	.byte $00,$00,$00,$00,$00,$00,$00,$00
	.byte $00,$00,$00,$00,$00,$00,$00,$00
	.byte $00,$6b,$37,$00,$00,$00,$08,$00
	.byte $14,$0f,$00,$00,$00,$00,$00,$00
	.byte $00,$00,$01,$02,$03,$04,$00,$01
	.byte $02,$03,$04,$05,$06,$07

linecol	.byte 9,11,8,12,10,15,7,1,13,7,3,12,14,4,6

#ifndef SMOOTH_SCROLL
scrollcol .byte 11,12,15
#endif // ! SMOOTH_SCROLL
	.) 
	*/
    static final byte psid_boot[] = {
    	(byte) 0x01, (byte) 0x08, (byte) 0x0B, (byte) 0x08, (byte) 0xD4, (byte) 0x07, (byte) 0x9E, (byte) 0x32, 
    	(byte) 0x30, (byte) 0x36, (byte) 0x31, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA9, (byte) 0x00, 
    	(byte) 0x4C, (byte) 0xB6, (byte) 0x08, (byte) 0x0E, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0xAD, (byte) 0x02, (byte) 0x01, (byte) 0x8D, (byte) 0x28, (byte) 0x03, (byte) 0xAD, (byte) 0x03, 
    	(byte) 0x01, (byte) 0x8D, (byte) 0x29, (byte) 0x03, (byte) 0xA0, (byte) 0x00, (byte) 0xC6, (byte) 0xF8, 
    	(byte) 0xC6, (byte) 0xFA, (byte) 0xB1, (byte) 0xF7, (byte) 0x91, (byte) 0xF9, (byte) 0xC8, (byte) 0xD0, 
    	(byte) 0xF9, (byte) 0xC6, (byte) 0xFB, (byte) 0xD0, (byte) 0xF1, (byte) 0xA6, (byte) 0xFE, (byte) 0xBD, 
    	(byte) 0x04, (byte) 0x01, (byte) 0x85, (byte) 0xF9, (byte) 0xBD, (byte) 0x08, (byte) 0x01, (byte) 0x85, 
    	(byte) 0xFA, (byte) 0xA0, (byte) 0x00, (byte) 0xBD, (byte) 0x10, (byte) 0x01, (byte) 0xF0, (byte) 0x11, 
    	(byte) 0x85, (byte) 0xFB, (byte) 0xB1, (byte) 0xFC, (byte) 0x91, (byte) 0xF9, (byte) 0xC8, (byte) 0xD0, 
    	(byte) 0xF9, (byte) 0xE6, (byte) 0xFD, (byte) 0xE6, (byte) 0xFA, (byte) 0xC6, (byte) 0xFB, (byte) 0xD0, 
    	(byte) 0xF1, (byte) 0xBD, (byte) 0x0C, (byte) 0x01, (byte) 0xF0, (byte) 0x15, (byte) 0x85, (byte) 0xFB, 
    	(byte) 0xB1, (byte) 0xFC, (byte) 0x91, (byte) 0xF9, (byte) 0xC8, (byte) 0xC4, (byte) 0xFB, (byte) 0xD0, 
    	(byte) 0xF7, (byte) 0x98, (byte) 0x18, (byte) 0x65, (byte) 0xFC, (byte) 0x85, (byte) 0xFC, (byte) 0x90, 
    	(byte) 0x02, (byte) 0xE6, (byte) 0xFD, (byte) 0xCA, (byte) 0x10, (byte) 0xC1, (byte) 0xA5, (byte) 0xFF, 
    	(byte) 0xF0, (byte) 0x1E, (byte) 0x85, (byte) 0xFA, (byte) 0xA9, (byte) 0xD8, (byte) 0x85, (byte) 0xFD, 
    	(byte) 0xA0, (byte) 0x00, (byte) 0x84, (byte) 0xFC, (byte) 0x84, (byte) 0xF9, (byte) 0xA2, (byte) 0x07, 
    	(byte) 0xC6, (byte) 0x01, (byte) 0xB1, (byte) 0xFC, (byte) 0x91, (byte) 0xF9, (byte) 0xC8, (byte) 0xD0, 
    	(byte) 0xF9, (byte) 0xE6, (byte) 0xFD, (byte) 0xE6, (byte) 0xFA, (byte) 0xCA, (byte) 0x10, (byte) 0xF2, 
    	(byte) 0xA9, (byte) 0x37, (byte) 0x85, (byte) 0x01, (byte) 0x6C, (byte) 0x00, (byte) 0x01, (byte) 0x78, 
    	(byte) 0xA2, (byte) 0xFF, (byte) 0x9A, (byte) 0xD8, (byte) 0x48, (byte) 0xAD, (byte) 0xA6, (byte) 0x02, 
    	(byte) 0x29, (byte) 0x01, (byte) 0x48, (byte) 0xA5, (byte) 0xA2, (byte) 0x48, (byte) 0xA9, (byte) 0x37, 
    	(byte) 0x85, (byte) 0x01, (byte) 0x20, (byte) 0x84, (byte) 0xFF, (byte) 0xA2, (byte) 0x2D, (byte) 0xBD, 
    	(byte) 0x38, (byte) 0x09, (byte) 0x9D, (byte) 0x00, (byte) 0xD0, (byte) 0xCA, (byte) 0x10, (byte) 0xF7, 
    	(byte) 0xA0, (byte) 0x00, (byte) 0x98, (byte) 0x99, (byte) 0x00, (byte) 0xD8, (byte) 0x99, (byte) 0x00, 
    	(byte) 0xD9, (byte) 0x99, (byte) 0x00, (byte) 0xDA, (byte) 0x99, (byte) 0x00, (byte) 0xDB, (byte) 0xC8, 
    	(byte) 0xD0, (byte) 0xF1, (byte) 0xA2, (byte) 0x1D, (byte) 0xA9, (byte) 0x0F, (byte) 0x9D, (byte) 0x2D, 
    	(byte) 0xD8, (byte) 0xCA, (byte) 0x10, (byte) 0xFA, (byte) 0xA2, (byte) 0x0E, (byte) 0xBD, (byte) 0x66, 
    	(byte) 0x09, (byte) 0x9D, (byte) 0x04, (byte) 0xD8, (byte) 0xCA, (byte) 0x10, (byte) 0xF7, (byte) 0xA0, 
    	(byte) 0xF0, (byte) 0xA2, (byte) 0x27, (byte) 0xA9, (byte) 0x01, (byte) 0xE0, (byte) 0x07, (byte) 0xF0, 
    	(byte) 0x05, (byte) 0xB0, (byte) 0x05, (byte) 0xA9, (byte) 0x0D, (byte) 0x2C, (byte) 0xA9, (byte) 0x07, 
    	(byte) 0x88, (byte) 0x99, (byte) 0xA0, (byte) 0xD8, (byte) 0x99, (byte) 0x18, (byte) 0xD9, (byte) 0xCA, 
    	(byte) 0x10, (byte) 0xEB, (byte) 0x98, (byte) 0xD0, (byte) 0xE4, (byte) 0xA9, (byte) 0x0C, (byte) 0x99, 
    	(byte) 0x08, (byte) 0xDA, (byte) 0xC8, (byte) 0xD0, (byte) 0xFA, (byte) 0xA9, (byte) 0x34, (byte) 0x85, 
    	(byte) 0x01, (byte) 0xA0, (byte) 0x00, (byte) 0xB9, (byte) 0x12, (byte) 0x08, (byte) 0x99, (byte) 0xF7, 
    	(byte) 0x00, (byte) 0xC8, (byte) 0xC0, (byte) 0xA4, (byte) 0xD0, (byte) 0xF5, (byte) 0x4C, (byte) 0x14, 
    	(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x6B, (byte) 0x37, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, 
    	(byte) 0x00, (byte) 0x14, (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x00, 
    	(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x09, 
    	(byte) 0x0B, (byte) 0x08, (byte) 0x0C, (byte) 0x0A, (byte) 0x0F, (byte) 0x07, (byte) 0x01, (byte) 0x0D, 
    	(byte) 0x07, (byte) 0x03, (byte) 0x0C, (byte) 0x0E, (byte) 0x04, (byte) 0x06, 
   };

}
