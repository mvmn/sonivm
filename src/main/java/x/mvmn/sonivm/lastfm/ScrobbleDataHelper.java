package x.mvmn.sonivm.lastfm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.umass.lastfm.scrobble.ScrobbleData;

public class ScrobbleDataHelper {

	public static byte[] serialize(ScrobbleData track, int endOfEntryMarker) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			append(baos, String.valueOf(track.getTrackNumber()));
			append(baos, track.getTrack());
			append(baos, track.getArtist());
			append(baos, track.getAlbum());
			append(baos, track.getAlbumArtist());
			append(baos, String.valueOf(track.getDuration()));
			append(baos, String.valueOf(track.getTimestamp()));
			append(baos, track.getMusicBrainzId());
			append(baos, track.getStreamId());
			baos.write(endOfEntryMarker);

			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to serialize TrackInfo", ioe);
		}
	}

	public static ScrobbleData deserialize(byte[] data) {
		String[] fieldValues = new String[9];

		byte[] buffer = new byte[255];
		int fieldStartIdx = 0;
		int fieldNumber = 0;
		for (int i = 0; i < data.length; i++) {
			int nextIdx = i - fieldStartIdx + 1;
			byte b = data[i];
			if (b == 255) {
				break;
			}
			if (b == 0) {
				fieldStartIdx = i + 1;
				fieldValues[fieldNumber++] = nextIdx > 1 ? new String(buffer, 0, nextIdx - 1, StandardCharsets.UTF_8) : null;
			} else {
				// If buffer overflow - extend buffer
				if (nextIdx >= buffer.length) {
					buffer = Arrays.copyOf(buffer, buffer.length + 255);
				}
				buffer[nextIdx - 1] = b;
			}
		}

		ScrobbleData scrobbleData = new ScrobbleData();
		scrobbleData.setTrackNumber(Integer.parseInt(fieldValues[0]));
		scrobbleData.setTrack(fieldValues[1]);
		scrobbleData.setArtist(fieldValues[2]);
		scrobbleData.setAlbum(fieldValues[3]);
		scrobbleData.setAlbumArtist(fieldValues[4]);
		scrobbleData.setDuration(Integer.parseInt(fieldValues[5]));
		scrobbleData.setTimestamp(Integer.parseInt(fieldValues[6]));
		scrobbleData.setMusicBrainzId(fieldValues[7]);
		scrobbleData.setStreamId(fieldValues[8]);

		return scrobbleData;
	}

	private static void append(OutputStream baos, String value) throws IOException {
		if (value != null) {
			baos.write(value.getBytes(StandardCharsets.UTF_8));
		}
		baos.write(0);
	}
}
