.pc = cmdLineVars.get("pc").asNumber()

// entry address
 	coldvec:     .word cold
 
// initial user interrupt vectors
 	irqusr:      .word irqret
// These should never run
 	brkusr:      .word exception
 	nmiusr:      .word exception
 
// redirect basic restart vector
// to finish the init sequence
// (hooks in via stop function)
 	stopusr:     .word setiomap
 
 	playnum:     .byte 0
 	speed:       .byte 0
 	initvec:     .word 0
 	playvec:     .word 0
 	rndwait:     .word 0
 	initiomap:   .byte 0
 	playiomap:   .byte 0
 	video:       .byte 0
 	clock:       .byte 0
 	flags:       .byte 0
 
// init/play PSID
 	play:        jmp (playvec)
 	init:        jmp (initvec)
 
// cold start
 	cold:        sei
 
// set VICII raster to line 311 for RSIDs
 	        ldx #$9b
 	        ldy #$37
 
// we should use the proper values for
// the default raster, however if the tune
// is playing at the wrong speed (e.g.
// PAL at NTSC) use the compatibility
// raster instead to try make it work
 	        eor clock
 	        ora initiomap
 	        beq vicinit
 
// set VICII raster to line 0 for PSIDs
// (compatibility raster)
 	        ldx #$1b
 	        ldy #$00
 	vicinit:     stx $d011
 	        sty $d012
 
// Don't override default irq handler for RSIDs
 	        lda initiomap
 	        beq random
 
// If play address, override default irq vector so
// we reach are routine to handle play routine
 	        lda playiomap
 	        beq random
 	        ldx #<irqjob
 	        stx $0314
 
// simulate time before user loads tune
 	random:      ldx rndwait
 	        ldy rndwait+1
 	        inx
 	        iny
 	wait:        dex
 	        bne wait
 	        dey
 	        bne wait
 
// 0 indicates VIC timing (PSIDs only)
// else it's from CIA
 	        lda speed
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
 	ciaclear:    lda $dc0d
 
// set I/O map and call song init routine
 	        lda initiomap
 	        bne setbank
// Only release interrupt mask for real
// C64 tunes (initiomap = 0) thus
// providing a more realistic environment
 	        lda #$37
 	setbank:     sta $01
 
 	setregs:     lda flags
 	        pha
 	        lda playnum
 	        plp
 	        jsr init
 	setiomap:    lda initiomap
 	        beq idle
 	        lda playiomap
 	        beq run
 	        lda #$37
 	        sta $01
 	run:         cli
 	idle:        jmp idle
 
 	irqjob:      lda $01
 	        pha
 	        lda playiomap
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
 	irqret:      jmp $ea31
 
// HLT
 	exception:   .byte $02
