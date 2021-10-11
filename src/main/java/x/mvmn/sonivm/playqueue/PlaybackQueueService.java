package x.mvmn.sonivm.playqueue;

import java.util.Collection;
import java.util.List;

import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;

public interface PlaybackQueueService {

	int getCurrentQueuePosition();

	PlaybackQueueEntry getCurrentEntry();

	PlaybackQueueEntry getEntryByIndex(int index);

	int queueSize();

	void setCurrentQueuePosition(int newPosition);

	void clearQueue();

	void addRows(Collection<PlaybackQueueEntry> newRows);

	void addRows(int atIndex, Collection<PlaybackQueueEntry> newRows);

	void deleteRows(int fromIndex, int toIndex);

	void moveRows(int toIndex, int firstRow, int lastRow);

	List<PlaybackQueueEntry> getCopyOfQueue();

	void signalUpdateInRow(int rowIndex);

	void signalUpdateInRows(int firstRow, int lastRow);

	void signalUpdateInTrackInfo(PlaybackQueueEntry updatedEntry);

	void setChangeListener(PlaybackQueueChangeListener changeListener);
}