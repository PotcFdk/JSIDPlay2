.pc = cmdLineVars.get("pc").asNumber()
 
// init/play PSID
 play:		jmp cmdLineVars.get("playAddr").asNumber()
 init:		jmp cmdLineVars.get("initAddr").asNumber()
 
irqusr:	.word irqjob 
brkusr:	.word irqexit
nmiusr:	.word nmijob

// cold start
 start:	sei
 
// set VICII raster to line 311 for RSIDs
	        ldx #$9b
 	        ldy #$37
 
// we should use the proper values for
// the default raster, however if the tune
// is playing at the wrong speed (e.g.
// PAL at NTSC) use the compatibility
// raster instead to try make it work
 	        eor #cmdLineVars.get("videoMode").asNumber()
 	        ora #cmdLineVars.get("initIOMap").asNumber()
 	        beq vicinit
 
// set VICII raster to line 0 for PSIDs
// (compatibility raster)
 	        ldx #$1b
 	        ldy #$00
vicinit:	stx $d011
 	        sty $d012
 
// Don't override default irq handler for RSIDs
 	        lda #cmdLineVars.get("initIOMap").asNumber()
 	        beq random
 
// If play address, override default irq vector so
// we reach are routine to handle play routine
 	        lda #cmdLineVars.get("playIOMap").asNumber()
 	        beq random

// Set interrupt vectors
// Don't step on the vectors if they're part of the PSID image
			lda #>cmdLineVars.get("loadAddr").asNumber()
			cmp #$03
			bcc random
			bne initirq
			lda #<cmdLineVars.get("loadAddr").asNumber()
			cmp #$1a
			bcc random
initirq:	ldx #$05
store03:	lda irqusr,x
			sta $0314,x
			dex
			bpl store03

// simulate time before user loads tune
random:		ldx #<cmdLineVars.get("powerOnDelay").asNumber()
 	        ldy #>cmdLineVars.get("powerOnDelay").asNumber()
 	        inx
 	        iny
wait:		dex
 	        bne wait
 	        dey
 	        bne wait
 
// 0 indicates VIC timing (PSIDs only)
// else it's from CIA
 	        lda #cmdLineVars.get("songSpeed").asNumber()
 	        bne ciaclear
 
// disable CIA 1 timer A interrupt but
// leave timer running for random numbers
 	        lda #$7f
 	        sta $dc0d
 
// clear any pending irqs
 	        lda $d019
 	        sta $d019
 
// enable VICII raster interrupt
 	        lda #$81
 	        sta $d01a
 
// clear any pending irqs
ciaclear:	lda $dc0d
 
// set I/O map and call song init routine
			lda #cmdLineVars.get("initIOMap").asNumber()
 	        bne setbank
// Only release interrupt mask for real
// C64 tunes (initiomap = 0) thus
// providing a more realistic environment
 	        lda #$37
setbank:	sta $01
 
setregs:	lda #cmdLineVars.get("flags").asNumber()
 	        pha
 	        lda #cmdLineVars.get("songNum").asNumber()-1
 	        plp
 	        jsr init
setiomap:	lda #cmdLineVars.get("initIOMap").asNumber()
 	        beq idle
 	        lda #cmdLineVars.get("playIOMap").asNumber()
 	        beq run
 	        lda #$37
 	        sta $01
run:		cli
idle:		jmp idle
 
irqjob:		lda $01
 	        pha
 	        lda #cmdLineVars.get("playIOMap").asNumber()
 	        sta $01
 	        lda #0
 	        // inc $d020
 	        jsr play
 	        // dec $d020
 	        pla
 	        sta $01
 	        dec $d019
 	        lda $dc0d
 	        pla
 	        tay
 	        pla
 	        tax
 	        pla
 	        rti
 
// IRQ Exit (code from Kernel ROM)
// This loop through is not needed but is
// to ensure compatibility with psid64
irqAddr:	jmp $ea31
 
// HLT
irqexit:
nmijob:
			.byte $02
