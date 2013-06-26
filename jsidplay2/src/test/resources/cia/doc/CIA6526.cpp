/* TODO:
- fix bug with Test Suite CIAxTx
- test Loopback extension
- test PC line
- implement and test TOD
- implement and test SDR
*/

////////////////////////////////////////////////////////////////////////////////
// CIA6526.cpp -- this file is part of the Emulator Developers Kit
// available at http://ourworld.compuserve.com/homepages/pc64/develop.htm

RegisterPersistentClass(CIA6526);

// save time by switching into idle mode if there are no pending events
const flag gfAllowIdle = true;

// do not set idle if a counter will underrun in less that n clocks
const int giIdleThreshold = 4;


////////////////////////////////////////////////////////////////////////////////
// initialisation

void CIA6526::DoInit() {

  // initialize base class
  Chip::DoInit();

  // set function to call at each clock
  SetOnClock((pfn)OnClock);
  SetBusy();

  // initialize components
  Reset.Init("Reset", this);
  Reset.SetOnHigh((pfn)OnReset);
  Reset.SetOnLow((pfn)OnReset);
  InitPorts();
  InitTimers();
  InitTOD();
  InitSDR();
  InitControl();

  // reset components
  OnReset();
};


////////////////////////////////////////////////////////////////////////////////
// reset components

void CIA6526::OnReset() {
  ResetPorts();
  ResetTimers();
  ResetTOD();
  ResetSDR();
  ResetControl();
}


////////////////////////////////////////////////////////////////////////////////
// bit masks for dwDelay and dwFeed
// dwDelay is shift left by one at each clock

#define CountA0     0x00000001
#define CountA1     0x00000002
#define CountA2     0x00000004
#define CountA3     0x00000008
#define CountB0     0x00000010
#define CountB1     0x00000020
#define CountB2     0x00000040
#define CountB3     0x00000080
#define LoadA0      0x00000100
#define LoadA1      0x00000200
#define LoadB0      0x00000400
#define LoadB1      0x00000800
#define PB6Low0     0x00001000
#define PB6Low1     0x00002000
#define PB7Low0     0x00004000
#define PB7Low1     0x00008000
#define Interrupt0  0x00010000
#define Interrupt1  0x00020000
#define OneShotA0   0x00040000
#define OneShotB0   0x00080000
#define DelayMask ~(0x00100000 | CountA0 | CountB0 | LoadA0 | LoadB0 | PB6Low0 | PB7Low0 | Interrupt0 | OneShotA0 | OneShotB0)


////////////////////////////////////////////////////////////////////////////////
// switch from idle mode back to busy after dwDelay or dwFeed have changed

void CIA6526::NewDelayOrFeed() {
  if (IsIdle()) {
    if (TimerIdle.IsCounting()) {
      int iClocks = TimerIdle.ReadCounter();
      if ((dwDelay & CountA3) != 0) {
        iCounterA += iClocks;
      }
      if ((dwDelay & CountB3) != 0) {
        iCounterB += iClocks;
      }
      TimerIdle.StopCounter();
    }
    SetBusy();
  }
}


////////////////////////////////////////////////////////////////////////////////
// switch from idle mode back to busy before a timer underflows

void CIA6526::OnTimerIdleFire() {
  SetBusy();
}


////////////////////////////////////////////////////////////////////////////////
// Port Registers PRA (0) and PRB (1)
// Data Direction Registers DDRA (2) and DDRB (3)
//
// Written values are stored in the latch. Open inputs are always high.
// After switching a line from input to output, it will go low if the
// corresponding bit in the latch is 0.
//
// To save time, PC is only drawn low for one clock if it is connected.
//
// byte bPALatch;        // buffered output values
// byte bPBLatch;
// byte bDDRA;           // directions: 0 = input, 1 = output
// byte bDDRB;
//
// Port PA;              // bidirectional ports with open collector outputs
// Port PB;
// Line PC;              // low for one clock after reading/writing PB
//
// Timer PCNextClock;    // draws PC to high again at the next clock
// void OnPCNextClock();

void CIA6526::InitPorts() {
  PA.Init("PA", this);
  PB.Init("PB", this);
  PC.Init("PC", this);
  PCNextClock.Init("PCNextClock", this);
  PCNextClock.SetOnFire((pfn)OnPCNextClock);
}

void CIA6526::ResetPorts() {
  bPALatch = 0;
  bPBLatch = 0;
  bDDRA = 0;
  bDDRB = 0;
  PA.SetOutput(0xFF);
  PB.SetOutput(0xFF);
  if (!PC.IsOutputHigh()) {
    PC.SetOutputHigh();
    assert(PCNextClock.IsCounting());
    PCNextClock.StopCounter();
  }
}

void CIA6526::OnPCNextClock() {
  assert(PC.IsOutputLow());
  PC.SetOutputHigh();
}

byte CIA6526::ReadPRA() {
  return PA.GetInput();
}

void CIA6526::WritePRA(byte bValue) {
  bPALatch = bValue;
  PA.SetOutput(bPALatch | ~bDDRA);
}

byte CIA6526::ReadPRB() {
  if (PC.IsConnected()) { // save time
    assert(PC.IsOutputHigh());
    PC.SetOutputLow();
    assert(!PCNextClock.IsCounting());
    PCNextClock.StartCounter(1);
  }
  return PB.GetInput();
}

void CIA6526::WritePRB(byte bValue) {
  if (PC.IsConnected()) { // save time
    assert(PC.IsOutputHigh());
    PC.SetOutputLow();
    assert(!PCNextClock.IsCounting());
    PCNextClock.StartCounter(1);
  }
  bPBLatch = bValue;
  PB.SetOutput(((bPBLatch | ~bDDRB) & ~bPB67TimerMode) | (bPB67TimerOut & bPB67TimerMode));
}

byte CIA6526::ReadDDRA() {
  return bDDRA;
}

void CIA6526::WriteDDRA(byte bValue) {
  bDDRA = bValue;
  PA.SetOutput(bPALatch | ~bDDRA);
}

byte CIA6526::ReadDDRB() {
  return bDDRB;
}

void CIA6526::WriteDDRB(byte bValue) {
  bDDRB = bValue;
  PB.SetOutput(((bPBLatch | ~bDDRB) & ~bPB67TimerMode) | (bPB67TimerOut & bPB67TimerMode));
}


////////////////////////////////////////////////////////////////////////////////
// Timers TA (4/5) and TB (6/7)
//
// Written values are stored in the latch. The timer will count back from the
// latch to -1. To get a period of x clocks, you must set the latch to x-1.
// Writing to the high byte when the timer is stopped loads the counter from
// the latch.

void CIA6526::InitTimers() {
  TimerIdle.Init("TimerIdle", this);
  TimerIdle.SetOnFire((pfn)OnTimerIdleFire);
}


void CIA6526::ResetTimers() {
  iCounterA = 0;
  iLatchA = 0xFFFF;
  iCounterB = 0;
  iLatchB = 0xFFFF;
}

byte CIA6526::ReadTALow() {
  if (TimerIdle.IsCounting() && (dwDelay & CountA3) != 0) {
    return (byte)(iCounterA + TimerIdle.ReadCounter());
  } else {
    return (byte)iCounterA;
  }
}

byte CIA6526::ReadTBLow() {
  if (TimerIdle.IsCounting() && (dwDelay & CountB3) != 0) {
    return (byte)(iCounterB + TimerIdle.ReadCounter());
  } else {
    return (byte)iCounterB;
  }
}

byte CIA6526::ReadTAHigh() {
  if (TimerIdle.IsCounting() && (dwDelay & CountA3) != 0) {
    return (byte)((iCounterA + TimerIdle.ReadCounter()) >> 8);
  } else {
    return (byte)(iCounterA >> 8);
  }
}

byte CIA6526::ReadTBHigh() {
  if (TimerIdle.IsCounting() && (dwDelay & CountB3) != 0) {
    return (byte)((iCounterB + TimerIdle.ReadCounter()) >> 8);
  } else {
    return (byte)(iCounterB >> 8);
  }
}

void CIA6526::WriteTALow(byte bValue) {
  ((byte*)&iLatchA)[0] = bValue;
}

void CIA6526::WriteTBLow(byte bValue) {
  ((byte*)&iLatchB)[0] = bValue;
}

void CIA6526::WriteTAHigh(byte bValue) {
  ((byte*)&iLatchA)[1] = bValue;

  // load counter if timer is stopped
  if ((bCRA & 0x01) == 0) {
    dwDelay |= LoadA0;
    NewDelayOrFeed();
  }
}

void CIA6526::WriteTBHigh(byte bValue) {
  ((byte*)&iLatchB)[1] = bValue;

  // load counter if timer is stopped
  if ((bCRB & 0x01) == 0) {
    dwDelay |= LoadB0;
    NewDelayOrFeed();
  }
}


////////////////////////////////////////////////////////////////////////////////
// Serial Data Register SDR (C)
//
// Line CNT;             // alternative counter input or gate
// void OnCNTHigh();

void CIA6526::OnCNTHigh() {
}

void CIA6526::InitSDR() {
  CNT.Init("CNT", this);
  CNT.SetOnHigh((pfn)OnCNTHigh);
}

void CIA6526::ResetSDR() {
}

byte CIA6526::ReadSDR() {
  return 0;
}

void CIA6526::WriteSDR(byte /*bValue*/) {

}


////////////////////////////////////////////////////////////////////////////////
// control and interrupt registers CRA (E), CRB (F), ICR/IMR (D)
//
// dword dwDelay;        // performs delay by shifting left at each clock
// dword dwFeed;         // new bits to feed into dwDelay
// byte bCRA;            // control register A
// byte bCRB;            // control register B
// byte bICR;            // interrupt control register
// byte bIMR;            // interrupt mask register
// byte bPB67TimerMode;  // bit mask for PB outputs: 0 = port register, 1 = timer
// byte bPB67TimerOut;   // PB outputs bits 6 and 7 in timer mode
// byte bPB67Toggle;     // PB outputs bits 6 and 7 in toggle mode
// byte abControlFill[1];
//
// void OnFlagLow();     // set ICR4 on falling edge

void CIA6526::InitControl() {
  Int.Init("Int", this);
  Flag.Init("Flag", this);
  Flag.SetOnLow((pfn)OnFlagLow);
}

void CIA6526::ResetControl() {
  dwDelay = 0;
  dwFeed = 0;
  bCRA = 0;
  bCRB = 0;
  bICR = 0;
  bIMR = 0;
  bPB67TimerMode = 0;
  bPB67TimerOut = 0;
  bPB67Toggle = 0;
  if (Int.IsOutputLow()) {
    Int.SetOutputHigh();
  }
}

void CIA6526::OnFlagLow() {
  bICR |= 0x10;
  // TODO: check IMR
}


////////////////////////////////////////////////////////////////////////////////
// read control register A

byte CIA6526::ReadCRA() {
  return (byte)(bCRA & ~0x10);
}


////////////////////////////////////////////////////////////////////////////////
// read control register B

byte CIA6526::ReadCRB() {
  return (byte)(bCRB & ~0x10);
}


////////////////////////////////////////////////////////////////////////////////
// write control register A

void CIA6526::WriteCRA(byte bValue) {

  // save old values for recognizing changes
  dword dwSavedDelay = dwDelay;
  dword dwSavedFeed = dwFeed;

  // output PB67 changes only once
  flag fPBChanged = false;

  // set clock in o2 mode // todo cnt
  if ((bValue & 0x21) == 0x01) {
    dwDelay |= CountA1 | CountA0;
    dwFeed |= CountA0;
  } else {
    dwDelay &= ~(CountA1 | CountA0);
    dwFeed &= ~CountA0;
  }

  // set one shot mode
  if ((bValue & 0x08) != 0) {
    dwFeed |= OneShotA0;
  } else {
    dwFeed &= ~OneShotA0;
  }

  // set force load
  if ((bValue & 0x10) != 0) {
    dwDelay |= LoadA0;
  }

  // set toggle high on rising edge of Start
  if ((bValue & 0x01) != 0 && (bCRA & 0x01) == 0) {
    if ((bCRA & 0x06) == 0x06 && (bPB67Toggle & 0x40) != 0x40) {
      fPBChanged = true;
    }
    bPB67Toggle |= 0x40;
  }

  // timer A output to PB6
  if ((bValue & 0x06) != (bCRA & 0x06)) {
    fPBChanged = true;
  }
  if ((bValue & 0x02) == 0) {
    bPB67TimerMode &= ~0x40;
  } else {
    bPB67TimerMode |= 0x40;
    if ((bValue & 0x04) == 0) {
      if ((dwDelay & PB7Low1) == 0) {
        bPB67TimerOut &= ~0x40;
      } else {
        bPB67TimerOut |= 0x40;
      }
    } else {
      bPB67TimerOut = (bPB67TimerOut & ~0x40) | (bPB67Toggle & 0x40);
    }
  }

  // write PB67 if they have changed
  if (fPBChanged) {
    PB.SetOutput(((bPBLatch | ~bDDRB) & ~bPB67TimerMode) | (bPB67TimerOut & bPB67TimerMode));
  }

  // set the register
  bCRA = bValue;

  // switch back to busy mode if something has changed
  if (dwDelay != dwSavedDelay || dwFeed != dwSavedFeed) {
    NewDelayOrFeed();
  }
}


////////////////////////////////////////////////////////////////////////////////
// write control register B

void CIA6526::WriteCRB(byte bValue) {

  // save old values for recognizing changes
  dword dwSavedDelay = dwDelay;
  dword dwSavedFeed = dwFeed;

  // output PB67 changes only once
  flag fPBChanged = false;

  // set clock in o2 mode // todo cnt
  if ((bValue & 0x61) == 0x01) {
    dwDelay |= CountB1 | CountB0;
    dwFeed |= CountB0;
  } else {
    dwDelay &= ~(CountB1 | CountB0);
    dwFeed &= ~CountB0;
  }

  // set one shot mode
  if ((bValue & 0x08) != 0) {
    dwFeed |= OneShotB0;
  } else {
    dwFeed &= ~OneShotB0;
  }

  // set force load
  if ((bValue & 0x10) != 0) {
    dwDelay |= LoadB0;
  }

  // set toggle high on rising edge of Start
  if ((bValue & 0x01) != 0 && (bCRB & 0x01) == 0) {
    if ((bCRB & 0x06) == 0x06 && (bPB67Toggle & 0x80) != 0x80) {
      fPBChanged = true;
    }
    bPB67Toggle |= 0x80;
  }

  // timer B output to PB7
  if ((bValue & 0x06) != (bCRB & 0x06)) {
    fPBChanged = true;
  }
  if ((bValue & 0x02) == 0) {
    bPB67TimerMode &= ~0x80;
  } else {
    bPB67TimerMode |= 0x80;
    if ((bValue & 0x04) == 0) {
      if ((dwDelay & PB7Low1) == 0) {
        bPB67TimerOut &= ~0x80;
      } else {
        bPB67TimerOut |= 0x80;
      }
    } else {
      bPB67TimerOut = (bPB67TimerOut & ~0x80) | (bPB67Toggle & 0x80);
    }
  }

  // write PB67 if they have changed
  if (fPBChanged) {
    PB.SetOutput(((bPBLatch | ~bDDRB) & ~bPB67TimerMode) | (bPB67TimerOut & bPB67TimerMode));
  }

  // set the register
  bCRB = bValue;

  // switch back to busy mode if something has changed
  if (dwDelay != dwSavedDelay || dwFeed != dwSavedFeed) {
    NewDelayOrFeed();
  }
}


////////////////////////////////////////////////////////////////////////////////
// read Interrupt Control Register ICR

byte CIA6526::ReadICR() {
  byte bSavedICR = bICR;

  // get status of the Int line into bit 7 and draw Int high
  if (Int.IsOutputLow()) {
    bSavedICR |= 0x80;
    Int.SetOutputHigh();
  }

  // discard pending interrupts
  dwDelay &= ~(Interrupt0 | Interrupt1);

  // set all events to 0
  bICR = 0;
  return bSavedICR;
}


////////////////////////////////////////////////////////////////////////////////
// write Interrupt Mask Register IMR

void CIA6526::WriteIMR(byte bValue) {
  
  // bit 7 means set (1) or clear (0) the other bits
  if ((bValue & 0x80) != 0) {
    bIMR |= (bValue & 0x1F);
  } else {
    bIMR &= ~(bValue & 0x1F);
  }

  // raise an interrupt in the next cycle if condition matches
  if ((bIMR & bICR) != 0) {
    if (!Int.IsOutputLow()) {
      dwDelay |= Interrupt0;
      NewDelayOrFeed();
    }
  }
}


////////////////////////////////////////////////////////////////////////////////
// convert ASM calling convention in Chip chain to C++

__declspec(naked) void CIA6526::OnClock() {
  static CIA6526* p;
  __asm mov p,ESI
  p->OnClockC();
  __asm mov ESI,[ESI]Chip.pNextChip
  __asm jmp [ESI]Chip.pfnOnClock
}


////////////////////////////////////////////////////////////////////////////////
// called at each system clock

void CIA6526::OnClockC() {

  // don't output PB67 changes more than once
  flag fPBChanged = false;

  // decrement counter A
  if ((dwDelay & CountA3) != 0) {
    iCounterA--;
  }

  // underflow counter A
  if (iCounterA == 0 && (dwDelay & CountA2) != 0) {

    // signal underflow event
    bICR |= 0x01;

    // underflow interrupt in next clock
    if ((bIMR & 0x01) != 0) {
      dwDelay |= Interrupt0;
    }

    // toggle underflow counter bit
    bPB67Toggle ^= 0x40;

    // timer A output to PB6
    if ((bCRA & 0x02) != 0) {

      // set PB6 high for one clock
      if ((bCRA & 0x04) == 0) {
        bPB67TimerOut |= 0x40;
        dwDelay |= PB6Low0;
        dwDelay &= ~PB6Low1;

      // toggle PB6 between high and low
      } else {
        bPB67TimerOut ^= 0x40;
        assert((bPB67TimerOut & 0x40) == (bPB67Toggle & 0x40));
      }

      // output new state
      fPBChanged = true;
    }

    // stop timer in one shot mode
    if (((dwDelay | dwFeed) & OneShotA0) != 0) {
      bCRA &= ~0x01;
      dwDelay &= ~(CountA2 | CountA1 | CountA0);
      dwFeed &= ~CountA0;
    }

    // timer A output to timer B in cascade mode
    if ((bCRB & 0x61) == 0x41 || (bCRB & 0x61) == 0x61 && CNT.IsInputHigh()) {
      dwDelay |= CountB1;
    }

    // load counter A
    dwDelay |= LoadA1;
  }

  // load counter A
  if ((dwDelay & LoadA1) != 0) {
    iCounterA = iLatchA;

    // don't decrement counter in next clock
    dwDelay &= ~CountA2;
  }

  // decrement counter B
  if ((dwDelay & CountB3) != 0) {
    iCounterB--;
  }

  // underflow counter B
  if (iCounterB == 0 && (dwDelay & CountB2) != 0) {

    // signal underflow event
    bICR |= 0x02;

    // underflow interrupt in next clock
    if ((bIMR & 0x02) != 0) {
      dwDelay |= Interrupt0;
    }

    // toggle underflow counter bit
    bPB67Toggle ^= 0x80;

    // timer B output to PB7
    if ((bCRB & 0x02) != 0) {

      // set PB7 high for one clock
      if ((bCRB & 0x04) == 0) {
        bPB67TimerOut |= 0x80;
        dwDelay |= PB7Low0;
        dwDelay &= ~PB7Low1;

      // toggle PB7 between high and low
      } else {
        bPB67TimerOut ^= 0x80;
        assert((bPB67TimerOut & 0x80) == (bPB67Toggle & 0x80));
      }

      // output new state
      fPBChanged = true;
    }

    // stop timer in one shot mode
    if (((dwDelay | dwFeed) & OneShotB0) != 0) {
      bCRB &= ~0x01;
      dwDelay &= ~(CountB2 | CountB1 | CountB0);
      dwFeed &= ~CountB0;
    }

    // load counter B
    dwDelay |= LoadB1;
  }

  // load counter B
  if ((dwDelay & LoadB1) != 0) {
    iCounterB = iLatchB;

    // don't decrement counter in next clock
    dwDelay &= ~CountB2;
  }

  // set PB67 back to low
  if ((dwDelay & (PB6Low1 | PB7Low1)) != 0) {
    if ((dwDelay & PB6Low1) != 0) {
      bPB67TimerOut &= ~0x40;
    }
    if ((dwDelay & PB7Low1) != 0) {
      bPB67TimerOut &= ~0x80;
    }
    fPBChanged = true;
  }

  // write new PB if it has changed
  if (fPBChanged) {
    PB.SetOutput(((bPBLatch | ~bDDRB) & ~bPB67TimerMode) | (bPB67TimerOut & bPB67TimerMode));
  }

  // set interrupt register and interrupt line
  if ((dwDelay & Interrupt1) != 0) {
    if (Int.IsOutputHigh()) {
      Int.SetOutputLow();
    }
  }

  // next clock
  dword dwNewDelay = (dwDelay << 1) & DelayMask | dwFeed;

  // link out of clock chain if there are no more pending events
  assert(!TimerIdle.IsCounting());
  if (gfAllowIdle && dwNewDelay == dwDelay) {
    switch (dwDelay & (CountA3 | CountB3)) {
    case CountA3:
      if (iCounterA >= giIdleThreshold) {
        TimerIdle.StartCounter(iCounterA - 1);
        iCounterA = 1;
        SetIdle();
      }
      break;
    case CountB3:
      if (iCounterB >= giIdleThreshold) {
        TimerIdle.StartCounter(iCounterB - 1);
        iCounterB = 1;
        SetIdle();
      }
      break;
    case CountA3 | CountB3:
      if (iCounterA >= giIdleThreshold && iCounterB >= giIdleThreshold) {
        int iClocks = min(iCounterA, iCounterB) - 1;
        TimerIdle.StartCounter(iClocks);
        iCounterA -= iClocks;
        iCounterB -= iClocks;
        SetIdle();
      }
      break;
    default:
      SetIdle();
    }
  }
  dwDelay = dwNewDelay;
}


////////////////////////////////////////////////////////////////////////////////
// Time Of Day (8/9/A/B)
//

void CIA6526::InitTOD() {
  TOD.Init("TOD", this);
  TOD.SetOnHigh((pfn)OnTODHigh);
}

void CIA6526::ResetTOD() {

}

void CIA6526::OnTODHigh() {

}

byte CIA6526::ReadTOD10ths() {
  return abTime[0];
}

void CIA6526::WriteTOD10ths(byte bValue) {
  abTime[0] = bValue;
}

byte CIA6526::ReadTODSec() {
  return abTime[1];
}

void CIA6526::WriteTODSec(byte bValue) {
  abTime[1] = bValue;
}

byte CIA6526::ReadTODMin() {
  return abTime[2];
}

void CIA6526::WriteTODMin(byte bValue) {
  abTime[2] = bValue;
}

byte CIA6526::ReadTODHr() {
  return abTime[3];
}

void CIA6526::WriteTODHr(byte bValue) {
  abTime[3] = bValue;
}







/*
  CIA Time Of Day TOD

  What are the values of abTime and abAlarm after a Reset?

  Will a Reset affect bTODCounter?

  Will a Reset stop the clock?

  Is the time incrementing on the rising or on the falling edge of TOD?

  Does writing to alarm HR stop the clock? Does writing to alarm 10THS
  restart the clock?

  Does changing time and alarm registers set ICR2 immediately?

  Will ICR2 be cleared after 1/10 sec? Does changing time and alarm
  registers clear ICR2?

  Will writing to a register affect bTODCounter?

  Will changes in PRA7 affect the current bTODCounter cycle or the next?

  Will bTODCounter be stopped when the clock is stopped?

  CLine TOD;            // 50 Hz PAL, 60 Hz NTSC
  byte abTime[4];       // current time
  byte abBufTime[4];    // frozen time after reading hour
  byte abAlarm[4];      // alarm time
  byte bTODCounter;     // counter 1/50th or 1/60th sec
  byte bStopFlag;       // clock is stopped after writing to HR
  byte bBufFlag;        // read from abBufTime instead of abTime
  byte abTODFill[1];

extern void fn(OnTODHigh)();
proc(ResetTOD)
  mov dword ptr mvar(abTime),0x91000000
  mov dword ptr mvar(abAlarm),0x91000000
  mov mvar(bTODCounter),6
  mov mvar(bStopFlag),0
  mov mvar(bBufFlag),0
  ret
endp

proc(ReadTOD_10THS)
  push ESI
  mov ESI,ECX

  cmp mvar(bBufFlag),0
  jne Buffered
  mov AL,mvar(abTime[0])

  pop ESI
  ret
Buffered:
  mov AL,mvar(abBufTime[0])
  mov mvar(bBufFlag),0

  pop ESI
  ret
endp

proc(ReadTOD_SEC)
  push ESI
  mov ESI,ECX

  cmp mvar(bBufFlag),0
  jne Buffered
  mov AL,mvar(abTime[1])

  pop ESI
  ret
Buffered:
  mov AL,mvar(abBufTime[1])

  pop ESI
  ret
endp

proc(ReadTOD_MIN)
  push ESI
  mov ESI,ECX

  cmp mvar(bBufFlag),0
  jne Buffered
  mov AL,mvar(abTime[2])

  pop ESI
  ret
Buffered:
  mov AL,mvar(abBufTime[2])

  pop ESI
  ret
endp

proc(ReadTOD_HR)
  push ESI
  mov ESI,ECX

  mov EAX,dword ptr mvar(abTime)
  mov dword ptr mvar(abBufTime),EAX
  mov mvar(bBufFlag),1
  mov AL,mvar(abBufTime[3])

  pop ESI
  ret
endp

proc(CompAlarm)
  mov EAX,dword ptr mvar(abTime)
  cmp EAX,dword ptr mvar(abAlarm)
  je Alarm
  ret
Alarm:
  or mvar(bICR),00000100b
  test mvar(bIMR),00000100b
  je NoInt
  SetLow(mvar(Int), 1)
NoInt:
  ret
endp

proc(WriteTOD_10THS)
  push ESI
  mov ESI,ECX
  mov EAX,[ESP+8]

  test mvar(bCRB),10000000b
  jne SetAlarm
  mov mvar(abTime[0]),AL
  mov mvar(bStopFlag),0
  call fn(CompAlarm)

  pop ESI
  ret 4
SetAlarm:
  mov mvar(abAlarm[0]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
endp

proc(WriteTOD_SEC)
  push ESI
  mov ESI,ECX
  mov EAX,[ESP+8]

  test mvar(bCRB),10000000b
  jne SetAlarm
  mov mvar(abTime[1]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
SetAlarm:
  mov mvar(abAlarm[1]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
endp

proc(WriteTOD_MIN)
  push ESI
  mov ESI,ECX
  mov EAX,[ESP+8]

  test mvar(bCRB),10000000b
  jne SetAlarm
  mov mvar(abTime[2]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
SetAlarm:
  mov mvar(abAlarm[2]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
endp

proc(WriteTOD_HR)
  push ESI
  mov ESI,ECX
  mov EAX,[ESP+8]

  test mvar(bCRB),10000000b
  jne SetAlarm
  mov mvar(bStopFlag),1
  mov mvar(abTime[3]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
SetAlarm:
  mov mvar(abAlarm[3]),AL
  call fn(CompAlarm)

  pop ESI
  ret 4
endp

const byte abIncTab[] = {
  0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10,0x0B,0x0C,0x0D,0x0E,0x0F,0x00,
  0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,0x20,0x1B,0x1C,0x1D,0x1E,0x1F,0x10,
  0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,0x30,0x2B,0x2C,0x2D,0x2E,0x2F,0x20,
  0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x40,0x3B,0x3C,0x3D,0x3E,0x3F,0x30,
  0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,0x49,0x50,0x4B,0x4C,0x4D,0x4E,0x4F,0x40,
  0x51,0x52,0x53,0x54,0x55,0x56,0x57,0x58,0x59,0x00,0x5B,0x5C,0x5D,0x5E,0x5F,0x50,
  0x61,0x62,0x63,0x64,0x65,0x66,0x67,0x68,0x69,0x70,0x6B,0x6C,0x6D,0x6E,0x6F,0x60,
  0x71,0x72,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x00,0x7B,0x7C,0x7D,0x7E,0x7F,0x70
};

const byte abHour[] = {
  0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10,0x0B,0x0C,0x0D,0x0E,0x0F,0x00,
  0x11,0x92,0x01,0x14,0x15,0x16,0x17,0x18,0x19,0x1A,0x0B,0x0C,0x0D,0x0E,0x0F,0x00
};

proc(OnTODHigh)
  push EBX
  push ThisReg
  mov ThisReg,ECX

  cmp mvar(bStopFlag),0
  jne Return

  dec mvar(bTODCounter)
  jne Return

  mov AL,mvar(bCRA)
  shr AL,7
  add AL,5
  mov mvar(bTODCounter),AL
  mov EAX,dword ptr mvar(abTime)
  cmp AL,09h
  je IncSec
  inc AL
  and AL,00001111b
  jmp SetTime

Return:
  pop ThisReg
  pop EBX
  ret

IncSec:
  xor AL,AL
  xor EBX,EBX
  mov BL,AH
  mov AH,abIncTab[EBX]
  and AH,AH
  jne SetTime
  ror EAX,16
  mov BL,AL
  mov AL,abIncTab[EBX]
  and AL,AL
  jne RollAndSetTime
  mov BL,AH
  and BL,00011111b
  and AH,10000000b
  xor AH,abHour[EBX]
RollAndSetTime:
  rol EAX,16
SetTime:
  mov dword ptr mvar(abTime),EAX
  call fn(CompAlarm)

  pop ThisReg
  pop EBX
  ret
endp

  CIA Serial Data Register SDR

  CLine SP;             // bidirectional serial line
  CLine CNT;            // bidirectional serial clock

proc(ResetSDR)
  //CNT.pfnOnHigh = NULL;
  //CNT.pfnOnLow = NULL;
  ret
endp

proc(ReadSDR)
  push ESI
  mov ESI,ECX


  pop ESI
  ret
endp

proc(WriteSDR)
  push ESI
  mov ESI,ECX
  mov EAX,[ESP+8]


  pop ESI
  ret 4
endp
*/
