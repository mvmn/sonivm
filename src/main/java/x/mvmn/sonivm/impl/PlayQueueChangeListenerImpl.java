package x.mvmn.sonivm.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.playqueue.PlaybackQueueChangeListener;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@Component
public class PlayQueueChangeListenerImpl implements PlaybackQueueChangeListener {

	@Autowired
	private SonivmMainWindow sonivmMainWindow;

	@Autowired
	private PlaybackQueueTableModel playbackQueueTableModel;

	@Override
	public void onTableRowsUpdate(int firstRow, int lastRow, boolean waitForUiUpdate) {
		SwingUtil.runOnEDT(() -> playbackQueueTableModel.fireTableRowsUpdated(firstRow, lastRow), waitForUiUpdate);
	}

	@Override
	public void onTableRowsInsert(int firstRow, int lastRow, boolean waitForUiUpdate) {
		SwingUtil.runOnEDT(() -> playbackQueueTableModel.fireTableRowsInserted(firstRow, lastRow), waitForUiUpdate);
		onQueueContentsChange();
	}

	@Override
	public void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate) {
		SwingUtil.runOnEDT(() -> playbackQueueTableModel.fireTableRowsDeleted(firstRow, lastRow), waitForUiUpdate);
		onQueueContentsChange();
	}

	private void onQueueContentsChange() {
		SwingUtil.runOnEDT(() -> sonivmMainWindow.updatePlayQueueSizeLabel(), false);
		sonivmMainWindow.applySearch();
	}
}
