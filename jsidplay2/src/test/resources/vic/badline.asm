; screenpos
; --------
; Test interrupt delay with badline.

; PAL only. The test sets up NMI interrupts simultaneously with VIC
; badlines and programs them to trigger at variable times with respect
; to the badline start position. The end result shows how interrupts
; are delayed with respect to badline.

; Program structure:
;
; - establish a stable on raster interrupt on the line preceding display.
; - program CIA timer to trigger right before a badline
; - in the CIA interrupt, change display color to a value and back
; - program next interrupt 7 rasterlines later & with 1 larger CIA delay,
;   repeat until end of screen.

; --- Consts

cinv = $fffe
cnmi = $fffa
raster = 48     ; start of raster interrupt
; try 7 and 71 to observe behavior at left and right edges of DMA.
topnmidelay = 7

; --- Code, based on:
; http://codebase64.org/doku.php?id=base:double_irq&s[]=stable&s[]=raster 

*=$0801
; "1 SYS 2061".
basic: !by $0b,$08,$01,$00,$9e,$32,$30,$36,$31,$00,$00,$00
; 2061 ->
    sei         ;Disable IRQs
    lda #$7f    ;Disable CIA IRQs
    sta $dc0d
    sta $dd0d

    ldx #0
    lda #99
clear:
    sta $400,x
    sta $500,x
    sta $600,x
    sta $700,x
    dex
    bne clear

    lda #$35    ;Bank out kernal and basic
    sta $01     ;$e000-$ffff
 
    lda #<irq1  ;Install RASTER IRQ
    ldx #>irq1  ;into Hardware
    sta $fffe   ;Interrupt Vector
    stx $ffff
 
    lda #$01    ;Enable RASTER IRQs
    sta $d01a
    lda #raster ;IRQ on line #raster
    sta $d012
    lda #$1b    ;High bit (lines 256-311)
    sta $d011
 
    lda #<nmi
    ldx #>nmi
    sta $fffa
    stx $fffb

    lda #$81    ; allow TA interrupt on CIA at $dc
    sta $dd0d
    lda #0      ; clear TAH
    sta $dd05

    asl $d019   ;Ack any previous
    bit $dc0d   ;IRQs
    bit $dd0d

    cli         ;Allow IRQ's
    jmp *       ;Endless Loop

nmi:
    sta reseta2
    lda $dd0d

    sta $d020
    stx $d020
    sty $d020
    stx $d020
    sta $d020
    stx $d020

    lda #$00
reseta2 = *-1
    rti

irq1:
     sta reseta1 ;Preserve A,X and Y
     stx resetx1 ;Registers
     sty resety1 ;VIA self modifying
                 ;code
                 ;(Faster than the
                 ;STACK is!)
 
     lda #<irq2  ;Set IRQ Vector
     ldx #>irq2  ;to point to the
                 ;next part of the
     sta $fffe   ;Stable IRQ
     stx $ffff   ;ON NEXT LINE!
     inc $d012
     asl $d019   ;Ack RASTER IRQ
     tsx         ;We want the IRQ
     cli         ;To return to our
     nop         ;endless loop
     nop         ;NOT THE END OF
     nop         ;THIS IRQ!
     nop
     nop         ;Execute nop's
     nop         ;until next RASTER
     nop         ;IRQ Triggers
     nop
     nop         ;2 cycles per
     nop         ;instruction so
     nop         ;we will be within
     nop         ;1 cycle of RASTER
     nop         ;Register change
     nop
irq2
     txs         ;Restore STACK
                 ;Pointer
     ldx #$08    ;Wait exactly 1
     dex         ;lines worth of
     bne *-1     ;cycles for compare
     bit $ea     ;Minus compare

     ldx $d012
     cpx $d012
     beq start   ;If no waste 1 more
                 ;cycle
start

  ; clear CRA
  lda #0
  sta $dd0e

  ; program CIA to trigger N clocks from now
  lda nmipos
  sta $dd04

  ; cause CIA to trigger
  ldx #$19
  stx $dd0e

  ; cause 1-clock offset on every second NMI to allow IRQ delivery at
  ; 1-clock increments rather than 2-clock increments.
  and #1
  bne *+2

  ; enough NOPs to cover all our delay values
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop

; 1. establish stable rasters throughout screen region,
;    by programming new interrupts 8 lines from now.
  lda irqpos
  cmp #$f7
  bcc skiprestart

; next addition would overflow, let's just start over.
  lda #raster
  sta $d012
  sta irqpos

  lda #topnmidelay
  sta nmipos
  jmp endirq2

skiprestart:
  cld
  clc
  adc #$8
  sta $d012
  sta irqpos

  inc nmipos

endirq2:
     lda #<irq1  ;Set IRQ to point
     ldx #>irq1  ;to subsequent IRQ
     sta $fffe
     stx $ffff
     asl $d019   ;Ack RASTER IRQ
 
     lda #$00    ;Reload A,X,and Y
reseta1  = *-1       ;registers
     ldx #$00
resetx1  = *-1
     ldy #$00
resety1  = *-1
     rti         ;Return from IRQ

irqpos: !by raster
nmipos: !by topnmidelay
 
