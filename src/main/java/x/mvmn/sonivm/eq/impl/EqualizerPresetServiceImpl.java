package x.mvmn.sonivm.eq.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import x.mvmn.sonivm.eq.model.EqualizerPreset;

@Service
public class EqualizerPresetServiceImpl implements EqualizerPresetService {

	private static final Map<String, EqualizerPreset> DEFAULT_PRESETS;
	static {
		Map<String, EqualizerPreset> defaultPresets = new HashMap<>();
		defaultPresets.put("* Normal *",
				EqualizerPreset.builder().bands(new int[] { 500, 500, 500, 500, 500, 500, 500, 500, 500, 500 }).gain(300).build());
		defaultPresets.put("Classical",
				EqualizerPreset.builder().bands(new int[] { 500, 500, 500, 500, 500, 500, 700, 700, 700, 760 }).gain(300).build());
		defaultPresets.put("Club",
				EqualizerPreset.builder().bands(new int[] { 500, 500, 420, 340, 340, 340, 420, 500, 500, 500 }).gain(300).build());
		defaultPresets.put("Dance",
				EqualizerPreset.builder().bands(new int[] { 260, 340, 460, 500, 500, 660, 700, 700, 500, 500 }).gain(300).build());
		defaultPresets.put("Full Bass",
				EqualizerPreset.builder().bands(new int[] { 260, 260, 260, 360, 460, 620, 760, 780, 780, 780 }).gain(300).build());
		defaultPresets.put("Full Bass Treble",
				EqualizerPreset.builder().bands(new int[] { 340, 340, 500, 680, 620, 460, 280, 220, 180, 180 }).gain(300).build());
		defaultPresets.put("Full Treble",
				EqualizerPreset.builder().bands(new int[] { 780, 780, 780, 620, 420, 240, 80, 80, 80, 80 }).gain(300).build());
		defaultPresets.put("Laptop",
				EqualizerPreset.builder().bands(new int[] { 380, 220, 360, 600, 580, 460, 380, 240, 160, 140 }).gain(300).build());
		defaultPresets.put("Live",
				EqualizerPreset.builder().bands(new int[] { 660, 500, 400, 360, 340, 340, 400, 420, 420, 420 }).gain(300).build());
		defaultPresets.put("Party",
				EqualizerPreset.builder().bands(new int[] { 320, 320, 500, 500, 500, 500, 500, 500, 320, 320 }).gain(300).build());
		defaultPresets.put("Pop",
				EqualizerPreset.builder().bands(new int[] { 560, 380, 320, 300, 380, 540, 560, 560, 540, 540 }).gain(300).build());
		defaultPresets.put("Reggae",
				EqualizerPreset.builder().bands(new int[] { 480, 480, 500, 660, 480, 340, 340, 480, 480, 480 }).gain(300).build());
		defaultPresets.put("Rock",
				EqualizerPreset.builder().bands(new int[] { 320, 380, 640, 720, 560, 400, 280, 240, 240, 240 }).gain(300).build());
		defaultPresets.put("Techno",
				EqualizerPreset.builder().bands(new int[] { 300, 340, 480, 660, 640, 480, 300, 240, 240, 280 }).gain(300).build());
		DEFAULT_PRESETS = Collections.unmodifiableMap(defaultPresets);
	}

	private File presetsFolder;

	private ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	protected void init() {
		File sonivmHomeFolder = new File(System.getProperty("sonivm_home_folder"));
		File equalizerPresetsFolder = new File(sonivmHomeFolder, "eqp_resets");
		if (!equalizerPresetsFolder.exists()) {
			equalizerPresetsFolder.mkdirs();
		}
		this.presetsFolder = equalizerPresetsFolder;
	}

	@Override
	public Collection<String> listPresets() {
		return Stream
				.concat(DEFAULT_PRESETS.keySet().stream(),
						Stream.of(presetsFolder.listFiles())
								.map(File::getName)
								.filter(fileName -> fileName.toLowerCase().endsWith(".snvmeq"))
								.map(FilenameUtils::getBaseName))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public void savePreset(String name, EqualizerPreset preset) throws IOException {
		String fileName = FilenameUtils.normalize(name).replaceAll(File.separator, "_").replaceAll(File.pathSeparator, "_") + ".snvmeq";
		FileUtils.writeByteArrayToFile(new File(presetsFolder, fileName), objectMapper.writeValueAsBytes(preset), false);
	}

	@Override
	public EqualizerPreset loadPreset(String name) throws JsonParseException, JsonMappingException, IOException {
		if (DEFAULT_PRESETS.containsKey(name)) {
			return DEFAULT_PRESETS.get(name);
		}
		File presetFile = new File(presetsFolder, name + ".snvmeq");
		if (!presetFile.exists()) {
			return null;
		}
		return objectMapper.readValue(FileUtils.readFileToByteArray(presetFile), EqualizerPreset.class);
	}

	@Override
	public EqualizerPreset importWinAmpEqfPreset(File eqfFile) {
		return null;
	}

	@Override
	public void exportWinAmpEqfPreset(String name, EqualizerPreset preset, File targetFolder) {

	}
}
