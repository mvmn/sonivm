package x.mvmn.sonivm;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;

public interface PlaybackListener {

	void onPlaybackStateChange(PlaybackState stopped);

	void onPlaybackError(String errorMessage);

	void onPlaybackProgress(long playbackPositionMillis, int totalDurationSeconds);

	void onPlaybackStart(AudioFileInfo audioInfo, PlaybackQueueEntry currentTrack);
}
