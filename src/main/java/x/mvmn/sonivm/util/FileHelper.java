package x.mvmn.sonivm.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileHelper {

	public static void appendToFile(File file, byte[] data) throws IOException {
		FileUtils.writeByteArrayToFile(file, data, true);
	}

	public static void appendToFile(File file, String string) throws IOException {
		FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8, true);
	}

	public static void overwriteFile(File file, byte[] data) throws IOException {
		FileUtils.writeByteArrayToFile(file, data, false);
	}

	public static void truncateFile(File file, long newSize) throws IOException {
		try (FileOutputStream outChan = new FileOutputStream(file, true)) {
			outChan.getChannel().truncate(newSize);
		}
	}

	public static byte[] readFile(File file) throws IOException {
		return FileUtils.readFileToByteArray(file);
	}
}
