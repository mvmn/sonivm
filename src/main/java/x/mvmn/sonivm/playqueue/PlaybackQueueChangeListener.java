package x.mvmn.sonivm.playqueue;

public interface PlaybackQueueChangeListener {

	public void onTableRowsUpdate(int firstRow, int lastRow, boolean waitForUiUpdate);

	public void onTableRowsInsert(int firstRow, int lastRow, boolean waitForUiUpdate);

	public void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate);
}