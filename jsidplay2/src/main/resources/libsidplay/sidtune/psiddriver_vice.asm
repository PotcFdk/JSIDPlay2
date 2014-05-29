.pc = cmdLineVars.get("pc").asNumber()

coldAddr:	jmp cold

// CBM80 backup
cbm80:	.byte 0, 0, 0, 0, 0, 0, 0, 0, 0

// Parameters
speed:	.word cmdLineVars.get("speed").asNumber() & $ffff
		.word [cmdLineVars.get("speed").asNumber()>>16] & $ffff
iomap:	.byte cmdLineVars.get("playIOMap").asNumber()

irqusr:	.word irqjob 

// Cold start
// Turn interrupts off
cold:	lda #$00
	sta $d01a
	lda $d019
	sta $d019
	lda #$7f
	sta $dc0d
	sta $dd0d
	lda $dc0d
	lda $dd0d

// Restore CBM80
	ldx #$08
store80:	lda cbm80,x
	sta $8000,x
	dex
	bpl store80

// Set interrupt vectors
// Don't step on the vectors if they're part of the PSID image
	lda #>cmdLineVars.get("loadAddr").asNumber()
	cmp #$03
	bcc vicras
	bne initirq
	lda #<cmdLineVars.get("loadAddr").asNumber()
	cmp #$1a
	bcc vicras
initirq:	ldx #$05
store03:	lda irqusr,x
	sta $0314,x
	dex
	bpl store03

// Set VICII raster to line 0
vicras:	lda #$1b
	ldx #$00
	sta $d011
	stx $d012

// Set CIA 1 Timer A to 60Hz
	lda #cmdLineVars.get("videoMode").asNumber()
	beq ntsc
pal:	lda #$25
	ldx #$40
	bne timer
ntsc:	lda #$95
	ldx #$42
timer:	sta $dc04
	stx $dc05

// Maximum volume
	lda #$0f
	sta $d418

// Get song number
	ldy #cmdLineVars.get("songNum").asNumber()
	dey
	cpy #cmdLineVars.get("songs").asNumber()
	bcc songset
	ldy #$00
songset:	tya
	pha

// No PLAY => tune must set up interrupts itself (CIA 1 running)
	lda #>cmdLineVars.get("playAddr").asNumber()
	beq ciainit
	jsr getmap
	sta iomap

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
	ldx #$01
	sta $dc0d
	stx $dc0e

// Set I/O map and call song init routine
doinit:	lda #$2f
	sta $00
	lda #>cmdLineVars.get("initAddr").asNumber()
	jsr getmap
	sta $01
	pla
	jsr init
	lda #>cmdLineVars.get("playAddr").asNumber()
	beq run
	lda #$36
	sta $01

run:	cli
idle:	jmp idle

// init/play PSID
init:	jmp cmdLineVars.get("initAddr").asNumber()
play:	jmp cmdLineVars.get("playAddr").asNumber()

// Get required I/O map to reach address
getmap:	cmp #$e0
	bcc kern_on
	lda #$35
	rts
kern_on:	cmp #$d0
	bcc io_on
	lda #$34
	rts
io_on:	lda #$36
	rts

// IRQ handler
irqjob:	lda #>cmdLineVars.get("playAddr").asNumber()
	beq noplay
	lda iomap
	sta $01
	jsr play
	lda #$36
	sta $01
noplay:	lda $d019
	sta $d019
	lda $dc0d
brkAddr:	pla
	tay
	pla
	tax
	pla
	rti

nmiAddr:  bit $dd0d
	rti 