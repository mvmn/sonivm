package x.mvmn.sonivm.ui.retro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import x.mvmn.sonivm.ui.retro.rasterui.RasterFrameWindow;

@AllArgsConstructor
@Builder
public class RetroUIPlaylistWindow {

	@Getter
	protected final RasterFrameWindow window;
}
