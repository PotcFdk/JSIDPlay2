1999 rem**************************
2000 rem .....device on ?.....
2001 f=peek(768)
2002 open1,y
2003 poke768,185
2004 open1,8,15,"i":close1
2005 poke768,f
2006 ifst<>-128then g$="on":return
2007 g$="off":return
2008 rem y=device
2009 rem g$=floppy on / off
2010 rem**************************
3000 rem .....printer on ?.....
3001 f=peek(768)
3002 open1,4
3003 poke768,185
3004 print#1:close1
3005 poke768,f
3006 ifst<>-128then g$="on":return
3007 g$="off":return
3008 rem g$=printer  on / off
