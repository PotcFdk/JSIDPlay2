;
; Copyright 2007,2008 Martin Wendt / enthusi(ONS)
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program. If not, see <http://www.gnu.org/licenses/>.

!to "neoramdrive-040.prg",cbm 

bankl = $fb
bankm = $fc
bankh = $fd
temp = $fe
temp1 = $a7
temp2 = $a8
temp3 = $a9
temp_c3 = $aa 
temp_cmp = $ab

 *=$0801
 !word basend
 !word 0
 !byte $9e
 !tx "2061"
 !byte 0
basend
 !word 0
 *=$080d
 ldx #$00 ;in case you load
 stx $dfff ;after heavy use of c64 :)
bas_0
 stx $dffe
bas_1
 lda launch_code
 sta $de00,x ;store small launcher at de00...
 inc bas_1+1
 bne bas_2
 inc bas_1+2
bas_2 
 inx
 bne bas_1
bas_3
 lda #01
 sta $dffe ;dfff still at 0
bas_4
 lda start_of_data
 sta $de00,x
 inc bas_4+1
 bne bas_5
 inc bas_4+2
bas_5 
 inx
 bne bas_4
 inc bas_3+1
 lda bas_3+1
 cmp #$07 ;transfer 7 pages.
 ; 7 is good since only now it wont overwrite future
 ; data content at reboot of basic-init-code! 
 beq bas_end
 jmp bas_3
bas_end
 ldx #$00 ;set back to bank 0
 ;so neoramdrive can start right away!
 stx $dfff
 stx $dffe
 ldx #$ff
bas_end1 inx
 lda ini_txt,x
 beq bas_end2
 jsr $ffd2
 bne bas_end1
bas_end2 rts
ini_txt !byte $0d
!tx " * NEORAM-DRIVE NOW LOADED TO NEORAM *"
!byte $0d
!tx " * LAUNCH WITH SYS 57E3.     /N2C[O] *"
!byte $0d, $00
;--------------------------------------
launch_code
;this code is the small kicker at de00 that copies the 
;launcher to $c900 and starts it there
!pseudopc $de00 {
start_of_launcher 
 ldx #(end_of_first_launcher-start_of_launcher)
 ;only copy required no of bytes
sol_1
 lda $de00,x ;adjust x to actual extern code-start
sol_2 sta $c900
 inx
 beq sol_end
 inc sol_2+1
 jmp sol_1
sol_end
 lda #$00
 sta sol_2+1
 jmp $c900
end_of_first_launcher
}
launch_code_over
!pseudopc $c900 {
;----------------------------------
;now comes the actual launcher that will act 
;from $c900 and must be small enough to for 1st page
;i think the drive-code shall start on 2nd page cleanly *g*
second_launch
 ldx #$00
init_0
 lda #$01
 sta $dffe
init_1 
 lda $de00,x
init_2 
 sta $ca00
 inx
 bne init_3
;first page fully read of neoram, go to 2nd page
 inc init_0+1
init_3 
 inc init_2+1
 bne init_4
 inc init_2+2
init_4 
 lda init_2+2
 cmp #$d0 ;written till $cfff?
 bne init_0
 jmp $ca00 ; launch drive code (finally)
}
!scr "this is the launchercode.presented to you by enthusi of onslaught."
!scr "please report bugs and feature requests."
sys_starter
jumptable !byte $4c,$00,$de
!scr "thanks to x1541 for the design of the neoram!"
!scr "contact me via mail c64@enthusi.de /2008"
;-----------------------------------------
;here starts the true drive-code but
;beware it starts at 0,8,0 (l,m,h) address 
;check for labels: startpage0..3
;also ramsize
start_of_data
!pseudopc $ca00 {
start_of_code
 lda #$02 ;drive no
 sta $02
 lda $0330 
 sta load 
 lda $0331 
 sta load+1 
 lda #<load_prg
 sta $0330 
 lda #>load_prg 
 sta $0331 
 lda $0332 
 sta save 
 lda $0333 
 sta save+1 
 lda #<save_prg
 sta $0332 
 lda #>save_prg
 sta $0333 
 lda #$00
 sta $d020 
 ldx #$00 
print lda text,x 
 jsr $ffd2 
 beq print_end 
 inx 
 bne print 
print_end rts 
text
 !byte $93, $0d
 !tx " ** NEORAM-DRIVE N2C/ONSLAUGHT V0.40 **"
 !byte $0d, $00
save 
 !byte $ea
 !byte $f5
load 
 !byte $a2
 !byte $f4
save_prg 
 ldy $ba
 cpy $02
 beq ramsave
 jmp (save)
ramsave sei 
 jsr set_bank_ram
 jsr seek_end 
 jsr next_byte 
 jsr next_byte 
 jsr next_byte 
 ldy #$00 
name sty $030c 
 ldy $030c 
 lda ($bb),y 
 ldy #$00 
 jsr save_byte 
 jsr next_byte 
 ldy $030c 
 iny 
 cpy $b7 
 beq endname 
 jmp name 
endname lda #$00 
 ldy #$00 
 jsr save_byte 
 jsr next_byte 
 lda $c1 
 jsr save_byte 
 jsr next_byte 
 lda $c2 
 jsr save_byte 
 jsr next_byte 
saveloop 
 lda $c2 
 cmp $af 
 bne noend 
 lda $c1 
 cmp $ae 
 beq endsave 
noend lda ($c1),y 
 jsr save_byte 
 jsr next_byte 
 jsr next_byte2 
 jmp saveloop 
endsave 
 lda #$00 
 jsr save_byte 
 lda bankl 
 sta $030c 
 lda bankm
 sta $030d 
 lda bankh
 sta $030e
 jsr next_byte 
 lda #$00 
 jsr save_byte 
 jsr next_byte 
 lda #$00 
 jsr save_byte 
 jsr seek_end 
 lda $030c 
 jsr save_byte 
 jsr next_byte 
 lda $030d 
 jsr save_byte 
 jsr next_byte 
 lda $030e 
 jsr save_byte 
 clc 
 jsr set_bank_rom
 cli 
 rts 
next_byte 
 inc bankl
 bne set_end
 inc bankm
 bne set_end
 inc bankh
set_end
 rts
next_byte2
 inc $c1 
 bne nopage 
 inc $c2 
nopage rts 
seek_end
startpage0 lda #$07 
 sta bankm
 lda #$00
 sta bankl
 sta bankh
findloop 
 ldy #$00 
 jsr get_byte 
 sta temp1
 jsr next_byte 
 jsr get_byte 
 sta temp2
 jsr next_byte 
 jsr get_byte 
 sta temp3
 lda temp1
 bne not_0
 lda temp2
 bne not_0
 lda temp3
 bne not_0
 jmp endfound
not_0 
 ldx temp1
 stx bankl
 ldx temp2
 stx bankm
 ldx temp3
 stx bankh
 jmp findloop 
endfound 
 lda bankl
 sec 
 sbc #$02
 sta bankl
 lda bankm
 sbc #$00 
 sta bankm
 lda bankh
 sbc #$00 
 sta bankh
 clc 
 rts 
load_prg
 pha 
 lda $ba 
 cmp $02 
 beq okload 
 pla 
 jmp (load) 
okload pla 
 sta $93 
 ldy #$00 
 lda ($bb),y 
 cmp #$5f ;"<-"
 bne spr0
 jmp format
spr0 cmp #$5c ;"pound"
 bne spr1 
 jmp scratch 
spr1 cmp #$24 ;"$"
 bne spr2 
 jmp directory 
spr2 sei 
 jsr set_bank_ram
 jsr search 
 lda $b9 
 bne absload 
 lda $c3 
 sta $c1 
 lda $c4 
 sta $c2 
 jsr next_byte 
 jsr next_byte 
 jmp loadloop 
absload jsr get_byte 
 sta $c1 
 jsr next_byte 
 jsr get_byte 
 sta $c2 
 jsr next_byte 
loadloop 
 lda bankh
 cmp $b0
 bne load1
 lda bankm
 cmp $af
 bne load1 
 lda bankl
 cmp $ae 
 beq endload 
load1 lda $93 
 bne verify 
 jsr get_byte 
 sta ($c1),y 
verifyed jsr next_byte 
 jsr next_byte2 
 jmp loadloop 
verify jsr get_byte 
 cmp ($c1),y 
 beq verifyed 
 jsr set_bank_rom
 cli 
 lda #$1c 
 jmp $f715 
endload ldx $c1 
 ldy $c2 
 jsr set_bank_rom
 cli 
 lda #$40 
 sta $90 
 clc 
 rts 
search 
startpage1 lda #$07
 sta bankm
 lda #$00
 sta bankl
 sta bankh
nameloop1 
 ldy #$00 
 sty $030c 
 lda bankl
 sta $c1
 lda bankm
 sta $c2
 lda bankh
 sta temp_c3
 jsr get_byte 
 sta temp1
 jsr next_byte 
 jsr get_byte 
 sta temp2
 jsr next_byte 
 jsr get_byte 
 sta temp3
 lda temp1
 bne not_0_2
 lda temp2
 bne not_0_2
 lda temp3
 bne not_0_2
 jmp notfound
not_0_2
 lda temp1
 sta $ae
 lda temp2
 sta $af
 lda temp3
 sta $b0
 jsr next_byte 
nameloop ldy $030c 
 lda ($bb),y 
 cmp #$2a 
 beq ast 
 cmp #$3f 
 beq joker 
 ldy #$00 
 pha 
 jsr get_byte 
 beq found 
 sta temp_cmp
 pla 
 cmp temp_cmp
 bne next 
joker ldy $030c 
 cpy $b7 
 beq next 
 iny 
 sty $030c 
 jsr next_byte 
 jmp nameloop 
found pla 
 ldy $030c 
 cpy $b7 
 bne next 
foundnull 
 jsr next_byte 
 ldy #$00 
 rts 
notfound pla 
 pla 
 jsr set_bank_rom
 cli 
 jmp $f704 ;error-msg
next 
 lda $ae
 sta bankl
 lda $af
 sta bankm
 lda $b0
 sta bankh
 jmp nameloop1 
ast ldy #$00 
seek_0 jsr next_byte 
 jsr get_byte 
 bne seek_0 
 jmp foundnull 
directory 
 sei 
 ldx #$ff
cat2 inx 
 lda text0,x
 jsr $ffd2
 bne cat2
 jsr set_bank_ram
startpage2 lda #$07
 sta $af
 sta bankm
 lda #$00
 sta bankl
 sta bankh
 sta $ae
 sta $b0
catloop 
 ldy #$00 
 jsr get_byte 
 sta temp1
 jsr next_byte 
 jsr get_byte 
 sta temp2
 jsr next_byte 
 jsr get_byte 
 sta temp3
 lda temp1
 bne not_0_3
 lda temp2
 bne not_0_3
 lda temp3
 bne not_0_3
 jmp nopgm
not_0_3
 sec
 lda temp1
 sbc $ae
 sta pal
 lda temp2
 sbc $af
 sta pam
 jsr set_bank_rom
 ldx pam
 cpx #100
 bcs bignum
 lda #$20
 jsr $ffd2
bignum 
 cpx #10
 bcs realbignum
 lda #$20
 jsr $ffd2
realbignum
 inx ; to avoid 0-block size (still in total free!)
 txa
 jsr printdec
 lda #$20
 jsr $ffd2
 lda #$22
 jsr $ffd2
 jsr set_bank_ram
 lda temp1
 sta $ae
 lda temp2
 sta $af
 lda temp3
 sta $b0
 ldy #$00 
p_name jsr next_byte 
 jsr get_byte 
 beq endnme 
 pha 
 jsr set_bank_rom
 cli 
 pla 
 jsr $ffd2 
 sei 
 jsr set_bank_ram
 jmp p_name 
endnme lda $ae 
 sta bankl
 lda $af 
 sta bankm
 lda $b0
 sta bankh
 jsr set_bank_rom
 cli
 lda #$22
 jsr $ffd2
 lda #$0d
 jsr $ffd2
 sei
 jsr set_bank_ram
 jmp catloop
nopgm
 jsr set_bank_rom
 cli
 lda #$0d
 jsr $ffd2
 sec
 lda #$ff
 sbc bankm
 tax
ramsize
 lda #$1f;#$07 for 512KB
 sbc bankh
 jsr $bdcd ;dec-print
 ldx #$00
bytesfree lda text1,x
 beq dir_end
 jsr $ffd2
 inx
 bne bytesfree 
dir_end clc
 ldx $2d
 ldy $2e
 lda #$40
 sta $90 ;st
 rts
text0 !byte $0d,$12
 !tx "  NEORAM-DRIVE  "
 !byte $92,$0d,$0d,$00
text1 !tx " BLOCKS FREE"
 !byte $00
pal !byte $00
pam !byte $00
pah !byte $00
pbl !byte $00
pbm !byte $00
pbh !byte $00
s_l !byte $00 ;src
s_m !byte $00
s_h !byte $00
d_l !byte $00 ;dst
d_m !byte $00
d_h !byte $00
l_l !byte $00 ;len
l_m !byte $00
l_h !byte $00
es_l !byte $00
es_m !byte $00
es_h !byte $00
scratch sei 
 jsr set_bank_ram
 dec $b7
 inc $bb
 bne np
 inc $bc
np jsr search
 jsr seek_end
 jsr next_byte
 jsr next_byte
 jsr next_byte
 jsr next_byte

 lda bankl
 sta es_l
 lda bankm
 sta es_m
 lda bankh
 sta es_h

 sec 
 lda $ae
 sta s_l
 sbc $c1
 sta l_l
 lda $af
 sta s_m
 sbc $c2
 sta l_m
 lda $b0
 sta s_h
 sbc temp_c3
 sta l_h
nichtab 
 lda $c1
 sta d_l
 lda $c2 
 sta d_m 
 lda temp_c3
 sta d_h
move_new 
 lda s_l
 sta bankl
 lda s_m
 sta bankm
 lda s_h
 sta bankh
 jsr get_byte 
 pha
 jsr next_byte
 lda bankl
 sta s_l
 lda bankm
 sta s_m
 lda bankh
 sta s_h
 lda d_l 
 sta bankl
 lda d_m
 sta bankm
 lda d_h
 sta bankh
 pla
 jsr save_byte 
 jsr next_byte
 lda bankl
 sta d_l
 lda bankm
 sta d_m
 lda bankh
 sta d_h
 lda s_l
 cmp es_l
 bne move_new
 lda s_m
 cmp es_m
 bne move_new
 lda s_m
 cmp es_m
 bne move_new
korrloop ldy #$00 
 lda $c1
 sta bankl
 lda $c2
 sta bankm
 lda temp_c3
 sta bankh
korr2 jsr get_byte 
 sta pal
 sec
 sbc l_l 
 sta pbl
 php
 jsr save_byte 
 jsr next_byte 
 jsr get_byte 
 sta pam
 plp 
 sbc l_m
 sta pbm
 php
 jsr save_byte 
 jsr next_byte 
 jsr get_byte 
 sta pah
 plp 
 sbc l_h
 sta pbh
 jsr save_byte 
 lda pal
 bne not_yet_end
 lda pam
 bne not_yet_end
 lda pah
 bne not_yet_end
 lda bankl
 sec 
 sbc #$02
 sta bankl
 lda bankm
 sbc #$00 
 sta bankm
 lda bankh
 sbc #$00 
 sta bankh
 lda #$00
 jsr save_byte
 jsr next_byte
 jsr save_byte
 jsr next_byte
 jsr save_byte
 jmp endmove_new
not_yet_end
 lda pbl
 sta bankl
 lda pbm
 sta bankm
 lda pbh
 sta bankh
 jmp korr2
endmove_new 
 clc 
 ldx $2d 
 ldy $2e 
 jsr set_bank_rom
 cli 
 lda #$40 
 sta $90 
 rts 
format 
 sei
 inc $d020
 jsr set_bank_ram
startpage3
 lda #$07
 sta bankm
 lda #$00
 sta bankl
 sta bankh
 jsr save_byte
 jsr next_byte
 jsr save_byte
 jsr next_byte
 jsr save_byte
 jsr next_byte
 jsr save_byte
 jsr next_byte
 sta bankl
 sta bankm
 sta bankh
 jsr set_bank_rom
 dec $d020
 cli
 rts
get_byte 
 lda bankm
 sta temp
 and #%00111111
 sta $dffe
 lda bankh
 asl temp
 rol
 asl temp
 rol
 sta $dfff
 ldx bankl
 lda $de00,x
get_x 
 rts
save_byte
 stx save_x+1
 sty save_y+1
 sta save_a+1
 tay
 lda bankm
 sta temp
 and #%00111111
 sta $dffe
 lda bankh
 asl temp
 rol
 asl temp
 rol
 sta $dfff
 ldx bankl
 tya
 sta $de00,x
save_x
 ldx #$00
save_y
 ldy #$00
save_a
 lda #$00
 rts
set_bank_ram
 lda $01 
 and #%11111101
 sta $01 
 rts
set_bank_rom
 lda $01 
 ora #$03
 sta $01 
 rts
printdec
 ldx #0 
pdloop
 jsr div10 
 pha 
 inx 
 cpy #0 
 beq pdloop2 
 tya 
 jmp pdloop 
pdloop2 pla 
 ora #$30
 jsr $ffd2 
 dex 
 bne pdloop2 
div10 sec 
 ldy #$ff 
divlp iny 
 sbc #10 
 bcs divlp 
 adc #10 
 rts 
}
