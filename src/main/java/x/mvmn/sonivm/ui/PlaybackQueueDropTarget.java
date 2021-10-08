package x.mvmn.sonivm.ui;

import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
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

		if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.acceptDrop(DnDConstants.ACTION_LINK);
			try {
				@SuppressWarnings("unchecked")
				List<File> fileList = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
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