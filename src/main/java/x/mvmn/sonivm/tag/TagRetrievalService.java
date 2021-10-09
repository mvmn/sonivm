package x.mvmn.sonivm.tag;

import java.io.File;

import x.mvmn.sonivm.ui.model.PlaybackQueueEntry.TrackMetadata;

public interface TagRetrievalService {

	TrackMetadata getAudioFileMetadata(File file);

}
