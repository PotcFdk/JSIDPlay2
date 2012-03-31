; spritecollisionirq
; ------------------
; 2010 Hannu Nuotio

; This program tests if the sprite collision IRQ can retrigger
; without clearing the IRQ.

!ct scr

; --- Macros

; SpriteLine - for easy definition of sprites
; from "ddrv.a" by Marco Baye
!macro SpriteLine .v {
!by .v>>16, (.v>>8)&255, .v&255
}


; --- Constants

screen = $0400
spriteptr = $07f8
spriteloc = $0200

irqcounter = screen + 40*23
irqcounter_type = screen + 40*23 + 1
irqcounter_raster = screen + 40*23 + 2
irqcounter_sprite = screen + 40*23 + 3

sprite0x = 24
sprite0y = 97
sprite1x = sprite0x 
sprite1y = sprite0y
sprite2x = sprite0x + 10
sprite2y = sprite0y + 48
sprite3x = sprite2x
sprite3y = sprite2y

rasterline_1 = sprite0y + 24
rasterline_2 = sprite2y + 24


; --- Variables

; - zero page

strptr = $39            ; zp pointer to string
tmpptr = $56            ; temporary zp pointer 2
scrptr = $fb            ; zp pointer to screen
colptr = $fd            ; zp pointer to color
tmpvar = $ff            ; temporary zp variable
rasternum = $fa         ; number of active raster IRQ


; --- Main

; start of program
*=$0801
entry:
; BASIC stub: "1 SYS 2061"
!by $0b,$08,$01,$00,$9e,$32,$30,$36,$31,$00,$00,$00

mlcodeentry:
; disable sprites
lda #$00
sta $d015

; print some text
lda #>screen
sta scrptr+1
lda #<screen
sta scrptr
lda #>text_msg
sta strptr+1
lda #<text_msg
sta strptr
jsr print_text

; set common sprite stuff
lda #<sprite0x
sta $d000
lda #<sprite1x
sta $d002
lda #<sprite2x
sta $d004
lda #<sprite3x
sta $d006
lda #sprite0y
sta $d001
lda #sprite1y
sta $d003
lda #sprite2y
sta $d005
lda #sprite3y
sta $d007
lda #(((>sprite3x)<<3) + ((>sprite2x)<<2) + ((>sprite1x)<<1) + (>sprite0x))
sta $d010
lda #$0f
sta $d027   ; sprite 0 color
sta $d028   ; sprite 1 color
sta $d029   ; sprite 2 color
sta $d02a   ; sprite 3 color
lda #0
sta $d01b   ; priority
sta $d01c   ; MCM
sta $d01d   ; x expand
sta $d017   ; y expand

; set sprite
lda #>spriteptr
sta tmpptr+1
lda #<spriteptr
sta tmpptr
lda #>spriteloc
sta colptr+1
lda #<spriteloc
sta colptr
jsr set_sprite

; print some text
lda #>screen
sta scrptr+1
lda #<screen
sta scrptr
lda #>text_msg
sta strptr+1
lda #<text_msg
sta strptr
jsr print_text

; reset IRQ counter
lda #0
sta irqcounter

; - interrupt setup
; from "An Introduction to Programming C-64 Demos" by Puterman aka Linus Åkerlund
; http://user.tninet.se/~uxm165t/demo_programming/demo_prog/demo_prog.html
; ... + modifications
;
sei             ; interrupts off
lda #$35
sta $01         ; kernal off
lda #$7f
ldx #$05
sta $dc0d       ; Turn off CIA 1 interrupts
sta $dd0d       ; Turn off CIA 2 interrupts
stx $d01a       ; Turn on raster interrupts, turn on sprite-sprite collision
lda #<int       ; low part of address of interrupt handler code
ldx #>int       ; high part of address of interrupt handler code
sta $fffe       ; store in interrupt vector
stx $ffff
lda #0
jsr set_raster  ; set raster 1 to activate first
lda #<nmi       ; low part of address of NMI handler code
ldx #>nmi       ; high part of address of NMI handler code
sta $fffa       ; store in NMI vector
stx $fffb
lda $dc0d       ; ACK CIA 1 interrupts
lda $dd0d       ; ACK CIA 2 interrupts
asl $d019       ; ACK VIC interrupts
cli             ; interrupts on

; everything ready, enable sprites
lda #$0f
sta $d015

; loop
- jmp -


; --- Subroutines


; - set_sprite
; parameters:
;  colptr -> sprite location
;  tmpptr -> pointer location
;
set_sprite:
ldy #0
- lda sprite,y
sta (colptr),y
iny
cpy #63
bne -
lda colptr+1
sta tmpvar
lda colptr
ldy #6
- clc
ror tmpvar
ror
dey
bne -
ldy #7
- sta (tmpptr),y
dey
bpl -
rts


; - print_text
; parameters:
;  scrptr -> screen location to print to
;  strptr -> string to print
;
print_text:
ldy #0
- lda (strptr),y
beq +++         ; end if zero
sta (scrptr),y  ; print char
iny
bne -           ; loop if not zero
inc strptr+1
inc scrptr+1
bne -
+++
clc
tya             ; update scrptr to next char
adc scrptr
sta scrptr
bcc +
inc scrptr+1
+
rts             ; return

; - set_raster
; parameters:
;  a = num
;
set_raster:
ldx #<rasterline_1
ldy #>rasterline_1
sta rasternum
cmp #$00
beq +
ldx #<rasterline_2
ldy #>rasterline_2
+
stx $d012
lda #$80
cpy #$00
bne +
lda #$00
+
ora $d011
sta $d011
rts


; --- Interrupt routines

; - IRQ
;
int:
inc irqcounter
lda $d019
sta irqcounter_type
sta $d019       ; ACK interrupt (to re-enable it)
; check if the IRQ was from raster
ror
bcs int_raster

int_sprite:
; ...if not, we assume sprite-sprite collision
inc irqcounter_sprite
; show red for sprite IRQ
ldx #2
stx $d021
stx $d020
; note that the collision register is not read here
bne int_exit

int_raster:
inc irqcounter_raster
lda rasternum   ; check raster number
bne int_raster_2

int_raster_1:
; show white for raster IRQ 1
ldx #1
stx $d021
stx $d020

lda #1          ; raster 2 next
bne int_raster_exit

int_raster_2:
; show purple for raster IRQ 2
ldx #4
stx $d021
stx $d020

; the second raster IRQ ACKs the sprite collision IRQs
lda $d01e       ; ACK sprite-sprite collision interrupt
lda $d01f       ; ACK sprite-background collision interrupt

lda #0          ; raster 1 next
int_raster_exit:
jsr set_raster  ; set next raster IRQ location

int_exit:
ldy #0
sty $d021
sty $d020
rti             ; return

; - NMI
;
nmi:
rti             ; return


; --- Data

; - Strings

text_msg
!tx "sprite collision irq testprog v1        "
!tx "raster and spr-spr coll irq enabled     "
!tx "                                        "
!tx "                                        "
!tx "the first sprites (1 pixel, overlap):   "
!tx "                                        "
!tx "                                        "
!tx "the first raster irq (white):           "
!tx "                                        "
!tx "                                        "
!tx "the second sprites (1 pixel, overlap):  "
!tx "                                        "
!tx "                                        "
!tx "the second raster irq (purple:)         "
!tx "                                        "
!tx "                                        "
!tx "only the second raster irq acks the     "
!tx "collision, hence no red line near the   "
!tx "second sprites.                         "
!tx "                                        "
!by 0

; - Sprites

sprite
!if 1 {
;            765432107654321076543210
+SpriteLine %#....................... ;1
+SpriteLine %........................ ;2
+SpriteLine %........................ ;3
+SpriteLine %........................ ;4
+SpriteLine %........................ ;5
+SpriteLine %........................ ;6
+SpriteLine %........................ ;7
+SpriteLine %........................ ;8
+SpriteLine %........................ ;9
+SpriteLine %........................ ;10
+SpriteLine %........................ ;11
+SpriteLine %........................ ;12
+SpriteLine %........................ ;13
+SpriteLine %........................ ;14
+SpriteLine %........................ ;15
+SpriteLine %........................ ;16
+SpriteLine %........................ ;17
+SpriteLine %........................ ;18
+SpriteLine %........................ ;19
+SpriteLine %........................ ;20
+SpriteLine %........................ ;21
} else {
+SpriteLine %#...#..###...###..####.. ;1
+SpriteLine %#...#...#...#.....#..... ;2
+SpriteLine %#...#...#...#.....#..... ;3
+SpriteLine %#...#...#...#.....###... ;4
+SpriteLine %.#.#....#...#.....#..... ;5
+SpriteLine %..#....###...###..####.. ;6
+SpriteLine %........................ ;7
+SpriteLine %........................ ;8
+SpriteLine %####...########..##...## ;9
+SpriteLine %#####..########..##...## ;10
+SpriteLine %#...##....##.....##...## ;11
+SpriteLine %#...##....##.....##...## ;12
+SpriteLine %#...##....##.....##...## ;13
+SpriteLine %#...##....##.....##...## ;14
+SpriteLine %#...##....##.....##...## ;15
+SpriteLine %#####.....##......##.##. ;16
+SpriteLine %####......##.......###.. ;17
+SpriteLine %........................ ;18
+SpriteLine %........................ ;19
+SpriteLine %#.#.#.#.#.#.#.#.#.#.#.#. ;20
+SpriteLine %.#.#.#.#.#.#.#.#.#.#.#.# ;21
}
