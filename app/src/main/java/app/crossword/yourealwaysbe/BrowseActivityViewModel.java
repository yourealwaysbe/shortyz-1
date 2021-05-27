
package app.crossword.yourealwaysbe;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
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
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.util.files.PuzMetaFile;

public class BrowseActivityViewModel extends ViewModel {
    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivityViewModel.class.getCanonicalName());

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    // not fixed num in case user creates loads of downloads
    private ExecutorService downloadExecutorService
        = Executors.newCachedThreadPool();
    private Handler handler = new Handler(Looper.getMainLooper());

    private SharedPreferences prefs;

    private boolean isViewArchive = false;

    private MutableLiveData<List<MutableLiveData<PuzMetaFile>>> puzzleFiles
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

    /**
     * Get list of puzzle files in currently viewed directory
     *
     * List of mutable live data. Each live data is for one puzzle --
     * the base puzzle may change, but it might get updated with new
     * meta data (e.g. new % completed). If it gets set to null, it
     * means the puzzle was removed from the puzzle list.
     *
     * The puzzle list might have items added to it, a new value will be
     * posted, containing the same list with the new item at the end.
     */
    public MutableLiveData<List<MutableLiveData<PuzMetaFile>>>
    getPuzzleFiles() {
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
        threadWithUILock(() -> {
            FileHandler fileHandler = getFileHandler();

            DirHandle directory = archive
                ? fileHandler.getArchiveDirectory()
                : fileHandler.getCrosswordsDirectory();

            List<MutableLiveData<PuzMetaFile>> puzFiles
                = new ArrayList<>();

            for (PuzMetaFile pm : fileHandler.getPuzMetas(directory)) {
                puzFiles.add(new MutableLiveData<>(pm));
            }

            // use handler for this so viewArchive changes when
            // puzzleFiles does
            handler.post(() -> {
                setIsViewArchive(archive);
                puzzleFiles.setValue(puzFiles);
            });
        });
    }

    public void deletePuzzle(PuzMetaFile puzMeta) {
        deletePuzzles(Collections.singleton(puzMeta));
    }

    public void deletePuzzles(Collection<PuzMetaFile> puzMetas) {
        threadWithUILock(() -> {
            FileHandler fileHandler = getFileHandler();

            DirHandle viewedDir = getViewedDirectory();

            for (PuzMetaFile puzMeta : puzMetas) {
                fileHandler.delete(puzMeta);

                if (puzMeta.isInDirectory(viewedDir))
                    removeFromPuzzleList(puzMeta);
            }
        });
    }

    public void movePuzzle(PuzMetaFile puzMeta, DirHandle destDir) {
        movePuzzles(Collections.singleton(puzMeta), destDir);
    }

    public void movePuzzles(
        Collection<PuzMetaFile> puzMetas, DirHandle destDir
    ) {
        threadWithUILock(() -> {
            DirHandle directory = getViewedDirectory();
            FileHandler fileHandler = getFileHandler();

            for (PuzMetaFile puzMeta : puzMetas) {
                boolean addToList = destDir.equals(directory);
                boolean removeFromList = puzMeta.isInDirectory(directory);

                fileHandler.moveTo(puzMeta, destDir);

                if (addToList && !removeFromList)
                    addPuzzleToList(puzMeta);
                else if (removeFromList && !addToList)
                    removeFromPuzzleList(puzMeta);
            }
        });
    }

    public void cleanUpPuzzles() {
        threadWithUILock(() -> {
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
                    = fileHandler.getPuzMetas(crosswords);
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
                    = fileHandler.getPuzMetas(archive);
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
                fileHandler.moveTo(puzMeta, archive);

            startLoadFiles();
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

            if (!getIsViewArchive()) {
                handler.post(() -> {
                    startLoadFiles();
                });
            }
        });
    }

    public void loadPuzzle(PuzMetaFile puzMeta) {
        threadWithUILock(() -> {
            FileHandler fileHandler = getFileHandler();
            try {
                Puzzle puz = fileHandler.load(puzMeta);
                if (puz == null || puz.getBoxes() == null) {
                    throw new IOException(
                        "Puzzle is null or contains no boxes."
                    );
                }
                handler.post(() -> {
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
                handler.post(() -> {
                    String filename = null;
                    try {
                        filename = fileHandler.getName(puzMeta.getPuzHandle());
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
        threadWithUILock(() -> {
            FileHandler fileHandler = getFileHandler();

            try {
                PuzMetaFile refreshedMeta
                    = fileHandler.loadPuzMetaFile(refreshHandle);

                if (refreshedMeta == null)
                    return;

                if (refreshedMeta != null) {
                    List<MutableLiveData<PuzMetaFile>> pmList
                        = puzzleFiles.getValue();

                    int index = -1;
                    for (MutableLiveData<PuzMetaFile> pm : pmList) {
                        index += 1;
                        if (pm.getValue().isSameMainFile(refreshHandle)) {
                            pm.postValue(refreshedMeta);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warning("Could not refresh puz meta " + e);
            }
        });
    }

    /**
     * Import file from uri to crosswords folder
     *
     * Triggers refresh of puzzle list if crosswords folder is currently
     * shown in the case that the import succeeds or forceReload is true.
     */
    public void importURI(Uri uri, boolean forceReload) {
        threadWithUILock(() -> {
            ForkyzApplication application = ForkyzApplication.getInstance();
            ContentResolver resolver = application.getContentResolver();

            final PuzHandle ph = PuzzleImporter.importUri(resolver, uri);

            if (!getIsViewArchive()) {
                if (forceReload) {
                    startLoadFiles();
                } else if (ph != null) {
                    try {
                        PuzMetaFile pm = getFileHandler().loadPuzMetaFile(ph);
                        addPuzzleToList(pm);
                    } catch (IOException e) {
                        // fall back to full reload
                        startLoadFiles();
                    }
                }
            }

            handler.post(() -> {
                String msg = application.getString(
                    ph != null ? R.string.import_success : R.string.import_failure
                );
                Toast t = Toast.makeText(application, msg, Toast.LENGTH_SHORT);
                t.show();
            });
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

    /**
     * crosswords if not viewing archive, else archive
     */
    private DirHandle getViewedDirectory() {
        FileHandler fileHandler = getFileHandler();
        return getIsViewArchive()
            ? fileHandler.getArchiveDirectory()
            : fileHandler.getCrosswordsDirectory();
    }

    @Override
    protected void onCleared() {
        executorService.shutdown();
        downloadExecutorService.shutdown();
    }

    private FileHandler getFileHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }

    private void threadWithUILock(Runnable r) {
        // no lock actually needed because executorService is single
        // threaded guaranteed
        executorService.execute(() -> {
            try {
                isUIBusy.postValue(true);
                r.run();
            } finally {
                isUIBusy.postValue(false);
            }
        });
    }

    /**
     * Don't add the same file twice!
     */
    private void addPuzzleToList(PuzMetaFile puzMeta) {
        puzzleFiles.getValue().add(new MutableLiveData<>(puzMeta));
        puzzleFiles.postValue(puzzleFiles.getValue());
    }

    /**
     * Assumes files only appear once in list
     */
    private void removeFromPuzzleList(PuzMetaFile delPuzMeta) {
        List<MutableLiveData<PuzMetaFile>> puzList = puzzleFiles.getValue();

        if (puzList == null)
            return;

        int index = 0;
        int delIndex = -1;

        while (index < puzList.size() && delIndex < 0) {
            PuzMetaFile pm = puzList.get(index).getValue();

            if (pm.isSameMainFile(delPuzMeta))
                delIndex = index;

            index += 1;
        }

        if (delIndex >= 0) {
            puzList
                .remove(delIndex)
                .postValue(null);
        }
    }
}
