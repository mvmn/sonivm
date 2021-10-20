package x.mvmn.sonivm.eq.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.util.Tuple2;

public interface EqualizerPresetService {

	Collection<String> listPresets();

	void savePreset(String name, EqualizerPreset preset) throws IOException;

	EqualizerPreset loadPreset(String name) throws JsonParseException, JsonMappingException, IOException;

	Tuple2<String, EqualizerPreset> importWinAmpEqfPreset(File eqfFile);

	void exportWinAmpEqfPreset(String name, EqualizerPreset preset, File targetFolder);
}