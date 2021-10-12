package x.mvmn.sonivm.tag.impl;

import java.io.File;
import java.util.regex.Pattern;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.stereotype.Service;

import x.mvmn.sonivm.tag.TagRetrievalService;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry.TrackMetadata;
import x.mvmn.sonivm.util.StringUtil;

@Service
public class TagRetrievalServiceImpl implements TagRetrievalService {
	private static final Pattern PTRN_DATE_STARTS_WITH_YEAR = Pattern.compile("^\\d{4}\\-.*");

	@Override
	public TrackMetadata getAudioFileMetadata(File file) {
		try {
			AudioFile audioFile = AudioFileIO.read(file);
			Tag tag = audioFile.getTag();
			if (tag != null) {
				int trackLength = audioFile.getAudioHeader().getTrackLength();
				return TrackMetadata.builder()
						.trackNumber(StringUtil.nullForBlank(tag.getFirst(FieldKey.TRACK)))
						.artist(StringUtil.nullForBlank(tag.getFirst(FieldKey.ARTIST)))
						.album(StringUtil.nullForBlank(tag.getFirst(FieldKey.ALBUM)))
						.title(StringUtil.nullForBlank(tag.getFirst(FieldKey.TITLE)))
						.date(StringUtil.nullForBlank(convertDate(tag.getFirst(FieldKey.YEAR))))
						.genre(StringUtil.nullForBlank(tag.getFirst(FieldKey.GENRE)))
						.duration(trackLength > 0 ? trackLength : null)
						.build();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read tags for a file " + file.getAbsolutePath(), e);
		}
		return null;
	}

	private String convertDate(String date) {
		if (date != null && PTRN_DATE_STARTS_WITH_YEAR.matcher(date.trim()).matches()) {
			date = date.trim().substring(0, 4);
		}
		return date;
	}
}
