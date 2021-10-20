package x.mvmn.sonivm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import lombok.experimental.UtilityClass;
import x.mvmn.sonivm.eq.model.EqualizerPreset;

@UtilityClass
public class WinAmpEqualizerFileUtil {

	public static final String HEADER_OF_WINAMP_EQ_LIBRARY_FILE_V1_1 = "Winamp EQ library file v1.1";

	public static Tuple2<String, EqualizerPreset> fromEQF(byte[] eqfFileContent) {
		String heading = new String(eqfFileContent, 0, HEADER_OF_WINAMP_EQ_LIBRARY_FILE_V1_1.getBytes(StandardCharsets.US_ASCII).length,
				StandardCharsets.US_ASCII);
		if (heading.equals(HEADER_OF_WINAMP_EQ_LIBRARY_FILE_V1_1)) {
			int endOfNameIndex = 32;
			while (endOfNameIndex < 288 && eqfFileContent[endOfNameIndex] != 0) {
				endOfNameIndex++;
			}
			String name = new String(eqfFileContent, 31, endOfNameIndex - 31, StandardCharsets.US_ASCII);
			int gain = fromWinAmpEQF(eqfFileContent[298]);
			int[] bands = new int[10];
			for (int bandIndex = 0; bandIndex < 10; bandIndex++) {
				bands[bandIndex] = fromWinAmpEQF(eqfFileContent[288 + bandIndex]);
			}
			return Tuple2.<String, EqualizerPreset> builder().a(name).b(EqualizerPreset.builder().gain(gain).bands(bands).build()).build();
		} else {
			throw new RuntimeException("Not a WinAmp v1.1 EQ library file - heading content is " + heading);
		}
	}

	public byte[] toEQF(EqualizerPreset preset, String name) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(HEADER_OF_WINAMP_EQ_LIBRARY_FILE_V1_1.getBytes(StandardCharsets.US_ASCII));
		baos.write(new byte[] { 0x1A, 0x21, 0x2D, 0x2D });

		byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
		if (nameBytes.length < 256) {
			nameBytes = Arrays.copyOf(nameBytes, 256);
		}
		baos.write(nameBytes, 0, 256);
		baos.write(0);
		for (int bandVal : preset.getBands()) {
			baos.write(toWinAmpEQF(bandVal));
		}
		baos.write(toWinAmpEQF(preset.getGain()));

		return baos.toByteArray();
	}

	private static int toWinAmpEQF(int eqValue) {
		// 0 - 63, 1000 - 0, 500 - 32
		return (1000 - eqValue) * 63 / 1000;
	}

	private static int fromWinAmpEQF(int eqfValue) {
		return (63 - eqfValue) * 1000 / 64;
	}

	// public static void main(String args[]) {
	// JFileChooser jfc = new JFileChooser();
	// jfc.setMultiSelectionEnabled(true);
	// jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	// if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
	// ObjectMapper om = new ObjectMapper();
	// Stream.of(jfc.getSelectedFiles())
	// .map(CallUtil.safe(f -> FileUtils.readFileToByteArray(f)))
	// .map(WinAmpEqualizerFileUtil::fromEQF)
	// .map(CallUtil.safe(v -> om.writerWithDefaultPrettyPrinter().writeValueAsString(v)))
	// .forEach(System.out::println);
	// }
	// }
}
