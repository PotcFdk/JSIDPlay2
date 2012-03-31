////////////////////////////////////////////////////////////////////////////////
// CIA6526.h -- this file is part of the Emulator Developers Kit
// available at http://ourworld.compuserve.com/homepages/pc64/develop.htm

class CIA6526 : public Chip {
  void OnReset();


////////////////////////////////////////////////////////////////////////////////
// ports

  byte bPALatch;        // buffered output values
  byte bPBLatch;
  byte bDDRA;           // directions: 0 = input, 1 = output
  byte bDDRB;
 
  Timer PCNextClock;    // draws PC high again at the next clock
  void OnPCNextClock(); // TODO: use 2 bits in dwDelay

  void InitPorts();
  void ResetPorts();

  byte ReadPRA();
  void WritePRA(byte bValue);
  byte ReadPRB();
  void WritePRB(byte bValue);
  byte ReadDDRA();
  void WriteDDRA(byte bValue);
  byte ReadDDRB();
  void WriteDDRB(byte bValue);


////////////////////////////////////////////////////////////////////////////////
// timers

  int iCounterA;        // counter
  int iLatchA;          // start value
  int iCounterB;
  int iLatchB;
  Timer TimerIdle;      // called one clock before a timer fires
  void OnTimerIdleFire();

  void InitTimers();
  void ResetTimers();

  byte ReadTALow();
  void WriteTALow(byte bValue);
  byte ReadTAHigh();
  void WriteTAHigh(byte bValue);
  byte ReadTBLow();
  void WriteTBLow(byte bValue);
  byte ReadTBHigh();
  void WriteTBHigh(byte bValue);


////////////////////////////////////////////////////////////////////////////////
// TOD time of day

  byte abTime[4];       // current time
  byte abBufTime[4];    // frozen time after reading hour
  byte abAlarm[4];      // alarm time
  byte bTODCounter;     // counter 1/50th or 1/60th sec
  byte bStopFlag;       // clock is stopped after writing to HR
  byte bBufFlag;        // read from abBufTime instead of abTime
  byte abTODFill[1];

  void InitTOD();
  void ResetTOD();

  void OnTODHigh();

  byte ReadTOD10ths();
  void WriteTOD10ths(byte bValue);
  byte ReadTODSec();
  void WriteTODSec(byte bValue);
  byte ReadTODMin();
  void WriteTODMin(byte bValue);
  byte ReadTODHr();
  void WriteTODHr(byte bValue);

////////////////////////////////////////////////////////////////////////////////
// SDR serial data register

  void OnCNTHigh();

  void InitSDR();
  void ResetSDR();

  byte ReadSDR();
  void WriteSDR(byte bValue);

////////////////////////////////////////////////////////////////////////////////
// control and interrupt registers

  dword dwDelay;        // performs delay by shifting left at each clock
  dword dwFeed;         // new bits to feed into dwDelay
  byte bCRA;            // control register A
  byte bCRB;            // control register B
  byte bICR;            // interrupt control register
  byte bIMR;            // interrupt mask register
  byte bPB67TimerMode;  // bit mask for PB outputs: 0 = port register, 1 = timer
  byte bPB67TimerOut;   // PB outputs bits 6 and 7 in timer mode
  byte bPB67Toggle;     // PB outputs bits 6 and 7 in toggle mode
  byte abControlFill[1];

  void NewDelayOrFeed();


  void OnFlagLow();     // set ICR4 on falling edge

  void OnClock();
  void OnClockC();

  void InitControl();
  void ResetControl();

  byte ReadCRA();
  void WriteCRA(byte bValue);
  byte ReadCRB();
  void WriteCRB(byte bValue);
  byte ReadICR();
  void WriteIMR(byte bValue);

  // initialisation
  virtual void DoInit();

public:

  // constructor
  global CIA6526() {
    static const Register aRegs[16] = {
      (bpfn)ReadPRA, (pfnb)WritePRA, 0,
      (bpfn)ReadPRB, (pfnb)WritePRB, DummyReads,
      (bpfn)ReadDDRA, (pfnb)WriteDDRA, 0,
      (bpfn)ReadDDRB, (pfnb)WriteDDRB, 0,
      (bpfn)ReadTALow, (pfnb)WriteTALow, 0,
      (bpfn)ReadTAHigh, (pfnb)WriteTAHigh, 0,
      (bpfn)ReadTBLow, (pfnb)WriteTBLow, 0,
      (bpfn)ReadTBHigh, (pfnb)WriteTBHigh, 0,
      (bpfn)ReadTOD10ths, (pfnb)WriteTOD10ths, DummyReads,
      (bpfn)ReadTODSec, (pfnb)WriteTODSec, 0,
      (bpfn)ReadTODMin, (pfnb)WriteTODMin, 0,
      (bpfn)ReadTODHr, (pfnb)WriteTODHr, DummyReads,
      (bpfn)ReadSDR, (pfnb)WriteSDR, DummyReads,
      (bpfn)ReadICR, (pfnb)WriteIMR, DummyReads,
      (bpfn)ReadCRA, (pfnb)WriteCRA, 0,
      (bpfn)ReadCRB, (pfnb)WriteCRB, 0
    };
    pRegisters = aRegs;
    iRegisterCount = 16;
  }

  // interfaces
  Port PA;
  Port PB;
  Line PC;    // output: low for one clock after reading/writing PB
  Line TOD;   // input: PAL 50 Hz, NTSC 60 Hz
  Line Flag;  // input: may generate int on falling edge
  Line SP;    // serial data port
  Line CNT;   // serial clock or input timer clock or timer gate
  Line Int;
  Line Reset;
};
