/**
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package libsidplay.mem;

public interface IPSIDDrv {
	/**
	 * <PRE>
	 * 	        ; entry address
	 * 	coldvec     .word cold
	 * 
	 * 	        ; initial user interrupt vectors
	 * 	irqusr      .word irqret
	 * 	        ; These should never run
	 * 	brkusr      .word exception
	 * 	nmiusr      .word exception
	 * 
	 * 	        ; redirect basic restart vector
	 * 	        ; to finish the init sequence
	 * 	        ; (hooks in via stop function)
	 * 	stopusr     .word setiomap
	 * 
	 * 	playnum     .byte 0
	 * 	speed       .byte 0
	 * 	initvec     .word 0
	 * 	playvec     .word 0
	 * 	rndwait     .word 0
	 * 	initiomap   .byte 0
	 * 	playiomap   .byte 0
	 * 	video       .byte 0
	 * 	clock       .byte 0
	 * 	flags       .byte 0
	 * 
	 * 	        ; init/play PSID
	 * 	play        jmp (playvec)
	 * 	init        jmp (initvec)
	 * 
	 * 	        ; cold start
	 * 	cold        sei
	 * 
	 * 	        ; setup hardware
	 * 	doinit      ldy $02a6
	 * 	        lda video
	 * 	        sta $02a6
	 * 	        pha
	 * 	        jsr $ff84
	 * 	        pla
	 * 	        sty $02a6
	 * 
	 * 	        ; set VICII raster to line 311 for RSIDs
	 * 	        ldx #$9b
	 * 	        ldy #$37
	 * 
	 * 	        ; we should use the proper values for
	 * 	        ; the default raster, however if the tune
	 * 	        ; is playing at the wrong speed (e.g.
	 * 	        ; PAL at NTSC) use the compatibility
	 * 	        ; raster instead to try make it work
	 * 	        eor clock
	 * 	        ora initiomap
	 * 	        beq vicinit
	 * 
	 * 	        ; set VICII raster to line 0 for PSIDs
	 * 	        ; (compatibility raster)
	 * 	        ldx #$1b
	 * 	        ldy #$00
	 * 	vicinit     stx $d011
	 * 	        sty $d012
	 * 
	 * 	        ; Don't override default irq handler for RSIDs
	 * 	        lda initiomap
	 * 	        beq random
	 * 
	 * 	        ; If play address, override default irq vector so
	 * 	        ; we reach are routine to handle play routine
	 * 	        lda playiomap
	 * 	        beq random
	 * 	        ldx #&lt;irqjob
	 * 	        stx $0314
	 * 
	 * 	        ; simulate time before user loads tune
	 * 	random      ldx rndwait
	 * 	        ldy rndwait+1
	 * 	        inx
	 * 	        iny
	 * 	wait        dex
	 * 	        bne wait
	 * 	        dey
	 * 	        bne wait
	 * 
	 * 	        ; 0 indicates VIC timing (PSIDs only)
	 * 	        ; else it's from CIA
	 * 	        lda speed
	 * 	        bne ciaclear
	 * 
	 * 	        ; disable CIA 1 timer A interrupt but
	 * 	        ; leave timer running for random numbers
	 * 	        lda #$7f
	 * 	        sta $dc0d
	 * 
	 * 	        ; clear any pending irqs
	 * 	        lda $d019
	 * 	        sta $d019
	 * 
	 * 	        ; enable VICII raster interrupt
	 * 	        lda #$81
	 * 	        sta $d01a
	 * 
	 * 	        ; clear any pending irqs
	 * 	ciaclear    lda $dc0d
	 * 
	 * 	        ; set I/O map and call song init routine
	 * 	        lda initiomap
	 * 	        bne setbank
	 * 	        ; Only release interrupt mask for real
	 * 	        ; C64 tunes (initiomap = 0) thus
	 * 	        ; providing a more realistic environment
	 * 	        lda #$37
	 * 	setbank     sta $01
	 * 
	 * 	setregs     lda flags
	 * 	        pha
	 * 	        lda playnum
	 * 	        plp
	 * 	        jsr init
	 * 	setiomap    lda initiomap
	 * 	        beq idle
	 * 	        lda playiomap
	 * 	        beq run
	 * 	        lda #$37
	 * 	        sta $01
	 * 	run         cli
	 * 	idle        jmp idle
	 * 
	 * 	irqjob      lda $01
	 * 	        pha
	 * 	        lda playiomap
	 * 	        sta $01
	 * 	        lda #0
	 * 	        jsr play
	 * 	        pla
	 * 	        sta $01
	 * 	        dec $d019
	 * 	        lda $dc0d
	 * 	        pla
	 * 	        tay
	 * 	        pla
	 * 	        tax
	 * 	        pla
	 * 	        rti
	 * 
	 * 	        ; IRQ Exit (code from Kernel ROM)
	 * 	        ; This loop through is not needed but is
	 * 	        ; to ensure compatibility with psid64
	 * 	irqret      jmp $ea31
	 * 
	 * 	        ; HLT
	 * 	exception   .byte $02
	 * 
	 * 	.end
	 * </PRE>
	 */
	public static final byte PSIDDRV[] = {
		(byte) 0x01, (byte) 0x00, (byte) 0x6F, (byte) 0x36, (byte) 0x35, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x10, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1D, (byte) 0x10, (byte) 0xBC, (byte) 0x10, (byte) 0xBF,
		(byte) 0x10, (byte) 0xBF, (byte) 0x10, (byte) 0x8E, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x6C, (byte) 0x0E, (byte) 0x10, (byte) 0x6C, (byte) 0x0C, (byte) 0x10,
		(byte) 0x78, (byte) 0xAC, (byte) 0xA6, (byte) 0x02, (byte) 0xAD, (byte) 0x14, (byte) 0x10, (byte) 0x8D,
		(byte) 0xA6, (byte) 0x02, (byte) 0x48, (byte) 0xea, (byte) 0xea, (byte) 0xea, (byte) 0x68, (byte) 0x8C,
		(byte) 0xA6, (byte) 0x02, (byte) 0xA2, (byte) 0x9B, (byte) 0xA0, (byte) 0x37, (byte) 0x4D, (byte) 0x15,
		(byte) 0x10, (byte) 0x0D, (byte) 0x12, (byte) 0x10, (byte) 0xF0, (byte) 0x04, (byte) 0xA2, (byte) 0x1B,
		(byte) 0xA0, (byte) 0x00, (byte) 0x8E, (byte) 0x11, (byte) 0xD0, (byte) 0x8C, (byte) 0x12, (byte) 0xD0,
		(byte) 0xAD, (byte) 0x12, (byte) 0x10, (byte) 0xF0, (byte) 0x0A, (byte) 0xAD, (byte) 0x13, (byte) 0x10,
		(byte) 0xF0, (byte) 0x05, (byte) 0xA2, (byte) 0xA0, (byte) 0x8E, (byte) 0x14, (byte) 0x03, (byte) 0xAE,
		(byte) 0x10, (byte) 0x10, (byte) 0xAC, (byte) 0x11, (byte) 0x10, (byte) 0xE8, (byte) 0xC8, (byte) 0xCA,
		(byte) 0xD0, (byte) 0xFD, (byte) 0x88, (byte) 0xD0, (byte) 0xFA, (byte) 0xAD, (byte) 0x0B, (byte) 0x10,
		(byte) 0xD0, (byte) 0x10, (byte) 0xA9, (byte) 0x7F, (byte) 0x8D, (byte) 0x0D, (byte) 0xDC, (byte) 0xAD,
		(byte) 0x19, (byte) 0xD0, (byte) 0x8D, (byte) 0x19, (byte) 0xD0, (byte) 0xA9, (byte) 0x81, (byte) 0x8D,
		(byte) 0x1A, (byte) 0xD0, (byte) 0xAD, (byte) 0x0D, (byte) 0xDC, (byte) 0xAD, (byte) 0x12, (byte) 0x10,
		(byte) 0xD0, (byte) 0x02, (byte) 0xA9, (byte) 0x37, (byte) 0x85, (byte) 0x01, (byte) 0xAD, (byte) 0x16,
		(byte) 0x10, (byte) 0x48, (byte) 0xAD, (byte) 0x0A, (byte) 0x10, (byte) 0x28, (byte) 0x20, (byte) 0x1A,
		(byte) 0x10, (byte) 0xAD, (byte) 0x12, (byte) 0x10, (byte) 0xF0, (byte) 0x0A, (byte) 0xAD, (byte) 0x13,
		(byte) 0x10, (byte) 0xF0, (byte) 0x04, (byte) 0xA9, (byte) 0x37, (byte) 0x85, (byte) 0x01, (byte) 0x58,
		(byte) 0x4C, (byte) 0x9D, (byte) 0x10, (byte) 0xA5, (byte) 0x01, (byte) 0x48, (byte) 0xAD, (byte) 0x13,
		(byte) 0x10, (byte) 0x85, (byte) 0x01, (byte) 0xA9, (byte) 0x00, (byte) 0x20, (byte) 0x17, (byte) 0x10,
		(byte) 0x68, (byte) 0x85, (byte) 0x01, (byte) 0xCE, (byte) 0x19, (byte) 0xD0, (byte) 0xAD, (byte) 0x0D,
		(byte) 0xDC, (byte) 0x68, (byte) 0xA8, (byte) 0x68, (byte) 0xAA, (byte) 0x68, (byte) 0x40, (byte) 0x4C,
		(byte) 0x31, (byte) 0xEA, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x82, (byte) 0x02,
		(byte) 0x82, (byte) 0x02, (byte) 0x82, (byte) 0x02, (byte) 0x82, (byte) 0x02, (byte) 0x82, (byte) 0x10,
		(byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x07, (byte) 0x82, (byte) 0x12, (byte) 0x82, (byte) 0x03,
		(byte) 0x82, (byte) 0x0F, (byte) 0x82, (byte) 0x05, (byte) 0x82, (byte) 0x05, (byte) 0x22, (byte) 0x05,
		(byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x0B, (byte) 0x82, (byte) 0x18, (byte) 0x82, (byte) 0x09,
		(byte) 0x82, (byte) 0x04, (byte) 0x82, (byte) 0x04, (byte) 0x82, (byte) 0x03, (byte) 0x82, (byte) 0x05,
		(byte) 0x82, (byte) 0x0A, (byte) 0x82, (byte) 0x06, (byte) 0x82, (byte) 0x07, (byte) 0x82, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00,
	};
}
