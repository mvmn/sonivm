package x.mvmn.sonivm.playqueue;

import java.io.File;
import java.util.List;

public interface PlaybackQueueFileImportService {

	void importFilesIntoPlayQueue(int queuePosition, List<File> filesToImport);

}