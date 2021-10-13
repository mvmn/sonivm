package x.mvmn.sonivm.tag.impl;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.stereotype.Service;

import x.mvmn.sonivm.tag.TagRetrievalService;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry.TrackMetadata;
import x.mvmn.sonivm.util.StringUtil;
import x.mvmn.sonivm.util.TimeDateUtil;

@Service
public class TagRetrievalServiceImpl implements TagRetrievalService {

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
						.date(StringUtil.nullForBlank(TimeDateUtil.yearFromDateTagValue(tag.getFirst(FieldKey.YEAR))))
						.genre(StringUtil.nullForBlank(tag.getFirst(FieldKey.GENRE)))
						.duration(trackLength > 0 ? trackLength : null)
						.build();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read tags for a file " + file.getAbsolutePath(), e);
		}
		return null;
	}
}
