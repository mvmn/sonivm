package x.mvmn.sonivm.ui;

import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;

public class PlayQueueTableDnDTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 3844629189801876896L;

	private static final Logger LOGGER = Logger.getLogger(PlayQueueTableDnDTransferHandler.class.getName());

	private static final DataFlavor localObjectFlavor = new DataFlavor(String.class, "Integer Row Index");

	private final JTable playbackQueueTable;
	private final PlaybackQueueTableModel playbackQueueTableModel;

	public PlayQueueTableDnDTransferHandler(JTable playbackQueueTable, PlaybackQueueTableModel playbackQueueTableModel) {
		this.playbackQueueTable = playbackQueueTable;
		this.playbackQueueTableModel = playbackQueueTableModel;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport info) {
		boolean result = info.getComponent() == playbackQueueTable && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor)
				|| info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		playbackQueueTable.setCursor(result ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
		return result;
	}

	@Override
	protected Transferable createTransferable(JComponent component) {
		if (component == playbackQueueTable) {
			int[] selectedRows = playbackQueueTable.getSelectedRows();
			return new DataHandler("" + selectedRows[0] + "-" + selectedRows[selectedRows.length - 1], localObjectFlavor.getMimeType());
		} else {
			return super.createTransferable(component);
		}
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport info) {
		if (info.getComponent() != playbackQueueTable) {
			return false;
		}
		JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
		int insertPosition = dl.getRow();
		int max = playbackQueueTable.getModel().getRowCount();
		if (insertPosition < 0 || insertPosition > max) {
			insertPosition = max;
		}
		playbackQueueTable.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

		String[] idxRangeStrs;
		if (info.getTransferable().isDataFlavorSupported(localObjectFlavor)) {
			try {
				idxRangeStrs = info.getTransferable().getTransferData(localObjectFlavor).toString().split("-");

				int startRow = Integer.parseInt(idxRangeStrs[0]);
				int endRow = Integer.parseInt(idxRangeStrs[1]);
				int rowCount = endRow - startRow + 1;
				if (insertPosition > endRow || insertPosition < startRow) {
					List<PlaybackQueueEntry> selectedRowValues = new ArrayList<>(rowCount);
					for (int i = startRow; i <= endRow; i++) {
						selectedRowValues.add(playbackQueueTableModel.getRowValue(i));
					}

					if (endRow < insertPosition) {
						insertPosition -= rowCount;
					}

					playbackQueueTableModel.deleteRows(startRow, endRow + 1);
					playbackQueueTableModel.addRows(insertPosition, selectedRowValues);

					playbackQueueTable.getSelectionModel().setSelectionInterval(insertPosition, insertPosition + rowCount - 1);

					return true;
				} else {
					return false;
				}
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "UnsupportedFlavorException on playback queue table drag-n-drop. Flavors: "
						+ Arrays.toString(info.getTransferable().getTransferDataFlavors()), e);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "IO error on playback queue table drag-n-drop.", e);
			}
		}
		return false;
	}

	@Override
	protected void exportDone(JComponent c, Transferable t, int act) {
		if (act == TransferHandler.MOVE) {
			playbackQueueTable.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}