package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.dnd.DropTarget;
import java.util.stream.Stream;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;

import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

public class SonivmMainWindow extends JFrame {
	private static final long serialVersionUID = -3402450540379541023L;

	private final JLabel lblStatus;
	private final JTable tblPlayQueue;
	// private final PlaybackQueueTableModel playbackQueueTableModel;
	private final JTree treeTrackLibrary;
	private final JButton btnPlayPause;
	private final JButton btnStop;
	private final JButton btnNextTrack;
	private final JButton btnPreviousTrack;
	private final JSlider seekSlider;
	private final JSlider volumeSlider;
	private volatile boolean seekSliderIsDragged;

	public SonivmMainWindow(String title, SonivmController controller, PlaybackQueueTableModel playbackQueueTableModel) {
		super(title);

		// this.playbackQueueTableModel = playbackQueueTableModel;
		tblPlayQueue = new JTable(playbackQueueTableModel);
		tblPlayQueue.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		tblPlayQueue.setCellSelectionEnabled(false);
		tblPlayQueue.setRowSelectionAllowed(true);
		treeTrackLibrary = new JTree();

		btnPlayPause = new JButton("->");
		btnStop = new JButton("[x]");
		btnNextTrack = new JButton(">>");
		btnPreviousTrack = new JButton("<<");
		lblStatus = new JLabel("Stopped");

		seekSlider = new JSlider(0, 0);
		seekSlider.setEnabled(false);

		volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);

		JPanel playbackButtonsPanel = new JPanel(new GridLayout(1, 4));
		JButton[] buttons = new JButton[] { btnPreviousTrack, btnPlayPause, btnStop, btnNextTrack };
		Stream.of(buttons).forEach(playbackButtonsPanel::add);
		Stream.of(buttons).forEach(btn -> btn.setFocusable(false));

		JPanel topPanel = SwingUtil.panel(BorderLayout::new).addEast(playbackButtonsPanel).addCenter(seekSlider).build();

		JPanel bottomPanel = SwingUtil.panel(BorderLayout::new).addCenter(lblStatus).build();

		JScrollPane scrollTblPlayQueue = new JScrollPane(tblPlayQueue);
		JSplitPane spLibraryAndPlayQueue = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, new JScrollPane(treeTrackLibrary),
				scrollTblPlayQueue);

		tblPlayQueue.setDragEnabled(true);
		tblPlayQueue.setDropMode(DropMode.USE_SELECTION);
		tblPlayQueue.setTransferHandler(new PlayQueueTableDnDTransferHandler(tblPlayQueue, playbackQueueTableModel));

		DropTarget dropTarget = new PlaybackQueueDropTarget(controller, tblPlayQueue);
		// tblPlayQueue.setDropTarget(dropTarget);
		scrollTblPlayQueue.setDropTarget(dropTarget);

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(topPanel, BorderLayout.NORTH);
		this.getContentPane().add(spLibraryAndPlayQueue, BorderLayout.CENTER);
		this.getContentPane().add(volumeSlider, BorderLayout.EAST);
		this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	public void allowSeek(int maxSliderValue) {
		this.seekSlider.setMaximum(maxSliderValue);
		this.seekSlider.setEnabled(true);
	}

	public void disallowSeek() {
		this.seekSlider.setMaximum(0);
		this.seekSlider.setEnabled(false);
	}

	public void updateSeekSliderPosition(int sliderNewPosition) {
		if (!seekSliderIsDragged) {
			seekSlider.getModel().setValue(sliderNewPosition);
		}
	}
}
