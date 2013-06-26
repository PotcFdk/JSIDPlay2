
*=$C000
;???????????????????????????????????????
; CIA-Timing                            
;???????????????????????????????????????
          sei                           
in1
          lda $d011                     
          bpl in1
in2                                    
          lda $d011                     
          bmi in2                       
          ldy #5                        
          jsr waitline                  
          ldx #32                       
          ldy #0                        
in3                                    
          lda $d012                     
          cmp #10                       
          bne in3                       
          stx $dd04                     
          sty $dd05                     
          lda #$11                      
          sta $dd0e
          lda #129                      
          sta $dd0d                     
          ldx #<nmi                     
          ldy #>nmi                     
          stx $0318
          sty $0319                     
          bit 0                         
          bit 0                         
	byte $ea                              
	byte $ea                              
	byte $ea                              
	byte $ea                              
	byte $ea                              
	byte $ea                              
	byte $ea                              
	byte $ea                              
	byte $ea                              
nmi                                    
          ldx $dd04                     
          pla                           
          pla                           
          pla                           
          lda $dd0d                     
          lda #127
          sta $dd0d
          lda #8                        
          sta $dd0e                     
          cli                           
          stx ciatiming                 
                                        
          lda #$47                      
          sta $0318                     
          lda #$fe
          sta $0319
rts

waitline
          cpy $d012
          bne waitline
          ldx #10
de1
          dex
          bne de1
          iny
          cpy $d012                     
          nop                           
          beq sk1                       
          cmp 0                         
          nop
sk1                                    
          ldx #9                        
de2                                    
          dex                           
          bne de2                       
          iny
          nop                           
          nop
          cpy $d012                     
          nop                           
          beq sk2                       
          cmp 0                         
sk2                                    
          ldx #10                       
de3                                    
          dex                           
          bne de3
          iny                           
          cpy $d012                     
          bne sk3                       
sk3
          rts                           

ciatiming
 byte 0
