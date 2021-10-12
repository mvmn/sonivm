package x.mvmn.sonivm.tag.impl;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.google.common.base.Optional;

import ealvatag.audio.AudioFile;
import ealvatag.audio.AudioFileIO;
import ealvatag.tag.FieldKey;
import ealvatag.tag.Tag;
import x.mvmn.sonivm.tag.TagRetrievalService;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry.TrackMetadata;

@Service
public class TagRetrievalServiceImpl implements TagRetrievalService {

	@Override
	public TrackMetadata getAudioFileMetadata(File file) {
		try {
			AudioFile audioFile = AudioFileIO.read(file);
			Optional<Tag> optionalTag = audioFile.getTag();
			if (optionalTag.isPresent()) {
				Tag tag = optionalTag.get();
				return TrackMetadata.builder()
						.trackNumber(nullForBlank(tag.getFirst(FieldKey.TRACK)))
						.artist(nullForBlank(tag.getFirst(FieldKey.ARTIST)))
						.album(nullForBlank(tag.getFirst(FieldKey.ALBUM)))
						.title(nullForBlank(tag.getFirst(FieldKey.TITLE)))
						.date(nullForBlank(tag.getFirst(FieldKey.YEAR)))
						.genre(nullForBlank(tag.getFirst(FieldKey.GENRE)))
						.duration(audioFile.getAudioHeader().getDuration(TimeUnit.SECONDS, true))
						.build();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read tags for a file " + file.getAbsolutePath(), e);
		}
		return null;
	}

	private String nullForBlank(String str) {
		return str == null || str.isBlank() ? null : str;
	}
}
