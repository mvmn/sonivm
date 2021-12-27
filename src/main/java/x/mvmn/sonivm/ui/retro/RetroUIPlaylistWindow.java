package x.mvmn.sonivm.ui.retro;

import java.awt.Color;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import x.mvmn.sonivm.ui.retro.rasterui.RasterFrameWindow;

@AllArgsConstructor
@Builder
public class RetroUIPlaylistWindow {

	@Getter
	protected final RasterFrameWindow window;
	@Getter
	protected final JTable playlistTable;
	@Getter
	protected final DefaultTableModel playlistTableModel;
	@Getter
	protected final PlaylistColors playlistColors;

	@AllArgsConstructor
	@Builder
	public static class PlaylistColors {
		@Getter
		protected final Color backgroundColor;
		@Getter
		protected final Color textColor;
		@Getter
		protected final Color currentTrackTextColor;
		@Getter
		protected final Color selectionBackgroundColor;
	}
}
