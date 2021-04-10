
package app.crossword.yourealwaysbe;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.net.Scrapers;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.SingleLiveEvent;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.util.files.PuzMetaFile;

import java.io.IOException;
import java.lang.Void;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BrowseActivityViewModel extends ViewModel {
    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivityViewModel.class.getCanonicalName());

    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    // not fixed num in case user creates loads of downloads
    private ExecutorService downloadExecutorService
        = Executors.newCachedThreadPool();
    private Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private boolean isViewArchive = false;

    private MutableLiveData<List<PuzMetaFile>> puzzleFiles
        = new MutableLiveData<>();
    // busy with something that isn't downloading
    private MutableLiveData<Boolean> isUIBusy
        = new MutableLiveData<Boolean>();
    private SingleLiveEvent<Void> puzzleLoadEvents
        = new SingleLiveEvent<>();

    public BrowseActivityViewModel() {
        isUIBusy.setValue(false);
        prefs = PreferenceManager.getDefaultSharedPreferences(
            ForkyzApplication.getInstance()
        );
    }

    public MutableLiveData<List<PuzMetaFile>> getPuzzleFiles() {
        return puzzleFiles;
    }

    public MutableLiveData<Boolean> getIsUIBusy() {
        return isUIBusy;
    }

    public SingleLiveEvent<Void> getPuzzleLoadEvents() {
        return puzzleLoadEvents;
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

            List<PuzMetaFile> puzFiles
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

    public void cleanUpPuzzles() {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();

            boolean deleteOnCleanup
                = prefs.getBoolean("deleteOnCleanup", false);
            LocalDate maxAge
                = getMaxAge(prefs.getString("cleanupAge", "2"));
            LocalDate archiveMaxAge
                = getMaxAge(prefs.getString("archiveCleanupAge", "-1"));

            DirHandle crosswords
                = fileHandler.getCrosswordsDirectory();
            DirHandle archive
                = fileHandler.getArchiveDirectory();

            List<PuzMetaFile> toArchive = new ArrayList<>();
            List<PuzMetaFile> toDelete = new ArrayList<>();

            if (maxAge != null) {
                List<PuzMetaFile> puzFiles
                    = fileHandler.getPuzFiles(crosswords);
                Collections.sort(puzFiles);
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
                List<PuzMetaFile> puzFiles
                    = fileHandler.getPuzFiles(archive);
                Collections.sort(puzFiles);
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

    public void download(
        LocalDate date, List<Downloader> downloaders, boolean scrape
    ) {
        downloadExecutorService.execute(() -> {
            NotificationManager nm
                = (NotificationManager)
                    ForkyzApplication
                        .getInstance()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

            Downloaders dls = new Downloaders(
                prefs, nm, ForkyzApplication.getInstance()
            );

            dls.download(date, downloaders);

            if (scrape) {
                Scrapers scrapes = new Scrapers(
                    prefs, nm, ForkyzApplication.getInstance()
                );
                scrapes.scrape();
            }

            handler.post(() -> {
                startLoadFiles();
            });
        });
    }

    public void loadPuzzle(PuzMetaFile puzMeta) {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();
            try {
                Puzzle puz = fileHandler.load(puzMeta);
                if (puz == null || puz.getBoxes() == null) {
                    throw new IOException(
                        "Puzzle is null or contains no boxes."
                    );
                }
                handler.post(() -> {
                    isUIBusy.setValue(false);
                    ForkyzApplication application
                        = ForkyzApplication.getInstance();
                    application.setBoard(
                        new Playboard(
                            puz,
                            application.getMovementStrategy(),
                            prefs.getBoolean(
                                "preserveCorrectLettersInShowErrors", false
                            ),
                            prefs.getBoolean("dontDeleteCrossing", true)
                        ),
                        puzMeta.getPuzHandle()
                    );
                    puzzleLoadEvents.call();
                });
            } catch (IOException e) {
                isUIBusy.setValue(false);
                handler.post(() -> {
                    String filename = null;
                    try {
                        filename = fileHandler.getName(
                            puzMeta.getPuzHandle().getPuzFileHandle()
                        );
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }

                    ForkyzApplication application
                        = ForkyzApplication.getInstance();

                    Toast t = Toast.makeText(
                        application,
                        application.getString(
                            R.string.unable_to_read_file,
                            (filename != null ?  filename : "")
                        ),
                        Toast.LENGTH_SHORT
                    );
                    t.show();
                });
            }
        });
    }

    public void refreshPuzzleMeta(PuzHandle refreshHandle) {
        if (isUIBusy.getValue())
            return;

        isUIBusy.setValue(true);

        executorService.execute(() -> {
            FileHandler fileHandler = getFileHandler();
            boolean changed = false;

            FileHandle refreshedPuzFileHandle
                = refreshHandle.getPuzFileHandle();

            try {
                PuzMetaFile refreshedMeta
                    = fileHandler.loadPuzMetaFile(refreshHandle);

                if (refreshedMeta != null) {
                    List<PuzMetaFile> pmList = puzzleFiles.getValue();

                    int index = -1;
                    for (PuzMetaFile pm : pmList) {
                        index += 1;
                        FileHandle pmPuzFileHandle
                            = pm.getPuzHandle().getPuzFileHandle();
                        if (pmPuzFileHandle.equals(refreshedPuzFileHandle)) {
                            pmList.set(index, refreshedMeta);
                            changed = true;
                        }
                    }

                    final boolean updateArray = changed;

                    handler.post(() -> {
                        isUIBusy.setValue(false);
                        if (updateArray)
                            puzzleFiles.setValue(pmList);
                    });
                }
            } catch (IOException e) {
                LOGGER.warning("Could not refresh puz meta " + e);
            }
        });
    }

    private LocalDate getMaxAge(String preferenceValue) {
        int cleanupValue = Integer.parseInt(preferenceValue) + 1;
        if (cleanupValue > 0)
            return LocalDate.now().minus(Period.ofDays(cleanupValue));
        else
            return null;
    }

    private void setIsViewArchive(boolean isViewArchive) {
        this.isViewArchive = isViewArchive;
    }

    @Override
    protected void onCleared() {
        executorService.shutdown();
        downloadExecutorService.shutdown();
    }

    private FileHandler getFileHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }
}
