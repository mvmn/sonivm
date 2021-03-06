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

import x.mvmn.sonivm.util.IntRange;

public final class PlaybackQueueDropTarget extends DropTarget {
	private static final Logger LOGGER = Logger.getLogger(PlaybackQueueDropTarget.class.getCanonicalName());

	private final SonivmUIController controller;
	private JTable tblPlayQueue;
	private static final long serialVersionUID = -2570206242097136500L;

	public PlaybackQueueDropTarget(SonivmUIController controller, JTable tblPlayQueue) throws HeadlessException {
		this.controller = controller;
		this.tblPlayQueue = tblPlayQueue;
	}

	@Override
	public synchronized void drop(DropTargetDropEvent dtde) {

		Point point = dtde.getLocation();
		int row = this.getComponent() == tblPlayQueue ? tblPlayQueue.rowAtPoint(point) : -1;

		Transferable transferable = dtde.getTransferable();
		if (dtde.isDataFlavorSupported(PlayQueueTableDnDTransferHandler.DATA_FLAVOR_STRING_ROW_INDEXES_RANGE)) {
			dtde.acceptDrop(DnDConstants.ACTION_COPY);
			try {
				IntRange intRange = (IntRange) transferable
						.getTransferData(PlayQueueTableDnDTransferHandler.DATA_FLAVOR_STRING_ROW_INDEXES_RANGE);

				controller.onDropQueueRowsInsideQueue(tblPlayQueue.getRowCount(), intRange.getFrom(), intRange.getTo());
				dtde.dropComplete(true);
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "UnsupportedFlavorException on playback queue table drag-n-drop. Flavors: "
						+ Arrays.toString(transferable.getTransferDataFlavors()), e);
				dtde.rejectDrop();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "IO error on playback queue table drag-n-drop.", e);
				dtde.rejectDrop();
			}
		} else if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.acceptDrop(DnDConstants.ACTION_COPY);
			try {
				@SuppressWarnings("unchecked")
				List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				controller.onDropFilesToQueue(row, fileList);
				dtde.dropComplete(true);
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "Drag-n-drop of files into queue failed - unsupported data flavor", e);
				dtde.rejectDrop();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Drag-n-drop of files into queue failed - IO issue", e);
				dtde.rejectDrop();
			}
		} else {
			super.drop(dtde);
		}
	}
}