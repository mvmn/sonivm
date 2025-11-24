package x.mvmn.sonivm.playqueue;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import x.mvmn.sonivm.util.IntRange;

public interface PlaybackQueueService {

	int getQueueSize();

	int getCurrentQueuePosition();

	void setCurrentQueuePosition(int newPosition);

	PlaybackQueueEntry getCurrentEntry();

	PlaybackQueueEntry getEntryByIndex(int index);

	void clearQueue();

	void addRows(Collection<PlaybackQueueEntry> newRows);

	void addRows(int atIndex, Collection<PlaybackQueueEntry> newRows);

	void deleteRows(int fromIndex, int toIndex);

	void moveRows(int toIndex, int firstRow, int lastRow);

	List<PlaybackQueueEntry> getCopyOfQueue();

	void signalUpdateInRow(int rowIndex);

	void signalUpdateInRows(int firstRow, int lastRow);

	void signalUpdateInTrackInfo(PlaybackQueueEntry updatedEntry);

	void addChangeListener(PlaybackQueueChangeListener changeListener);

	int[] findTracks(Predicate<PlaybackQueueEntry> matches);

	IntRange detectTrackRange(int currentPosition, BiPredicate<PlaybackQueueEntry, PlaybackQueueEntry> condition);

	void addQueue(String name);

	void removeQueue(int queueIndex);

	int getCurrentQueue();

	void setCurrentQueue(int currentQueue);

	String getQueueName(int queueIndex);

	int getQueuesCount();

	int getCurrentPlayQueue();
}