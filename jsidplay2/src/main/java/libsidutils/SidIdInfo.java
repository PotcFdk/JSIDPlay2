package libsidutils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SidIdInfo extends SidIdBase {

	private static final String COMMENT_TOKEN = "  COMMENT: ";

	private static final String REFERENCE_TOKEN = "REFERENCE: ";

	private static final String RELEASED_TOKEN = " RELEASED: ";

	private static final String AUTHOR_TOKEN = "   AUTHOR: ";

	private static final String NAME_TOKEN = "     NAME: ";

	/**
	 * Package of the internal configuration file.
	 */
	private static final String SID_ID_PKG = "libsidutils/";

	/**
	 * Name of the configuration file.
	 */
	private static final String FNAME = "sidid.nfo";

	public static class PlayerInfoSection {

		/**
		 * Player name.
		 */
		private final String playerName;

		private String name;
		private String author;
		private String released;
		private String reference;
		private String comment;

		/**
		 * Create a new section.
		 * 
		 * @param name
		 *            player name
		 */
		public PlayerInfoSection(final String name) {
			this.playerName = name;
		}

		public final String getName() {
			return name;
		}

		public final void setName(String name) {
			this.name = name;
		}

		public final String getAuthor() {
			return author;
		}

		public final void setAuthor(String author) {
			this.author = author;
		}

		public final String getReleased() {
			return released;
		}

		public final void setReleased(String released) {
			this.released = released;
		}

		public final String getReference() {
			return reference;
		}

		public final void setReference(String reference) {
			this.reference = reference;
		}

		public final String getComment() {
			return comment;
		}

		public final void setComment(String comment) {
			this.comment = comment;
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(playerName).append('\n');
			if (name != null)
				stringBuilder.append(NAME_TOKEN + name).append('\n');
			if (author != null)
				stringBuilder.append(AUTHOR_TOKEN + author).append('\n');
			if (released != null)
				stringBuilder.append(RELEASED_TOKEN + released).append('\n');
			if (comment != null)
				stringBuilder.append(COMMENT_TOKEN + comment).append('\n');
			if (reference != null)
				stringBuilder.append(REFERENCE_TOKEN + reference).append('\n');
			stringBuilder.append('\n');
			return stringBuilder.toString();
		}
	}

	private ArrayList<PlayerInfoSection> playerInfoList;

	/**
	 * Read configuration file and configure the SID-ID class.
	 * 
	 * @return
	 * @throws IOException
	 */
	public void readconfig() throws IOException {
		this.playerInfoList = new ArrayList<PlayerInfoSection>();
		PlayerInfoSection playerInfoSection = null;
		String line;
		try (final BufferedReader br = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(
						readConfiguration(FNAME, SID_ID_PKG))))) {
			while ((line = br.readLine()) != null) {
				final StringTokenizer stok = new StringTokenizer(line, "\n");
				while (stok.hasMoreTokens()) {
					final String token = stok.nextToken();
					if (token.length() > 0) {
						if (token.length() == 0) {
							playerInfoSection = null;
						} else if (token.startsWith(NAME_TOKEN)) {
							playerInfoSection.setName(token
									.substring(NAME_TOKEN.length()));
						} else if (token.startsWith(AUTHOR_TOKEN)) {
							playerInfoSection.setAuthor(token
									.substring(AUTHOR_TOKEN.length()));
						} else if (token.startsWith(RELEASED_TOKEN)) {
							playerInfoSection.setReleased(token
									.substring(RELEASED_TOKEN.length()));
						} else if (token.startsWith(COMMENT_TOKEN)) {
							playerInfoSection.setComment(token.substring(
									COMMENT_TOKEN.length()).trim());
						} else if (token.startsWith(REFERENCE_TOKEN)) {
							playerInfoSection.setReference(token
									.substring(REFERENCE_TOKEN.length()));
						} else if (token.startsWith(" ")) {
							// comment being continued...
							playerInfoSection.setComment(playerInfoSection
									.getComment() + "\n" + token);
						} else {
							// player name
							playerInfoSection = new PlayerInfoSection(token);
							playerInfoList.add(playerInfoSection);
						}
					}
				}
			}
		}
	}

	/**
	 * Search player ID Info.
	 * 
	 * @param playerName
	 *            player to get infos for
	 * @return player infos (or null, if not found)
	 */
	public PlayerInfoSection getPlayerInfo(final String playerName) {
		for (final PlayerInfoSection playerInfo : playerInfoList) {
			if (playerInfo.playerName.equals(playerName)) {
				return playerInfo;
			}
		}
		return null;
	}

	public void writeconfig(List<PlayerInfoSection> playerInfoList) {
		for (PlayerInfoSection playerInfoSection2 : playerInfoList) {
			System.out.println(playerInfoSection2.toString());
		}
	}

	public static void main(String[] args) throws IOException {
		SidIdInfo sidIdInfo = new SidIdInfo();
		sidIdInfo.readconfig();
		sidIdInfo.writeconfig(sidIdInfo.playerInfoList);
	}
}
