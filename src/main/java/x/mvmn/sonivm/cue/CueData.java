package x.mvmn.sonivm.cue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CueData {
	private String title;
	private String performer;
	private String songwriter;
	private CueDataFileData fileData;
	@Builder.Default
	private Map<String, String> rems = new HashMap<>();

	@Data
	@Builder
	public static class CueDataFileData {
		private String file;
		@Builder.Default
		private List<CueDataTrackData> tracks = new ArrayList<>();
	}

	@Data
	@Builder
	public static class CueDataTrackData {
		private String number;
		private String type;
		private String title;
		private String performer;
		private String songwriter;
		@Builder.Default
		private Map<String, String> rems = new HashMap<>();
		@Builder.Default
		private List<CueDataTrackIndex> indexes = new ArrayList<>();
	}

	@Data
	@Builder
	public static class CueDataTrackIndex {
		private Integer indexNumber;
		private String indexValue;
	}
}
