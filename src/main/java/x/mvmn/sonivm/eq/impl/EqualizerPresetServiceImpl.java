package x.mvmn.sonivm.eq.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import x.mvmn.sonivm.eq.EqualizerPresetService;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.util.Tuple2;
import x.mvmn.sonivm.util.WinAmpEqualizerFileUtil;

@Service
public class EqualizerPresetServiceImpl implements EqualizerPresetService {

	private static final Map<String, EqualizerPreset> DEFAULT_PRESETS;
	static {
		Map<String, EqualizerPreset> defaultPresets = new HashMap<>();
		defaultPresets.put("* Flat *", EqualizerPreset.of(500, invert(new int[] { 500, 500, 500, 500, 500, 500, 500, 500, 500, 500 })));
		defaultPresets.put("- Classical -",
				EqualizerPreset.of(500, invert(new int[] { 500, 500, 500, 500, 500, 500, 700, 700, 700, 760 })));
		defaultPresets.put("- Club -", EqualizerPreset.of(500, invert(new int[] { 500, 500, 420, 340, 340, 340, 420, 500, 500, 500 })));
		defaultPresets.put("- Dance -", EqualizerPreset.of(500, invert(new int[] { 260, 340, 460, 500, 500, 660, 700, 700, 500, 500 })));
		defaultPresets.put("- Full Bass -",
				EqualizerPreset.of(500, invert(new int[] { 260, 260, 260, 360, 460, 620, 760, 780, 780, 780 })));
		defaultPresets.put("- Full Bass & Treble -",
				EqualizerPreset.of(500, invert(new int[] { 340, 340, 500, 680, 620, 460, 280, 220, 180, 180 })));
		defaultPresets.put("- Full Treble -", EqualizerPreset.of(500, invert(new int[] { 780, 780, 780, 620, 420, 240, 80, 80, 80, 80 })));
		defaultPresets.put("- Laptop -", EqualizerPreset.of(500, invert(new int[] { 380, 220, 360, 600, 580, 460, 380, 240, 160, 140 })));
		defaultPresets.put("- Large hall -", EqualizerPreset.of(500, new int[] { 750, 750, 640, 640, 500, 359, 359, 359, 500, 500 }));
		defaultPresets.put("- Live -", EqualizerPreset.of(500, invert(new int[] { 660, 500, 400, 360, 340, 340, 400, 420, 420, 420 })));
		defaultPresets.put("- Party -", EqualizerPreset.of(500, invert(new int[] { 320, 320, 500, 500, 500, 500, 500, 500, 320, 320 })));
		defaultPresets.put("- Pop -", EqualizerPreset.of(500, invert(new int[] { 560, 380, 320, 300, 380, 540, 560, 560, 540, 540 })));
		defaultPresets.put("- Reggae -", EqualizerPreset.of(500, invert(new int[] { 480, 480, 500, 660, 480, 340, 340, 480, 480, 480 })));
		defaultPresets.put("- Rock -", EqualizerPreset.of(500, invert(new int[] { 320, 380, 640, 720, 560, 400, 280, 240, 240, 240 })));
		defaultPresets.put("- Soft rock -", EqualizerPreset.of(500, new int[] { 593, 593, 546, 500, 375, 343, 390, 500, 562, 718 }));
		defaultPresets.put("- Ska -", EqualizerPreset.of(500, new int[] { 421, 359, 375, 500, 593, 640, 718, 734, 765, 734 }));
		defaultPresets.put("- Techno -", EqualizerPreset.of(500, invert(new int[] { 300, 340, 480, 660, 640, 480, 300, 240, 240, 280 })));
		DEFAULT_PRESETS = Collections.unmodifiableMap(defaultPresets);
	}

	private File presetsFolder;

	private ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	protected void init() {
		File sonivmHomeFolder = new File(System.getProperty("sonivm_home_folder"));
		File equalizerPresetsFolder = new File(sonivmHomeFolder, "eq_presets");
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
	public Tuple2<String, EqualizerPreset> importWinAmpEqfPreset(InputStream eqfInputStream) {
		try {
			byte[] content = IOUtils.toByteArray(eqfInputStream);
			return WinAmpEqualizerFileUtil.fromEQF(content);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void exportWinAmpEqfPreset(String name, EqualizerPreset preset, OutputStream eqfOutputStream) {
		try {
			byte[] eqfContent = WinAmpEqualizerFileUtil.toEQF(preset, name);
			IOUtils.write(eqfContent, eqfOutputStream);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static int[] invert(int[] eqBands) {
		for (int i = 0; i < eqBands.length; i++) {
			eqBands[i] = 1000 - eqBands[i];
		}
		return eqBands;
	}
}
