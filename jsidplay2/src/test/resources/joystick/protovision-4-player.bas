600 poke56322,224:z=int(4*rnd(.))+1:onzgoto620,660,700,780
620 j=peek(56321)
625 if(jand1)=.thenprint"joy 1 oben"
630 if(jand2)=.thenprint"joy 1 unten"
635 if(jand4)=.thenprint"joy 1 links"
640 if(jand8)=.thenprint"joy 1 rechts"
645 if(jand16)=.thenprint "joy 1 feuer"
660 j=peek(56320)
665 if(jand1)=.thenprint"joy 2 oben"
670 if(jand2)=.thenprint"joy 2 unten"
675 if(jand4)=.thenprint"joy 2 links"
680 if(jand8)=.thenprint"joy 2 rechts"
685 if(jand16)=.thenprint"joy 2 feuer"
700 poke56579,128
710 poke56577,peek(56577)or128
720 j=peek(56577)
730 if(jand1)=.thenprint"joy 3 oben"
740 if(jand2)=.thenprint"joy 3 unten"
750 if(jand4)=.thenprint"joy 3 links"
760 if(jand8)=.thenprint"joy 3 rechts"
770 if(jand16)=.thenprint "joy 3 feuer"
780 poke56577,peek(56577)and127
790 j=peek(56577)
800 if(jand1)=.thenprint"joy 4 oben"
810 if(jand2)=.thenprint"joy 4 unten"
820 if(jand4)=.thenprint"joy 4 links"
830 if(jand8)=.thenprint"joy 4 rechts"
835 if(jand32)=.thenprint"joy 4 feuer"
840 goto620