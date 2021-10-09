package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.util.TimeUnitUtil;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

public class SonivmMainWindow extends JFrame {
	private static final long serialVersionUID = -3402450540379541023L;

	private final JLabel lblStatus;
	private final JLabel lblNowPlaying;
	private final JLabel lblPlayTimeElapsed;
	private final JLabel lblPlayTimeRemaining;
	// private final JLabel lblPlayTimeTotal;
	private final JTable tblPlayQueue;
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

		tblPlayQueue = new JTable(playbackQueueTableModel);
		tblPlayQueue.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		tblPlayQueue.setCellSelectionEnabled(false);
		tblPlayQueue.setRowSelectionAllowed(true);
		tblPlayQueue.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 8392498039681170058L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
					int column) {
				Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				if (row == playbackQueueTableModel.getCurrentQueuePosition()) {
					result.setFont(result.getFont().deriveFont(Font.BOLD));
				}

				return result;
			}
		});
		DefaultTableCellRenderer rightRendererForDuration = new DefaultTableCellRenderer();
		rightRendererForDuration.setHorizontalAlignment(JLabel.RIGHT);
		tblPlayQueue.getColumnModel().getColumn(5).setCellRenderer(rightRendererForDuration);
		DefaultTableCellRenderer rightRendererForDate = new DefaultTableCellRenderer();
		rightRendererForDate.setHorizontalAlignment(JLabel.RIGHT);
		tblPlayQueue.getColumnModel().getColumn(6).setCellRenderer(rightRendererForDate);

		treeTrackLibrary = new JTree();

		btnPlayPause = new JButton("->");
		btnStop = new JButton("[x]");
		btnNextTrack = new JButton(">>");
		btnPreviousTrack = new JButton("<<");
		lblStatus = new JLabel("Stopped");
		lblNowPlaying = new JLabel("Now playing:");
		lblPlayTimeElapsed = new JLabel("00:00/00:00");
		lblPlayTimeRemaining = new JLabel("-00:00/00:00");
		// lblPlayTimeTotal = new JLabel("00:00");

		seekSlider = new JSlider(0, 0);
		seekSlider.setEnabled(false);
		SwingUtil.makeJSliderMoveToClickPoistion(seekSlider);

		volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
		SwingUtil.makeJSliderMoveToClickPoistion(volumeSlider);

		JPanel playbackButtonsPanel = new JPanel(new GridLayout(1, 4));
		JButton[] buttons = new JButton[] { btnPreviousTrack, btnPlayPause, btnStop, btnNextTrack };
		Stream.of(buttons).forEach(playbackButtonsPanel::add);
		Stream.of(buttons).forEach(btn -> btn.setFocusable(false));

		JPanel topPanel = SwingUtil.panel(BorderLayout::new)
				.addEast(playbackButtonsPanel)
				.addCenter(SwingUtil.panel(BorderLayout::new)
						.addWest(lblPlayTimeElapsed)
						.addCenter(seekSlider)
						.addEast(lblPlayTimeRemaining)
						.build())
				.addNorth(lblNowPlaying)
				.build();
		topPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		JPanel bottomPanel = SwingUtil.panel(BorderLayout::new).addCenter(lblStatus).build();
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		volumeSlider.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		seekSlider.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		lblPlayTimeElapsed.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		lblPlayTimeRemaining.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		lblNowPlaying.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 8));
		lblNowPlaying.setFont(lblNowPlaying.getFont().deriveFont(Font.BOLD));

		JScrollPane scrollTblPlayQueue = new JScrollPane(tblPlayQueue);
		JSplitPane spLibraryAndPlayQueue = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, new JScrollPane(treeTrackLibrary),
				scrollTblPlayQueue);

		tblPlayQueue.setDragEnabled(true);
		tblPlayQueue.setDropMode(DropMode.USE_SELECTION);
		tblPlayQueue.setTransferHandler(new PlayQueueTableDnDTransferHandler(tblPlayQueue, controller));

		DropTarget dropTarget = new PlaybackQueueDropTarget(controller, tblPlayQueue);
		// tblPlayQueue.setDropTarget(dropTarget);
		scrollTblPlayQueue.setDropTarget(dropTarget);

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane()
				.add(SwingUtil.panel(BorderLayout::new).addSouth(topPanel).addNorth(lblNowPlaying).build(), BorderLayout.NORTH);
		this.getContentPane().add(spLibraryAndPlayQueue, BorderLayout.CENTER);
		this.getContentPane().add(volumeSlider, BorderLayout.EAST);
		this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		registerActionsWithController(controller);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent wndEvent) {
				controller.onWindowClose();
			}
		});
	}

	private void registerActionsWithController(SonivmController controller) {
		volumeSlider.addChangeListener(event -> controller.onVolumeChange(volumeSlider.getValue()));
		seekSlider.addChangeListener(event -> {
			if (seekSliderIsDragged) {
				controller.onSeek(seekSlider.getValue());
			}
		});
		seekSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				seekSliderIsDragged = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				seekSliderIsDragged = false;
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// controller.onSeek(seekSlider.getValue());
			}
		});

		btnPlayPause.addActionListener(event -> controller.onPlayPause());
		btnStop.addActionListener(event -> controller.onStop());
		btnNextTrack.addActionListener(event -> controller.onNextTrack());
		btnPreviousTrack.addActionListener(event -> controller.onPreviousTrack());

		tblPlayQueue.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2 && tblPlayQueue.getSelectedRow() != -1) {
					Point point = mouseEvent.getPoint();
					int row = tblPlayQueue.rowAtPoint(point);
					controller.onTrackSelect(row);
				}
			}
		});

		int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
		InputMap inputMap = tblPlayQueue.getInputMap(condition);
		ActionMap actionMap = tblPlayQueue.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "Delete");
		actionMap.put("Delete", new AbstractAction() {
			private static final long serialVersionUID = 8828376654199394308L;

			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = tblPlayQueue.getSelectedRows();
				if (selectedRows != null && selectedRows.length > 0) {
					controller.onDeleteRowsFromQueue(selectedRows[0], selectedRows[selectedRows.length - 1]);
				}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Select");
		actionMap.put("Select", new AbstractAction() {
			private static final long serialVersionUID = 8828376654199394308L;

			public void actionPerformed(ActionEvent e) {
				int selectedRow = tblPlayQueue.getSelectedRow();
				if (selectedRow >= 0 && selectedRow < tblPlayQueue.getRowCount()) {
					controller.onTrackSelect(selectedRow);
				}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "PlayPause");
		actionMap.put("PlayPause", new AbstractAction() {
			private static final long serialVersionUID = 8828376654199394308L;

			public void actionPerformed(ActionEvent e) {
				controller.onPlayPause();
			}
		});
	}

	public JTable getPlayQueueTable() {
		return tblPlayQueue;
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

	public void updateStatus(String status) {
		this.lblStatus.setText(status);
	}

	public void setPlayPauseButtonState(boolean playing) {
		btnPlayPause.setText(playing ? "||" : "->");
	}

	public void setTotalPlayTimeDisplay(long totalSeconds) {
		// lblPlayTimeTotal.setText(TimeUnitUtil.prettyPrintFromSeconds(totalSeconds));
	}

	public void setCurrentPlayTimeDisplay(long playedSeconds, long totalSeconds) {
		long remainingSeconds = totalSeconds - playedSeconds;
		lblPlayTimeRemaining.setText(
				"-" + TimeUnitUtil.prettyPrintFromSeconds(remainingSeconds) + "/" + TimeUnitUtil.prettyPrintFromSeconds(totalSeconds));
		lblPlayTimeElapsed
				.setText(TimeUnitUtil.prettyPrintFromSeconds(playedSeconds) + "/" + TimeUnitUtil.prettyPrintFromSeconds(totalSeconds));
	}

	public void updateNowPlaying(PlaybackQueueEntry trackInfo) {
		this.lblNowPlaying.setText("Now playing: " + (trackInfo != null ? trackInfo.toDisplayStr() : ""));
	}

	public void scrollToTrack(int rowNumber) {
		tblPlayQueue.scrollRectToVisible(new Rectangle(tblPlayQueue.getCellRect(rowNumber, 0, true)));
	}
}
