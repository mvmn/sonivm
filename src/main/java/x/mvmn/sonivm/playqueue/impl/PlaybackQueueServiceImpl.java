package x.mvmn.sonivm.playqueue.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import x.mvmn.sonivm.model.IntRange;
import x.mvmn.sonivm.playqueue.PlaybackQueueChangeListener;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;

@Service
public class PlaybackQueueServiceImpl implements PlaybackQueueService {

	private List<PlaybackQueueEntry> data = Collections.synchronizedList(new ArrayList<PlaybackQueueEntry>());
	private volatile int currentQueuePosition = -1;

	private static final Object QUEUE_POSITION_LOCK_OBJ = new Object();
	private static final Object DATA_LOCK_OBJ = new Object();

	private PlaybackQueueChangeListener changeListener;

	@Override
	public int getCurrentQueuePosition() {
		return currentQueuePosition;
	}

	@Override
	public PlaybackQueueEntry getCurrentEntry() {
		return currentQueuePosition >= 0 ? data.get(currentQueuePosition) : null;
	}

	@Override
	public PlaybackQueueEntry getEntryByIndex(int index) {
		return index >= 0 && index < data.size() ? data.get(index) : null;
	};

	@Override
	public int getQueueSize() {
		return data.size();
	}

	@Override
	public void setCurrentQueuePosition(int newPosition) {
		int rows = data.size();
		int oldPosition = this.currentQueuePosition;
		if (newPosition >= 0 && newPosition < rows) {
			this.currentQueuePosition = newPosition;
			onTableRowsUpdate(newPosition, newPosition, false);
		} else {
			this.currentQueuePosition = -1;
		}
		if (oldPosition >= 0 && oldPosition < rows) {
			onTableRowsUpdate(oldPosition, oldPosition, false);
		}
	}

	@Override
	public void clearQueue() {
		synchronized (DATA_LOCK_OBJ) {
			data.clear();
		}
	}

	@Override
	public void addRows(Collection<PlaybackQueueEntry> newRows) {
		addRows(-1, newRows);
	}

	@Override
	public void addRows(int atIndex, Collection<PlaybackQueueEntry> newRows) {
		synchronized (DATA_LOCK_OBJ) {
			int dataSizeBeforeAdd = data.size();
			if (atIndex < 0 || atIndex >= dataSizeBeforeAdd) {
				int numberAdded = newRows.size();
				data.addAll(newRows);
				onTableRowsInsert(dataSizeBeforeAdd, dataSizeBeforeAdd + numberAdded - 1, false);
			} else {
				int numberAdded = newRows.size();
				data.addAll(atIndex, newRows);
				synchronized (QUEUE_POSITION_LOCK_OBJ) {
					int currentQueuePosition = getCurrentQueuePosition();
					if (currentQueuePosition >= atIndex) {
						currentQueuePosition += numberAdded;
						setCurrentQueuePosition(currentQueuePosition);
					}
				}
				onTableRowsInsert(atIndex, atIndex + numberAdded - 1, false);
			}
		}
	}

	@Override
	public void deleteRows(int fromIndex, int toIndex) {
		synchronized (DATA_LOCK_OBJ) {
			int dataSizeBeforeDelete = data.size();
			if (fromIndex < 0) {
				fromIndex = 0;
			}
			if (toIndex < 0) {
				toIndex = 0;
			}
			if (fromIndex >= dataSizeBeforeDelete) {
				fromIndex = dataSizeBeforeDelete - 1;
			}
			if (toIndex > dataSizeBeforeDelete) {
				toIndex = dataSizeBeforeDelete;
			}

			if (fromIndex < toIndex) {
				for (int i = toIndex - 1; i >= fromIndex; i--) {
					data.remove(i);
				}
				int firstRow = fromIndex;
				int lastRow = toIndex - 1;
				onTableRowsDelete(firstRow, lastRow, true);
			}
		}
		synchronized (QUEUE_POSITION_LOCK_OBJ) {
			int currentQueuePosition = getCurrentQueuePosition();
			if (currentQueuePosition > toIndex - 1) {
				currentQueuePosition -= (toIndex - fromIndex);
				setCurrentQueuePosition(currentQueuePosition);
			} else if (currentQueuePosition >= fromIndex && currentQueuePosition < toIndex) {
				setCurrentQueuePosition(-1);
			}
		}

	}

	@Override
	public void moveRows(int toIndex, int firstRow, int lastRow) {
		int originalToIndex = toIndex;
		int rowCount = lastRow - firstRow + 1;
		synchronized (DATA_LOCK_OBJ) {
			List<PlaybackQueueEntry> selectedRowValues = new ArrayList<>(rowCount);
			selectedRowValues.addAll(data.subList(firstRow, lastRow + 1));
			for (int i = lastRow; i >= firstRow; i--) {
				data.remove(i);
			}
			onTableRowsDelete(firstRow, lastRow, true);
			if (lastRow < toIndex) {
				toIndex -= rowCount;
			}
			data.addAll(toIndex, selectedRowValues);
			int finalToIndex = toIndex;
			onTableRowsInsert(finalToIndex, finalToIndex + rowCount - 1, true);
		}

		// With regard to current queue position there are 6 cases, 4 of which need to be handled:
		// 1 - Rows above queue position were moved to somewhere else above queue position - inconsequential.
		// 2 - Rows below queue position were moved to somewhere else below queue position - inconsequential.
		// 3 - Rows above queue position were moved below queue position - queue position must be reduced by number of rows moved.
		// 4 - Rows below queue position were moved above queue position - queue position must be increased by number of rows moved.
		// 5 - Rows that include queue position were moved somewhere above - queue position must be decreased by value of offset
		// from original to new location of the moved rows.
		// 6 - Rows that include queue position were moved somewhere below - queue position must be increased by move offset,
		// but decreased by number of rows removed before it during move.
		synchronized (QUEUE_POSITION_LOCK_OBJ) {
			int currentQueuePosition = getCurrentQueuePosition();
			if (currentQueuePosition >= firstRow && currentQueuePosition <= lastRow) {
				if (currentQueuePosition > originalToIndex) {
					currentQueuePosition += originalToIndex - firstRow;
					setCurrentQueuePosition(currentQueuePosition);
				} else {
					//
					currentQueuePosition += originalToIndex - firstRow - rowCount;
					setCurrentQueuePosition(currentQueuePosition);
				}
			} else if (lastRow < currentQueuePosition && originalToIndex > currentQueuePosition) {
				currentQueuePosition -= rowCount;
				setCurrentQueuePosition(currentQueuePosition);
			} else if (firstRow > currentQueuePosition && originalToIndex <= currentQueuePosition) {
				currentQueuePosition += rowCount;
				setCurrentQueuePosition(currentQueuePosition);
			}
		}
	}

	@Override
	public List<PlaybackQueueEntry> getCopyOfQueue() {
		synchronized (DATA_LOCK_OBJ) {
			return new ArrayList<>(data);
		}
	}

	@Override
	public void signalUpdateInRow(int rowIndex) {
		onTableRowsUpdate(rowIndex, rowIndex, false);
	}

	@Override
	public void signalUpdateInRows(int firstRow, int lastRow) {
		onTableRowsUpdate(firstRow, lastRow, false);
	}

	@Override
	public void signalUpdateInTrackInfo(PlaybackQueueEntry updatedEntry) {
		// TODO: optimize
		int row = data.indexOf(updatedEntry);
		if (row >= 0) {
			signalUpdateInRow(row);
		}
	}

	@Override
	public void setChangeListener(PlaybackQueueChangeListener changeListener) {
		this.changeListener = changeListener;
	}

	private void onTableRowsUpdate(int firstRow, int lastRow, boolean waitForUiUpdate) {
		if (changeListener != null) {
			changeListener.onTableRowsUpdate(firstRow, lastRow, waitForUiUpdate);
		}
	}

	private void onTableRowsInsert(int firstRow, int lastRow, boolean waitForUiUpdate) {
		if (changeListener != null) {
			changeListener.onTableRowsInsert(firstRow, lastRow, waitForUiUpdate);
		}

	}

	private void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate) {
		if (changeListener != null) {
			changeListener.onTableRowsDelete(firstRow, lastRow, waitForUiUpdate);
		}
	}

	@Override
	public int[] findTracksByProperty(String value, boolean useArtist) {
		if (value == null) {
			value = "";
		} else {
			value = value.trim();
		}
		List<Integer> result = new ArrayList<>();
		synchronized (DATA_LOCK_OBJ) {
			int rows = data.size();
			for (int i = 0; i < rows; i++) {
				PlaybackQueueEntry queueEntry = data.get(i);
				String valB = useArtist ? queueEntry.getArtist() : queueEntry.getAlbum();
				if (valB == null) {
					valB = "";
				} else {
					valB = valB.trim();
				}
				if (trackPropertiesEqual(value, valB)) {
					result.add(i);
				}
			}
		}
		return result.stream().mapToInt(Integer::intValue).toArray();
	}

	private boolean propertyEquals(PlaybackQueueEntry entryA, PlaybackQueueEntry entryB, boolean byArtist) {
		String valA = byArtist ? entryA.getArtist() : entryA.getAlbum();
		String valB = byArtist ? entryB.getArtist() : entryB.getAlbum();
		return trackPropertiesEqual(valA, valB);
	}

	private boolean trackPropertiesEqual(String valA, String valB) {
		if (valA == null) {
			valA = "";
		}
		if (valB == null) {
			valB = "";
		}
		return valA.trim().equalsIgnoreCase(valB.trim());
	}

	@Override
	public IntRange detectTrackRange(int currentPosition, boolean byArtist) {
		synchronized (DATA_LOCK_OBJ) {
			int trackCount = data.size();
			if (trackCount > 0) {
				PlaybackQueueEntry currentTrack = data.get(currentPosition);
				int start = currentPosition;
				while (start > 0) {
					PlaybackQueueEntry prevTrack = data.get(start - 1);
					if (!propertyEquals(currentTrack, prevTrack, byArtist)) {
						break;
					} else {
						start--;
					}
				}
				int end = currentPosition;
				while (end < trackCount - 1) {
					PlaybackQueueEntry nextTrack = data.get(end + 1);
					if (!propertyEquals(currentTrack, nextTrack, byArtist)) {
						break;
					} else {
						end++;
					}
				}
				return new IntRange(start, end);
			} else {
				return new IntRange(-1, -1);
			}
		}
	}
}
