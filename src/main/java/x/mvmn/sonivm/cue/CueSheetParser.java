package x.mvmn.sonivm.cue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import x.mvmn.sonivm.cue.CueData.CueDataBuilder;
import x.mvmn.sonivm.cue.CueData.CueDataFileData;
import x.mvmn.sonivm.cue.CueData.CueDataFileData.CueDataFileDataBuilder;
import x.mvmn.sonivm.cue.CueData.CueDataTrackData;
import x.mvmn.sonivm.cue.CueData.CueDataTrackData.CueDataTrackDataBuilder;
import x.mvmn.sonivm.cue.CueData.CueDataTrackIndex;

public class CueSheetParser {

	private static final Logger LOGGER = Logger.getLogger(CueSheetParser.class.getCanonicalName());

	private static final Set<String> IGNORED_METADATA = Set.of("ISRC", "CATALOG", "FLAGS", "CDTEXTFILE", "PREGAP", "POSTGAP");

	private static enum ParserState {
		TOPLEVEL, FILE, TRACK
	}

	public static CueData parseCueFile(File file) {
		if (file != null && file.exists() && !file.isDirectory()) {
			try {
				String cueFileContents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
				return parse(cueFileContents, file.getAbsolutePath());
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Failed to read CUE file " + file, e);
				return null;
			}
		} else {
			return null;
		}
	}

	public static CueData parse(String cueFileContents, String fileName) {
		CueDataBuilder cueDataBuilder = CueData.builder();
		CueDataTrackDataBuilder trackBuilder = null;
		CueDataFileDataBuilder fileDataBuilder = null;
		List<CueDataFileData> files = new ArrayList<>();
		ParserState state = ParserState.TOPLEVEL;

		List<CueDataTrackData> tracks = new ArrayList<>();
		List<CueDataTrackIndex> trackIndexes = null;
		Map<String, String> sheetRems = new HashMap<>();
		Map<String, String> trackRems = new HashMap<>();

		String[] lines = cueFileContents.split("[\r\n]+");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.trim().isEmpty()) {
				continue;
			}
			String firstWord = line;
			String reminder = "";
			int indexOfSpace = line.indexOf(" ");
			if (indexOfSpace > 0) {
				firstWord = line.substring(0, indexOfSpace);
				reminder = line.substring(indexOfSpace + 1).trim();
			}
			firstWord.toUpperCase();
			if (IGNORED_METADATA.contains(firstWord)) {
				continue;
			}
			if ("TITLE".equals(firstWord)) {
				List<String> values = toIndividualParams(reminder);
				if (!values.isEmpty()) {
					if (ParserState.TRACK == state) {
						trackBuilder.title(values.get(0));
					} else {
						cueDataBuilder.title(values.get(0));
					}
				}
			} else if ("FILE".equals(firstWord)) {
				if (fileDataBuilder != null) {
					if (trackBuilder != null) {
						// Complete current track
						if (trackIndexes != null) {
							trackBuilder.indexes(trackIndexes);
						}
						tracks.add(trackBuilder.rems(trackRems).build());
						trackRems = new HashMap<>();
					}

					fileDataBuilder.tracks(tracks);
					files.add(fileDataBuilder.build());
					tracks = new ArrayList<>();
				}
				state = ParserState.FILE;
				fileDataBuilder = CueDataFileData.builder();
				List<String> params = toIndividualParams(reminder);
				if (params.size() > 0) {
					fileDataBuilder.file(params.get(0));
					if (params.size() > 1) {
						fileDataBuilder.fileAudioType(params.get(1));
					}
				} else {
					LOGGER.fine("Missing parameter to a file meta of cue file " + fileName + " line " + i);
				}
			} else if ("TRACK".equals(firstWord)) {
				if (ParserState.TRACK == state) {
					// Complete current track
					if (trackIndexes != null) {
						trackBuilder.indexes(trackIndexes);
					}
					tracks.add(trackBuilder.rems(trackRems).build());
					trackRems = new HashMap<>();
				} else if (ParserState.TOPLEVEL == state) {
					LOGGER.fine("Unexpected track meta on top-level of cue file " + fileName + " line " + i);
					continue;
				}
				trackBuilder = CueDataTrackData.builder();
				List<String> params = toIndividualParams(reminder);
				if (params.size() > 0) {
					trackBuilder.number(params.get(0));
				}
				if (params.size() > 1) {
					trackBuilder.dataType(params.get(1));
				}
				state = ParserState.TRACK;
				trackIndexes = null;
			} else if ("PERFORMER".equals(firstWord)) {
				List<String> values = toIndividualParams(reminder);
				if (!values.isEmpty()) {
					if (ParserState.TRACK == state) {
						trackBuilder.performer(values.get(0));
					} else {
						cueDataBuilder.performer(values.get(0));
					}
				}
			} else if ("SONGWRITER".equals(firstWord)) {
				List<String> values = toIndividualParams(reminder);
				if (!values.isEmpty()) {
					if (ParserState.TRACK == state) {
						trackBuilder.songwriter(values.get(0));
					} else {
						cueDataBuilder.songwriter(values.get(0));
					}
				}
			} else if ("INDEX".equals(firstWord)) {
				if (ParserState.TRACK == state) {
					List<String> values = toIndividualParams(reminder);

					if (values.size() > 1) {
						if (trackIndexes == null) {
							trackIndexes = new ArrayList<>();
						}
						int indexNumber = trackIndexes.size();
						try {
							indexNumber = Integer.parseInt(values.get(0));
						} catch (NumberFormatException e) {
							LOGGER.fine("Failed to parse index number as in in cue file " + fileName + " line " + i + ", value: "
									+ values.get(0));
						}
						trackIndexes.add(CueDataTrackIndex.builder().indexNumber(indexNumber).indexValue(values.get(1)).build());
					} else {
						LOGGER.fine("Insufficient params to index in cue file " + fileName + " line " + i);
					}
				} else {
					LOGGER.fine("Unexpected index meta outside of track in cue file " + fileName + " line " + i);
					continue;
				}
			} else if ("REM".equals(firstWord)) {
				List<String> values = toIndividualParams(reminder);
				if (values.size() > 0) {
					String key = values.get(0).toUpperCase();
					String value = "";
					if (values.size() == 2) {
						value = values.get(1);
					} else if (values.size() > 2) {
						value = values.subList(1, values.size()).stream().collect(Collectors.joining(" "));
					}
					if (ParserState.TRACK == state) {
						trackRems.put(key, value);
					} else if (ParserState.TOPLEVEL == state) {
						sheetRems.put(key, value);
					} else {
						LOGGER.fine("Unexpected rem meta in file section in cue file " + fileName + " line " + i);
						continue;
					}
				} else {
					LOGGER.fine("Empty rem meta in cue file " + fileName + " line " + i);
					continue;
				}
			} else {
				System.err.println(firstWord);
			}
		}

		if (trackBuilder != null) {
			tracks.add(trackBuilder.rems(trackRems).indexes(trackIndexes).build());
		}

		if (fileDataBuilder != null) {
			fileDataBuilder.tracks(tracks);
			files.add(fileDataBuilder.build());
		}
		cueDataBuilder.fileData(files).rems(sheetRems);

		return cueDataBuilder.build();
	}

	private static List<String> toIndividualParams(String cueMetaParamStr) {
		if (cueMetaParamStr.isEmpty()) {
			return Collections.emptyList();
		} else {
			List<String> result = new ArrayList<>();

			boolean inQuotes = false;
			boolean afterSlash = false;
			StringBuilder currentParam = new StringBuilder();
			for (int i = 0; i < cueMetaParamStr.length(); i++) {
				char charAtIndex = cueMetaParamStr.charAt(i);
				if (Character.isWhitespace(charAtIndex) && !inQuotes) {
					result.add(currentParam.toString());
					currentParam.setLength(0);
					afterSlash = false;
				} else if ('"' == charAtIndex && !afterSlash) {
					if (!inQuotes) {
						inQuotes = true;
					} else {
						inQuotes = false;
					}
					afterSlash = false;
				} else if ('\\' == charAtIndex) {
					afterSlash = true;
				} else {
					afterSlash = false;
					currentParam.append(charAtIndex);
				}
			}
			if (currentParam.length() > 0) {
				result.add(currentParam.toString());
			}

			return result;
		}
	}
}
