
package app.crossword.yourealwaysbe;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.PuzMetaFile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BrowseActivityViewModel extends ViewModel {
    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivityViewModel.class.getCanonicalName());

    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    private boolean isViewArchive = false;

    private MutableLiveData<PuzMetaFile[]> puzzleFiles
        = new MutableLiveData<PuzMetaFile[]>();

    public BrowseActivityViewModel() {
        // do nothing
    }

    public MutableLiveData<PuzMetaFile[]> getPuzzleFiles() {
        return puzzleFiles;
    }

    public boolean getIsViewArchive() {
        return isViewArchive;
    }

    public void startLoadFiles(boolean archive, String sourceMatch) {
        executorService.execute(() -> {
            final FileHandler fileHandler
                = ForkyzApplication.getInstance().getFileHandler();

            final DirHandle directory
                = archive
                    ? fileHandler.getArchiveDirectory()
                    : fileHandler.getCrosswordsDirectory();

            PuzMetaFile[] puzFiles
                = fileHandler.getPuzFiles(directory, sourceMatch);

            // use handler for this so viewArchive changes when
            // puzzleFiles does
            if (!executorService.isShutdown()) {
                handler.post(() -> {
                    if (!executorService.isShutdown()) {
                        setIsViewArchive(archive);
                        puzzleFiles.setValue(puzFiles);
                    }
                });
            }
        });
    }

    private void setIsViewArchive(boolean isViewArchive) {
        this.isViewArchive = isViewArchive;
    }

    @Override
    protected void onCleared() {
        executorService.shutdown();
    }
}
