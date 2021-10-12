package x.mvmn.sonivm.util;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.tagtraum.ffsampledsp.FFAudioFileReader;

public class AudioFileUtil {

	public static Long getAudioFileDurationInMilliseconds(File audioFile) throws IOException {
		Long result = null;
		try {
			AudioFileFormat format = new FFAudioFileReader().getAudioFileFormat(audioFile);
			if (format.properties() != null && format.properties().get("duration") != null) {
				Object duration = format.properties().get("duration");
				if (duration instanceof Number) {
					result = ((Number) duration).longValue() / 1000;
				} else {
					result = Long.parseLong(duration.toString()) / 1000;
				}
			}
		} catch (UnsupportedAudioFileException e) {
			result = null;
		}
		return result;
	}
}
