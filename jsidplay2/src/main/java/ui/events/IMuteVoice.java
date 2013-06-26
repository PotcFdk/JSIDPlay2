package ui.events;

/**
 * Mute a voice of a SID.
 * 
 * @author Ken
 * 
 */
public interface IMuteVoice extends IEvent {

	/**
	 * Get SID to be muted.
	 * 
	 * @return SID to be muted
	 */
	int getSidNum();

	/**
	 * Get voice of the SID to be muted.
	 * 
	 * @return voice of the SID to be muted
	 */
	int getVoice();

	/**
	 * Mute?
	 * 
	 * @return true (mute), false else
	 */
	boolean isMute();

}
