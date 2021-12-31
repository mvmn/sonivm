package x.mvmn.sonivm.ui;

import java.awt.Cursor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.util.IntRange;

@RequiredArgsConstructor
public class PlayQueueTableDnDTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 3844629189801876896L;

	private static final Logger LOGGER = Logger.getLogger(PlayQueueTableDnDTransferHandler.class.getName());

	public static final DataFlavor DATA_FLAVOR_STRING_ROW_INDEXES_RANGE = new DataFlavor(
			DataFlavor.javaSerializedObjectMimeType + ";class=x.mvmn.sonivm.util.IntRange", "String Row Indexes Range");

	private final JTable playbackQueueTable;
	private final SonivmUIController controller;

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport info) {
		boolean result = info.getComponent() == playbackQueueTable && info.isDrop()
				&& info.isDataFlavorSupported(DATA_FLAVOR_STRING_ROW_INDEXES_RANGE)
				|| info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		playbackQueueTable.setCursor(result ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
		return result;
	}

	@Override
	protected Transferable createTransferable(JComponent component) {
		if (component == playbackQueueTable) {
			int[] selectedRows = playbackQueueTable.getSelectedRows();
			// "" + selectedRows[0] + "-" + selectedRows[selectedRows.length - 1]
			return new DataHandler(new IntRange(selectedRows[0], selectedRows[selectedRows.length - 1]),
					DATA_FLAVOR_STRING_ROW_INDEXES_RANGE.getMimeType());
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

		if (info.getTransferable().isDataFlavorSupported(DATA_FLAVOR_STRING_ROW_INDEXES_RANGE)) {
			try {
				IntRange intRange = (IntRange) info.getTransferable().getTransferData(DATA_FLAVOR_STRING_ROW_INDEXES_RANGE);

				return controller.onDropQueueRowsInsideQueue(insertPosition, intRange.getFrom(), intRange.getTo());
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "UnsupportedFlavorException on playback queue table drag-n-drop. Flavors: "
						+ Arrays.toString(info.getTransferable().getTransferDataFlavors()), e);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "IO error on playback queue table drag-n-drop.", e);
			}
		} else if (info.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			try {
				@SuppressWarnings("unchecked")
				List<File> fileList = (List<File>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
				controller.onDropFilesToQueue(insertPosition, fileList);
			} catch (UnsupportedFlavorException e) {
				LOGGER.log(Level.WARNING, "UnsupportedFlavorException on playback queue table files drag-n-drop. Flavors: "
						+ Arrays.toString(info.getTransferable().getTransferDataFlavors()), e);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "IO error on playback queue table files drag-n-drop.", e);
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

	@Override

	public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
		if ((action == COPY) && (getSourceActions(comp) & action) != 0) {
			int[] selectedRows = playbackQueueTable.getSelectedRows();
			if (selectedRows.length > 0) {
				StringBuilder result = new StringBuilder();
				for (int rowIndex : selectedRows) {
					for (int colIndex = 0; colIndex < playbackQueueTable.getColumnCount(); colIndex++) {
						if (colIndex > 0) {
							result.append("\t");
						}
						Object value = playbackQueueTable.getValueAt(rowIndex, colIndex);
						result.append(value != null ? value.toString() : "");
					}
					result.append(System.lineSeparator());
				}

				Transferable t = new DataHandler(result.toString(), new DataFlavor(String.class, "Text").getMimeType());
				try {
					clip.setContents(t, null);
					exportDone(comp, t, action);
					return;
				} catch (IllegalStateException ise) {
					exportDone(comp, t, NONE);
					throw ise;
				}
			}

		}

		exportDone(comp, null, NONE);
	}
}