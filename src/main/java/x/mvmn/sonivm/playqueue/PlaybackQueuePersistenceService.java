package x.mvmn.sonivm.playqueue;

public interface PlaybackQueuePersistenceService {

	void savePlayQueuesContents();

	void saveQueueNames();

	void restorePlayQueues();

	void savePlayQueueContents(int queueIndex);

}