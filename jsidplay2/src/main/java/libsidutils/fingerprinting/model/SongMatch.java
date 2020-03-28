package libsidutils.fingerprinting.model;

public class SongMatch extends Match {

    private int idSong;

    public SongMatch(int idSong, int count, int time) {
    	super(count, time);
        this.idSong = idSong;
    }

    public int getIdSong() {
        return idSong;
    }

}