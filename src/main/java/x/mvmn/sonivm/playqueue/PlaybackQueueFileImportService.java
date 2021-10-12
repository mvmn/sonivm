package x.mvmn.sonivm.playqueue;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface PlaybackQueueFileImportService {

	void importFilesIntoPlayQueue(int queuePosition, List<File> filesToImport, Consumer<String> importProgressListener);

}