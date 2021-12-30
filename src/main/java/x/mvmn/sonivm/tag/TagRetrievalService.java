package x.mvmn.sonivm.tag;

import java.io.File;

import x.mvmn.sonivm.playqueue.PlaybackQueueEntry.TrackMetadata;

public interface TagRetrievalService {

	TrackMetadata getAudioFileMetadata(File file);

}
