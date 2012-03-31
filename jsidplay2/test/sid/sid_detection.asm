;SID DETECTION ROUTINE
*=$C000	
	;By SounDemon - Based on a tip from Dag Lem.
	;Put together by FTC after SounDemons instructions
	;...and tested by Rambones and Jeff.
	
	; - Don't run this routine on a badline
	; - Won't work in VICE (always detects 6581)
	
	sei		;No disturbing interrupts
	lda #$ff
badline
	cmp $d012	;Don't run it on a badline.
	bne badline
	
	;Detection itself starts here	
	lda #$ff	;Set frequency in voice 3 to $ffff 
	sta $d412	;...and set testbit (other bits doesn't matter) in $d012 to disable oscillator
	sta $d40e
	sta $d40f
	lda #$20	;Sawtooth wave and gatebit OFF to start oscillator again.
	sta $d412
	lda $d41b	;Accu now has different value depending on sid model (6581=3/8580=2)
	lsr		;...that is: Carry flag is set for 6581, and clear for 8580.
	bcc model_8580
model_6581
	lda #$01
	sta $0400
	rts

model_8580
	lda #$02
	sta $0400
	rts
