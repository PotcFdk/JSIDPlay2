Commodore RAM Expansion Unit (REU) Controller (REC) quick behavior test
Version 1.1.1, (C) Copyright 2008-12-16, Wolfgang Moser, http://d81.de

This program is a behavior compatibility test program for the Commodore RAM
Expansion Unit, intended as unit test for emulation developers.  
This software program is licensed as freeware, use it at your own risk.


This test program comprises the findings that have been collected to write up
the technical reference about the Commodore REU's controller chip, the
CSG8726R1. This technical documentation tries to describe the hardware
implementation aspects of the 8726R1 controller chip of the Commodore REU
[8726R1] as they show up to a programmer. The current release version of this
technical reference in electronic form can be found here:

 http://zimmers.net/anonftp/pub/cbm/documents/chipdata/CSG8726TechRefDoc-1.0.zip


The test program presented here checks for several of the more weird findings
with 12 dedicated test procedures. Please take note that the test procedures
don't work in an exhaustive way with probing each and every possible flag and
command combination. This test is intended to do a quick check for certain known
characteristics, to discover some incompatibilities and to give support in
finding any regressions in the development process, it does not do a full
compatibility check. 


When the test program finds a difference between the tested REU emulation
(hardware or software), for most of the test procedures it prints out register
dumps for the expected and the delivered register values. Have a look at the
self-test dump (this one is always printed to check, if the comparison routine
does work correctly):

    REU register assertion failed for test=self test, must fail
       expected: 0xc5/ef,0x82,0x1234,0x56789a/07ffff,0xbcde,0x3f,0xff,0xe2/ef,22146
       but got:  0x10/ef,0x11,0xe577,0xf80003/07ffff,0x0001,0x7f,0xbf,0x00/ef,4

In the first line the test name is given, here it is "self test, must fail".
Then the two dumps for the expected and the actually given register results are
printed.
  * The first 7 values represent the 10 registers of the Commodore REU,
    grouped into the status register (0xDF00), command (0xDF01), C64 base
    address (0xDF02..3), REU base address (0xDF04..6), transfer length
    (0xDF07..8), IRQ mask (0xDF09) and fixed addressing mode register (0xDF0A).
  * The next value is different from 0x00, when an IRQ was enabled and triggered
    from the REU, it represents the value that was read out from the REU's
    status register by the interrupt service routine.
  * The last value is a timer measurement value and tells the amount of clock
    cycles that the REU consumed for the most recent opertation

The groups for the status register, REU base address and the IRQ served status
register values are followed by a mask setting, e.g. "/07ffff".  Only the bits
that are set in the mask are considered to conduct the comparison. For the two
status register values, bit 4 is always taken out (DRAM page size configuration
bit), so effectively e.g. the values 0x40 and 0x50 are considered equal. Also
the REU base address bank register mostly comes with a mask setting, so that
only the lowest three bits are taken for the comparson. Since some software REU
emulations support fully featured 8-bit read/write bank registers, the correct
mask setting is autodetected upon program start. Btw. for such extended banking
registers, there is an incompatibility reported, but only one time.  


For each detected incompatibility, there are hints given, where to look up for
explanations with details about the expected behavior. Section numbers are
enclosed in parentheses that refer to version 1.0 of the above mentioned REU
technical reference. These section numbers also fit to the current work in
progress version 1.4 of that document and most probably also to the upcoming
release version, be it either version 1.5 or 2.0.


2008-12-16, Wolfgang Moser, http://d81.de
