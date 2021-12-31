package x.mvmn.sonivm.impl;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.playqueue.PlaybackQueueChangeListener;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@Component
public class PlayQueueChangeListenerImpl implements PlaybackQueueChangeListener {

	@Autowired
	private SonivmUI sonivmUI;

	@Autowired
	private PlaybackQueueTableModel playbackQueueTableModel;

	@Autowired
	private PlaybackQueueService playbackQueueService;

	@PostConstruct
	protected void init() {
		playbackQueueService.addChangeListener(this);
	}

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
		SwingUtil.runOnEDT(() -> sonivmUI.getMainWindow().updatePlayQueueSizeLabel(), false);
		sonivmUI.getMainWindow().applySearch();
	}
}
