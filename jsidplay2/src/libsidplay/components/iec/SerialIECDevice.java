/*
 * serial-iec-device.c
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
 *  David Hansel <david@hansels.net>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */
package libsidplay.components.iec;

import java.util.Arrays;

import libsidplay.common.ISID2Types.CPUClock;

public abstract class SerialIECDevice {

	protected static final int IECBUS_DEVICE_READ_DATA = 0x01;
	protected static final int IECBUS_DEVICE_READ_CLK = 0x04;
	protected static final int IECBUS_DEVICE_READ_ATN = 0x80;
	protected static final int IECBUS_DEVICE_WRITE_CLK = 0x40;
	protected static final int IECBUS_DEVICE_WRITE_DATA = 0x80;

	protected static final int P_PRE0 = 0;
	protected static final int P_PRE1 = 1;
	protected static final int P_PRE2 = 2;
	protected static final int P_READY = 3;
	protected static final int P_EOI = 4, P_EOIw = 5;
	protected static final int P_BIT0 = 6, P_BIT0w = 7;
	protected static final int P_BIT1 = 8, P_BIT1w = 9;
	protected static final int P_BIT2 = 10, P_BIT2w = 11;
	protected static final int P_BIT3 = 12, P_BIT3w = 13;
	protected static final int P_BIT4 = 14, P_BIT4w = 15;
	protected static final int P_BIT5 = 16, P_BIT5w = 17;
	protected static final int P_BIT6 = 18, P_BIT6w = 19;
	protected static final int P_BIT7 = 20, P_BIT7w = 21;
	protected static final int P_DONE0 = 22, P_DONE1 = 23;
	protected static final int P_FRAMEERR0 = 24, P_FRAMEERR1 = 25;

	protected static final int P_TALKING = 0x20;
	protected static final int P_LISTENING = 0x40;
	protected static final int P_ATN = 0x80;

	protected boolean enabled;
	protected byte byt, state, flags, primary, secondary, secondaryPrev;
	protected byte st[] = new byte[16];
	protected long timeout;

	private byte serialIECDeviceSt;
	private double serialIECDeviceCyclesPerUs = 1.0;
	private IECBus iecBus;

	/**
	 * Device number.
	 */
	protected int prnr = 0;

	public SerialIECDevice(final IECBus bus) {
		iecBus = bus;
		enabled = false;
		iecBus.deviceWrite(prnr,
				(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
	}

	public void reset() {
		if (enabled) {
			iecBus.deviceWrite(
					prnr,
					(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
			flags = 0;
			timeout = 0;
			Arrays.fill(st, 0, 15, (byte) 0);
		}
	}

	/*------------------------------------------------------------------------*/
	/* Implement IEC devices here. */
	/*------------------------------------------------------------------------*/

	protected void setDeviceEnable(boolean enable) {
		if (enable) {
			if (!enabled) {
				enabled = true;
				flags = 0;
				timeout = 0;
				Arrays.fill(st, 0, 15, (byte) 0);
			}
		} else {
			if (enabled) {
				iecBus.deviceWrite(prnr,
						(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
				enabled = false;
				timeout = 0;
			}
		}
	}

	/* ------------------------------------------------------------------------- */

	public int getID() {
		return prnr;
	}

	public void setClock(CPUClock cpuFreq) {
		serialIECDeviceCyclesPerUs = cpuFreq.getCpuFrequency() / 1000000.0;
	}

	public void clock() {
		if (enabled) {
			byte bus;

			/* read bus */
			bus = iecBus.deviceRead();

			if (0 == (flags & P_ATN) && 0 == (bus & IECBUS_DEVICE_READ_ATN)) {
				/* falling flank on ATN (bus master addressing all devices) */
				state = P_PRE0;
				flags |= P_ATN;
				primary = 0;
				secondaryPrev = secondary;
				secondary = 0;
				timeout = clk() + usToCycles(100);

				/*
				 * set DATA=0 ("I am here"). If nobody on the bus does this within
				 * 1ms, busmaster will assume that "Device not present"
				 */
				iecBus.deviceWrite(prnr, (byte) (IECBUS_DEVICE_WRITE_CLK));
			} else if ((flags & P_ATN) != 0
					&& (bus & IECBUS_DEVICE_READ_ATN) != 0) {
				/* rising flank on ATN (bus master finished addressing all devices) */
				flags &= ~P_ATN;

				if ((primary == 0x20 + prnr) || (primary == 0x40 + prnr)) {
					if ((secondary & 0xf0) == 0x60) {
						switch (primary & 0xf0) {
						case 0x20:
							listenTalk(prnr, secondary);
							serialIECDeviceSt = getStatus();
							break;
						case 0x40:
							listenTalk(prnr, secondary);
							serialIECDeviceSt = getStatus();
							break;
						}
					} else if ((secondary & 0xf0) == 0xe0) {
						serialIECDeviceSt = ((byte) 0);
						close(prnr, secondary);
						serialIECDeviceSt = getStatus();
						st[secondary & 0x0f] = serialIECDeviceSt;
					} else if ((secondary & 0xf0) == 0xf0) {
						/*
						 * iec_bus_open() will not actually open the file (since we
						 * don't have a filename yet) but just set things up so that
						 * the characters passed to iec_bus_write() before the next
						 * call to iec_bus_unlisten()will be interpreted as the
						 * filename. The file will actually be opened during the
						 * next call to iec_bus_unlisten()
						 */
						serialIECDeviceSt = ((byte) 0);
						open(prnr, secondary);
						serialIECDeviceSt = getStatus();
						st[secondary & 0x0f] = serialIECDeviceSt;
					}

					if (primary == 0x20 + prnr) {
						/* we were told to listen */
						flags &= ~P_TALKING;

						/*
						 * st!=0 means that the previous OPEN command failed, i.e.
						 * we could not open a file for writing. In that case,
						 * ignore the "LISTEN" request which will signal the error
						 * to the sender
						 */

						if (st[secondary & 0x0f] == 0) {
							flags |= P_LISTENING;
							state = P_PRE1;
						}

						/* set DATA=0 ("I am here") */
						iecBus.deviceWrite(prnr, (byte) IECBUS_DEVICE_WRITE_CLK);
					} else if (primary == 0x40 + prnr) {
						/* we were told to talk */
						flags &= ~P_LISTENING;
						flags |= P_TALKING;
						state = P_PRE0;
					}
				} else if ((primary == 0x3f) && (flags & P_LISTENING) != 0) {
					/* all devices were told to stop listening */
					flags &= ~P_LISTENING;

					/*
					 * if this is an UNLISTEN that followed an OPEN (0x2_ 0xf_),
					 * then iec_bus_unlisten will try to open the file with the
					 * filename that was received in between the OPEN and now. If
					 * the file cannot be opened, it will set st != 0.
					 */
					serialIECDeviceSt = st[secondaryPrev & 0x0f];
					unlisten(prnr, secondaryPrev);
					serialIECDeviceSt = getStatus();
					st[secondaryPrev & 0x0f] = serialIECDeviceSt;
				} else if (primary == 0x5f && (flags & P_TALKING) != 0) {
					/* all devices were told to stop talking */
					untalk(prnr, secondaryPrev);
					serialIECDeviceSt = getStatus();
					flags &= ~P_TALKING;
				}

				if (0 == (flags & (P_LISTENING | P_TALKING))) {
					/*
					 * we're neither listening nor talking => make sure we're not
					 * holding DATA or CLOCK line to 0
					 */
					iecBus.deviceWrite(
							prnr,
							(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
				}
			}

			if ((flags & (P_ATN | P_LISTENING)) != 0) {
				/* we are either under ATN or in "listening" mode */

				switch (state) {
				case P_PRE0:
					/*
					 * ignore anything that happens during first 100us after falling
					 * flank on ATN (other devices may have been sending and need
					 * some time to set CLK=1)
					 */
					if (clk() >= timeout)
						state = P_PRE1;
					break;
				case P_PRE1:
					/*
					 * make sure CLK=0 so we actually detect a rising flank in state
					 * P_PRE2
					 */
					if (0 == (bus & IECBUS_DEVICE_READ_CLK))
						state = P_PRE2;
					break;
				case P_PRE2:
					/* wait for rising flank on CLK ("ready-to-send") */
					if ((bus & IECBUS_DEVICE_READ_CLK) != 0) {
						/* react by setting DATA=1 ("ready-for-data") */
						iecBus.deviceWrite(
								prnr,
								(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
						timeout = clk() + usToCycles(200);
						state = P_READY;
					}
					break;
				case P_READY:
					if (0 == (bus & IECBUS_DEVICE_READ_CLK)) {
						/* sender set CLK=0, is about to send first bit */
						state = P_BIT0;
					} else if (0 == (flags & P_ATN)
							&& (clk() >= timeout)) {
						/*
						 * sender did not set CLK=0 within 200us after we set DATA=1
						 * => it is signaling EOI (not so if we are under ATN)
						 * acknowledge we received it by setting DATA=0 for 60us
						 */
						iecBus.deviceWrite(prnr, (byte) IECBUS_DEVICE_WRITE_CLK);
						state = P_EOI;
						timeout = clk() + usToCycles(60);
					}
					break;
				case P_EOI:
					if (clk() >= timeout) {
						/* Set DATA back to 1 and wait for sender to set CLK=0 */
						iecBus.deviceWrite(
								prnr,
								(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
						state = P_EOIw;
					}
					break;
				case P_EOIw:
					if (0 == (bus & IECBUS_DEVICE_READ_CLK)) {
						/* sender set CLK=0, is about to send first bit */
						state = P_BIT0;
					}
					break;
				case P_BIT0:
				case P_BIT1:
				case P_BIT2:
				case P_BIT3:
				case P_BIT4:
				case P_BIT5:
				case P_BIT6:
				case P_BIT7:
					if ((bus & IECBUS_DEVICE_READ_CLK) != 0) {
						/*
						 * sender set CLK=1, signaling that the DATA line represents
						 * a valid bit
						 */
						byte bit = (byte) (1 << ((byte) (state - P_BIT0) / 2));
						byt = (byte) ((byt & ~bit) | ((bus & IECBUS_DEVICE_READ_DATA) != 0 ? bit
								: 0));

						/*
						 * go to associated P_BIT(n)w state, waiting for sender to
						 * set CLK=0
						 */
						state++;
					}
					break;
				case P_BIT0w:
				case P_BIT1w:
				case P_BIT2w:
				case P_BIT3w:
				case P_BIT4w:
				case P_BIT5w:
				case P_BIT6w:
					if (0 == (bus & IECBUS_DEVICE_READ_CLK)) {
						/*
						 * sender set CLK=0. go to P_BIT(n+1) state to receive next
						 * bit
						 */
						state++;
					}
					break;
				case P_BIT7w:
					if (0 == (bus & IECBUS_DEVICE_READ_CLK)) {
						/* sender set CLK=0 and this was the last bit */
						if ((flags & P_ATN) != 0) {
							/*
							 * We are currently receiving under ATN. Store first two
							 * bytes received (contain primary and secondary
							 * address)
							 */
							if (primary == 0)
								primary = byt;
							else if (secondary == 0)
								secondary = byt;

							if (0 == (primary & 0x10)
									&& (primary & 0x0f) != prnr) {
								/*
								 * This is NOT a UNLISTEN (0x3f) or UNTALK (0x5f)
								 * command and the primary address is not ours =>
								 * Don't acknowledge the frame and stop listening.
								 * If all devices on the bus do this, the busmaster
								 * knows that "Device not present"
								 */
								state = P_DONE0;
							} else {
								/* Acknowledge frame by setting DATA=0 */
								iecBus.deviceWrite(prnr,
										(byte) IECBUS_DEVICE_WRITE_CLK);

								/*
								 * repeat from P_PRE2 (we know that CLK=0 so no need
								 * to go to P_PRE1)
								 */
								state = P_PRE2;
							}
						} else if ((flags & P_LISTENING) != 0) {
							/*
							 * We are currently listening for data => pass received
							 * byte on to the upper level
							 */
							serialIECDeviceSt = st[secondary & 0x0f];
							write(prnr, secondary, byt);
							serialIECDeviceSt = getStatus();
							st[secondary & 0x0f] = serialIECDeviceSt;

							if (st[secondary & 0x0f] != 0) {
								/*
								 * there was an error during iec_bus_write => stop
								 * listening. This will signal an error condition to
								 * the sender
								 */
								state = P_DONE0;
							} else {
								/* Acknowledge frame by setting DATA=0 */
								iecBus.deviceWrite(prnr,
										(byte) IECBUS_DEVICE_WRITE_CLK);

								/*
								 * repeat from P_PRE2 (we know that CLK=0 so no need
								 * to go to P_PRE1)
								 */
								state = P_PRE2;
							}
						}
					}
					break;
				case P_DONE0:
					/* we're just waiting for the busmaster to set ATN back to 1 */
					break;
				}
			} else if ((flags & P_TALKING) != 0) {
				/* we are in "talking" mode */
				switch (state) {
				case P_PRE0:
					if ((bus & IECBUS_DEVICE_READ_CLK) != 0) {
						/*
						 * busmaster set CLK=1 (and before that should have set
						 * DATA=0) we are getting ready for role reversal. Set
						 * CLK=0, DATA=1
						 */
						iecBus.deviceWrite(prnr, (byte) IECBUS_DEVICE_WRITE_DATA);
						state = P_PRE1;
						timeout = clk() + usToCycles(80);
					}
					break;
				case P_PRE1:
					if (clk() >= timeout) {
						/* signal "ready-to-send" (CLK=1) */
						iecBus.deviceWrite(
								prnr,
								(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
						state = P_READY;
						break;
					}
					// $FALL-THROUGH$
				case P_READY:
					if ((bus & IECBUS_DEVICE_READ_DATA) != 0) {
						/* receiver signaled "ready-for-data" (DATA=1) */

						serialIECDeviceSt = st[secondary & 0x0f];
						byte data = read(prnr, secondary);
						serialIECDeviceSt = getStatus();
						byt = data;
						st[secondary & 0x0f] = serialIECDeviceSt;

						if (st[secondary & 0x0f] == 0) {
							/*
							 * at least two bytes left to send. Go on to send first
							 * bit.
							 */
							state = P_BIT0;

							/* no need to wait before sending the first bit */
							timeout = clk();
						} else if (st[secondary & 0x0f] == 0x40) {
							/*
							 * only this byte left to send => signal EOI by keeping
							 * CLK=1
							 */
							state = P_EOI;
						} else {
							/*
							 * There was some kind of error, we have nothing to
							 * send. Just stop talking and wait for ATN. (This will
							 * produce a "File not found" when loading)
							 */
							flags &= ~P_TALKING;
						}
					}
					break;
				case P_EOI:
					if (0 == (bus & IECBUS_DEVICE_READ_DATA)) {
						/*
						 * receiver set DATA=0, first part of acknowledging the EOI
						 */
						state = P_EOIw;
					}
					break;
				case P_EOIw:
					if ((bus & IECBUS_DEVICE_READ_DATA) != 0) {
						/*
						 * receiver set DATA=1, final part of acknowledging the EOI.
						 * Go on to send first bit
						 */
						state = P_BIT0;

						/* no need to wait before sending the first bit */
						timeout = clk();
					}
					break;
				case P_BIT0:
				case P_BIT1:
				case P_BIT2:
				case P_BIT3:
				case P_BIT4:
				case P_BIT5:
				case P_BIT6:
				case P_BIT7:
					if (clk() >= timeout) {
						/*
						 * 60us have passed since we set CLK=1 to signal "data
						 * valid" for the previous bit. Pull CLK=0 and put next bit
						 * out on DATA.
						 */
						int bit = 1 << ((state - P_BIT0) / 2);
						iecBus.deviceWrite(
								prnr,
								(byte) ((byt & bit) != 0 ? IECBUS_DEVICE_WRITE_DATA
										: 0));

						/* go to associated P_BIT(n)w state */
						timeout = clk() + usToCycles(60);
						state++;
					}
					break;
				case P_BIT0w:
				case P_BIT1w:
				case P_BIT2w:
				case P_BIT3w:
				case P_BIT4w:
				case P_BIT5w:
				case P_BIT6w:
				case P_BIT7w:
					if (clk() >= timeout) {
						/*
						 * 60us have passed since we pulled CLK=0 and put the
						 * current bit on DATA. set CLK=1, keeping data as it is
						 * (this signals "data valid" to the receiver)
						 */
						if ((bus & IECBUS_DEVICE_READ_DATA) != 0)
							iecBus.deviceWrite(
									prnr,
									(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
						else
							iecBus.deviceWrite(prnr,
									(byte) (IECBUS_DEVICE_WRITE_CLK));

						/*
						 * go to associated P_BIT(n+1) state to send the next bit.
						 * If this was the final bit then next state is P_DONE0
						 */
						timeout = clk() + usToCycles(60);
						state++;
					}
					break;
				case P_DONE0:
					if (clk() >= timeout) {
						/*
						 * 60us have passed since we set CLK=1 to signal "data
						 * valid" for the final bit. Pull CLK=0 and set DATA=1. This
						 * prepares for the receiver acknowledgement.
						 */
						iecBus.deviceWrite(prnr, (byte) IECBUS_DEVICE_WRITE_DATA);
						timeout = clk() + usToCycles(1000);
						state = P_DONE1;
					}
					break;
				case P_DONE1:
					if (0 == (bus & IECBUS_DEVICE_READ_DATA)) {
						/* Receiver set DATA=0, acknowledging the frame */
						if (st[secondary & 0x0f] == 0x40) {
							/*
							 * This was the last byte => stop talking. This leaves
							 * us waiting for ATN.
							 */
							flags &= ~P_TALKING;
							st[secondary & 0x0f] = 0;

							/* Release the CLOCK line to 1 */
							iecBus.deviceWrite(
									prnr,
									(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
						} else {
							/*
							 * There is at least one more byte to send Start over
							 * from P_PRE1
							 */
							timeout = clk();
							state = P_PRE1;
						}
					} else if (clk() >= timeout) {
						/*
						 * We didn't receive an acknowledgement within 1ms. Set
						 * CLOCK=0 and after 100us back to CLOCK=1
						 */
						iecBus.deviceWrite(
								prnr,
								(byte) (IECBUS_DEVICE_WRITE_CLK | IECBUS_DEVICE_WRITE_DATA));
						timeout = clk() + usToCycles(100);
						state = P_FRAMEERR0;
					}
					break;
				case P_FRAMEERR0:
					if (clk() >= timeout) {
						/*
						 * finished 1-0-1 sequence of CLOCK signal to acknowledge
						 * the frame-error. Now wait for sender to set DATA=0 so we
						 * can continue.
						 */
						iecBus.deviceWrite(prnr, (byte) IECBUS_DEVICE_WRITE_DATA);
						state = P_FRAMEERR1;
					}
					break;
				case P_FRAMEERR1:
					if (0 == (bus & IECBUS_DEVICE_READ_DATA)) {
						/* sender set DATA=0, we can retry to send the byte */
						timeout = clk();
						state = P_PRE1;
					}
					break;
				}
			}
		}
	}

	/**
	 * Convert microseconds to cycles
	 * 
	 * @param us
	 *            number of microseconds
	 * @return cycle count
	 */
	private long usToCycles(double us) {
		return (long) ((us * serialIECDeviceCyclesPerUs) + 0.5);
	}

	public abstract void open(int device, byte secondary);
	public abstract void close(int device, byte secondary);
	public abstract void listenTalk(int device, byte secondary);
	public abstract void unlisten(int device, byte secondary);
	public abstract void untalk(int device, byte secondary);
	public abstract byte read(int device, byte secondary);
	public abstract void write(int device, byte secondary, byte data);
	public abstract byte getStatus();
	public abstract long clk();

}
