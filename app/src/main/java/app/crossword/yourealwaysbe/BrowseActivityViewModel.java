
package app.crossword.yourealwaysbe;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.PuzMetaFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    // busy with something that isn't downloading
    private MutableLiveData<Boolean> isUIBusy
        = new MutableLiveData<Boolean>();

    public BrowseActivityViewModel() {
        isUIBusy.setValue(false);
    }

    public MutableLiveData<PuzMetaFile[]> getPuzzleFiles() {
        return puzzleFiles;
    }

    public MutableLiveData<Boolean> getIsUIBusy() {
        return isUIBusy;
    }

    public boolean getIsViewArchive() {
        return isViewArchive;
    }

    public void startLoadFiles() {
        startLoadFiles(getIsViewArchive());
    }

    public void startLoadFiles(boolean archive) {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();

            DirHandle directory
                = archive
                    ? fileHandler.getArchiveDirectory()
                    : fileHandler.getCrosswordsDirectory();

            PuzMetaFile[] puzFiles
                = fileHandler.getPuzFiles(directory);

            // use handler for this so viewArchive changes when
            // puzzleFiles does
            handler.post(() -> {
                setIsViewArchive(archive);
                puzzleFiles.setValue(puzFiles);
                isUIBusy.setValue(false);
            });
        });
    }

    public void deletePuzzle(PuzMetaFile puzMeta) {
        deletePuzzles(Collections.singleton(puzMeta));
    }

    public void deletePuzzles(Collection<PuzMetaFile> puzMetas) {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();

            for (PuzMetaFile puzMeta : puzMetas)
                fileHandler.delete(puzMeta);

            handler.post(() -> {
                isUIBusy.setValue(false);
                startLoadFiles();
            });
        });
    }

    public void movePuzzle(
        PuzMetaFile puzMeta, DirHandle srcDir, DirHandle destDir
    ) {
        movePuzzles(Collections.singleton(puzMeta), srcDir, destDir);
    }

    public void movePuzzles(
        Collection<PuzMetaFile> puzMetas, DirHandle srcDir, DirHandle destDir
    ) {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();

            for (PuzMetaFile puzMeta : puzMetas)
                fileHandler.moveTo(puzMeta, srcDir, destDir);

            handler.post(() -> {
                isUIBusy.setValue(false);
                startLoadFiles();
            });
        });
    }

    public void cleanUpPuzzles(
        boolean deleteOnCleanup, LocalDate maxAge, LocalDate archiveMaxAge
    ) {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();

            DirHandle crosswords
                = fileHandler.getCrosswordsDirectory();
            DirHandle archive
                = fileHandler.getArchiveDirectory();

            List<PuzMetaFile> toArchive = new ArrayList<>();
            List<PuzMetaFile> toDelete = new ArrayList<>();

            if (maxAge != null) {
                PuzMetaFile[] puzFiles
                    = fileHandler.getPuzFiles(crosswords);
                Arrays.sort(puzFiles);
                for (PuzMetaFile pm : puzFiles) {
                    if ((pm.getComplete() == 100)
                            || (pm.getDate().isBefore(maxAge))) {
                        if (deleteOnCleanup) {
                            toDelete.add(pm);
                        } else {
                            toArchive.add(pm);
                        }
                    }
                }
            }

            if (archiveMaxAge != null) {
                PuzMetaFile[] puzFiles
                    = fileHandler.getPuzFiles(archive);
                Arrays.sort(puzFiles);
                for (PuzMetaFile pm : puzFiles) {
                    if (pm.getDate().isBefore(archiveMaxAge)) {
                        toDelete.add(pm);
                    }
                }
            }

            for (PuzMetaFile puzMeta : toDelete)
                fileHandler.delete(puzMeta);

            for (PuzMetaFile puzMeta : toArchive)
                fileHandler.moveTo(puzMeta, crosswords, archive);

            handler.post(() -> {
                isUIBusy.setValue(false);
                startLoadFiles();
            });
        });
    }


    private void setIsViewArchive(boolean isViewArchive) {
        this.isViewArchive = isViewArchive;
    }

    @Override
    protected void onCleared() {
        executorService.shutdown();
    }

    private FileHandler getFileHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }
}
