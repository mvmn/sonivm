package x.mvmn.sonivm;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface WinAmpSkinsService {

	Set<String> listSkins();

	File getSkinFile(String fileName);

	void importSkin(File skinFile) throws IOException;

}