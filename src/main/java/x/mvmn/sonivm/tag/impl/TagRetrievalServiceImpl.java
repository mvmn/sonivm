package x.mvmn.sonivm.tag.impl;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.google.common.base.Optional;

import ealvatag.audio.AudioFile;
import ealvatag.audio.AudioFileIO;
import ealvatag.tag.FieldKey;
import ealvatag.tag.Tag;
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
			Optional<Tag> optionalTag = audioFile.getTag();
			if (optionalTag.isPresent()) {
				Tag tag = optionalTag.get();
				return TrackMetadata.builder()
						.trackNumber(StringUtil.nullForBlank(tag.getFirst(FieldKey.TRACK)))
						.artist(StringUtil.nullForBlank(tag.getFirst(FieldKey.ARTIST)))
						.album(StringUtil.nullForBlank(tag.getFirst(FieldKey.ALBUM)))
						.title(StringUtil.nullForBlank(tag.getFirst(FieldKey.TITLE)))
						.date(StringUtil.nullForBlank(convertDate(tag.getFirst(FieldKey.YEAR))))
						.genre(StringUtil.nullForBlank(tag.getFirst(FieldKey.GENRE)))
						.duration(audioFile.getAudioHeader().getDuration(TimeUnit.SECONDS, true))
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
