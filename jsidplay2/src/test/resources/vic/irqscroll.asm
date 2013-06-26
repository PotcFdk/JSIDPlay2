;1x1 scrolling message inside IRQ by Richard Bayliss              

smooth = $02      ;Control for smooth scroll
screenloc = $0798 ;This is the line for where the scroll is placed

                 !to "irqscroll.prg",cbm
*= $0801
	!byte $0c,$08,$00,$00,$9e,$20,$34,$30,$39,$36,$00,$00


*= $1000  					;Code nach $1000
                 sei
                 jsr $ff81
                 lda #<message
                 ldy #>message
                 sta read+1
                 sty read+2
                 lda #<interrupt1
                 ldx #>interrupt1
                 ldy #$1b
                 sta $314
                 stx $315
                 sty $d011
                 lda #$7f
                 sta $dc0d
                 lda #$01
                 sta $d01a
                 cli
hold             jmp hold
interrupt1       inc $d019
                 lda #$00 
                 sta $d012
                 lda smooth ;Scroll section
                 sta $d016
                 lda #<interrupt2
                 ldx #>interrupt2
                 sta $314
                 stx $315
                 jmp $ea7e
interrupt2       inc $d019
                 lda #$e0
                 sta $d012
                 lda #$08 ;No scroll section here
                 sta $d016
                 lda #<interrupt1
                 ldx #>interrupt1
                 sta $314
                 stx $315
                 jsr scroll
                 jmp $ea7e
                 
scroll           lda smooth
                 sec
                 sbc #$01 ;Speed of scroll can be edited to how you want it, but don't go too mad :)
                 and #$07 ;We need this to make the variable smooth into something smooth :)
                 sta smooth
                 bcs endscroll 
                 ldx #$00
wrapmessage      lda screenloc+1,x
                 sta screenloc,x
                 inx
                 cpx #$28
                 bne wrapmessage
read             lda screenloc+$27
                 cmp #$00 ;Is byte 0 (@) read?
                 bne nowrap ;If not, goto label nowrap
                 lda #<message
                 ldy #>message
                 sta read+1
                 sty read+2
                 jmp read
nowrap           sta screenloc+$27
                 inc read+1
                 lda read+1
                 cmp #$00
                 bne endscroll
                 inc read+2
endscroll        rts

message          !scr "hello folks. this is a 1x1 char message scroller"
                 !scr " inside an IRQ interrupt, coded by richard bayliss"
                 !scr " of the new dimension, in acme cross assembler ... "
                 !scr "we do hope you like it ;o)) ... hopefully you should"
                 !scr "find the source code useful for future demos and stu"
                 !scr "ff if you want to learn to do scroll texts ...      "
                 !scr "wrap time!                                          "
                 !byte 0