package x.mvmn.sonivm.ui;

import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTable;

public final class PlaybackQueueDropTarget extends DropTarget {
	private static final Logger LOGGER = Logger.getLogger(PlaybackQueueDropTarget.class.getCanonicalName());

	private final SonivmController controller;
	private JTable tblPlayQueue;
	private static final long serialVersionUID = -2570206242097136500L;

	public PlaybackQueueDropTarget(SonivmController controller, JTable tblPlayQueue) throws HeadlessException {
		this.controller = controller;
		this.tblPlayQueue = tblPlayQueue;
	}

	@Override
	public synchronized void drop(DropTargetDropEvent dtde) {

		Point point = dtde.getLocation();
		int row = tblPlayQueue.rowAtPoint(point);

		Transferable transferable = dtde.getTransferable();
		if (dtde.isDataFlavorSupported(PlayQueueTableDnDTransferHandler.DATA_FLAVOR_STRING_ROW_INDEXES_RANGE)) {
			dtde.acceptDrop(DnDConstants.ACTION_LINK);
			try {
				String[] idxRangeStrs = transferable.getTransferData(PlayQueueTableDnDTransferHandler.DATA_FLAVOR_STRING_ROW_INDEXES_RANGE)
						.toString()
						.split("-");

				int startRow = Integer.parseInt(idxRangeStrs[0]);
				int endRow = Integer.parseInt(idxRangeStrs[1]);
				controller.onDropQueueRowsInsideQueue(tblPlayQueue.getRowCount(), startRow, endRow);
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "UnsupportedFlavorException on playback queue table drag-n-drop. Flavors: "
						+ Arrays.toString(transferable.getTransferDataFlavors()), e);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "IO error on playback queue table drag-n-drop.", e);
			}
		} else if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.acceptDrop(DnDConstants.ACTION_LINK);
			try {
				@SuppressWarnings("unchecked")
				List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				controller.onDropFilesToQueue(row, fileList);
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "Drag-n-drop of files into queue failed - unsupported data flavor", e);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Drag-n-drop of files into queue failed - IO issue", e);
			}
		} else {
			super.drop(dtde);
		}
	}
}