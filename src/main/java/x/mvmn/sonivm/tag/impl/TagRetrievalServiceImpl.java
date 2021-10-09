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
						.trackNumber(tag.getFirst(FieldKey.TRACK))
						.artist(tag.getFirst(FieldKey.ARTIST))
						.album(tag.getFirst(FieldKey.ALBUM))
						.title(tag.getFirst(FieldKey.TITLE))
						.date(tag.getFirst(FieldKey.YEAR))
						.genre(tag.getFirst(FieldKey.GENRE))
						.duration(audioFile.getAudioHeader().getDuration(TimeUnit.SECONDS, true))
						.build();
			}
		} catch (Exception e) {
			// TODO: proper handling
			throw new RuntimeException(e);
		}
		return null;
	}
}
