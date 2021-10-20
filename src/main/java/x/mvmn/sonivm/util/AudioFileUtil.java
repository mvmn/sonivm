package x.mvmn.sonivm.util;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.tagtraum.ffsampledsp.FFAudioFileReader;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AudioFileUtil {

	public static Integer getAudioFileDurationInMilliseconds(File audioFile) throws IOException {
		Integer result = null;
		try {
			AudioFileFormat format = new FFAudioFileReader().getAudioFileFormat(audioFile);
			if (format.properties() != null && format.properties().get("duration") != null) {
				Object duration = format.properties().get("duration");
				if (duration instanceof Number) {
					result = (int) (((Number) duration).longValue() / 1000);
				} else {
					result = (int) (Long.parseLong(duration.toString()) / 1000);
				}
			}
		} catch (UnsupportedAudioFileException e) {
			result = null;
		}
		return result;
	}
}
