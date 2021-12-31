package x.mvmn.sonivm.impl;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import x.mvmn.sonivm.WinAmpSkinsService;

@Service
public class WinAmpSkinsServiceImpl implements WinAmpSkinsService {

	protected volatile File skinsFolder;

	@PostConstruct
	protected void init() {
		File sonivmHomeFolder = new File(System.getProperty("sonivm_home_folder"));
		File skinsFolder = new File(sonivmHomeFolder, "winamp_skins");
		if (!skinsFolder.exists()) {
			skinsFolder.mkdirs();
		}
		this.skinsFolder = skinsFolder;
	}

	@Override
	public Set<String> listSkins() {
		return Stream.of(this.skinsFolder)
				.map(File::getName)
				.filter(fn -> fn.toLowerCase().endsWith(".wsz"))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public File getSkinFile(String fileName) {
		return new File(skinsFolder, fileName);
	}

	@Override
	public void importSkin(File skinFile) throws IOException {
		FileUtils.copyFile(skinFile, new File(skinsFolder, skinFile.getName()));
	}
}
