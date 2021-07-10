
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
import app.crossword.yourealwaysbe.view.PlayboardRenderer;

public class PlayActivityViewModel extends ViewModel {
    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivityViewModel.class.getCanonicalName());

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    private MutableLiveData<Boolean> isUIBusy
        = new MutableLiveData<Boolean>();
    private MutableLiveData<Playboard> liveBoard
        = new MutableLiveData<Playboard>();

    private SharedPreferences prefs;
    private PuzHandle puzHandle;
    private PlayboardRenderer renderer;
    private ImaginaryTimer timer;

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            PlayActivityViewModel.this.onTimerUpdate();
        }
    };

    public PlayActivityViewModel() {
        isUIBusy.setValue(false);
        prefs = PreferenceManager.getDefaultSharedPreferences(
            ForkyzApplication.getInstance()
        );
    }

    public MutableLiveData<Playboard> getLiveBoard() { return liveBoard; }
    public Playboard getBoard() { return liveBoard.getValue(); }
    public PuzHandle getPuzHandle() { return puzHandle; }

    public Puzzle getPuzzle() {
        Playboard board = liveBoard.getValue();
        return board == null ? null : board.getPuzzle();
    }

    public ImaginaryTimer getTimer() {
        return timer;
    }

    // TODO: set puzzle time from timer!

    public void resumeTimer() {
        Puzzle puz = getPuzzle();
        if (timer != null && puz != null && puz.getPercentComplete() != 100) {
            timer = new ImaginaryTimer(puz.getTime());
            timer.start();
        }
    }

    public void pauseTimer() {
        if (timer != null) {
            timer.stop();
            Puzzle puz = getPuzzle();
            if (puz != null)
                puz.setTime(timer.getElapsed());
        }
    }

    /**
     * Save the puzzle
     *
     * Will block, but saving is quick, so probably safer to let it
     * block onPause.
     */
    public void savePuzzle() {
        if (puzHandle == null) {
            LOGGER.severe("No puz handle to save puzzle to.");
            return;
        }

        Puzzle puz = getPuzzle();
        if (puz == null) {
            LOGGER.severe("No puzzle to save.");
            return;
        }

        try {
            getFileHandler().save(puz, puzHandle);
        } catch (IOException e) {
            LOGGER.severe("Error saving puzzle.");
            e.printStackTrace();
        }
    }

    public PlayboardRenderer getRenderer() {
        return renderer;
    }

    public void loadPuzzle(PuzHandle puzHandle) {
        threadWithUILock(() -> {
            FileHandler fileHandler = getFileHandler();
            try {
                Puzzle puz = fileHandler.load(puzHandle);
                if (puz == null || puz.getBoxes() == null) {
                    throw new IOException(
                        "Puzzle is null or contains no boxes."
                    );
                }
                handler.post(() -> {
                    PlayActivityViewModel.this.onPuzzleLoaded(puzHandle, puz);
                });
            } catch (IOException e) {
                handler.post(() -> {
                    String filename = null;
                    try {
                        filename = fileHandler.getName(puzHandle);
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

    private void onPuzzleLoaded(PuzHandle puzHandle, Puzzle puz) {
        ForkyzApplication application
            = ForkyzApplication.getInstance();
        this.puzHandle = puzHandle;
        liveBoard.setValue(
            new Playboard(
                puz,
                application.getMovementStrategy(),
                prefs.getBoolean(
                    "preserveCorrectLettersInShowErrors", false
                ),
                prefs.getBoolean("dontDeleteCrossing", true)
            )
        );
    }

    private void createTimer() {
        Puzzle puz = getPuzzle();
        if (puz != null && puz.getPercentComplete() != 100) {
            timer = new ImaginaryTimer(puz.getTime());
            timer.start();

            if (prefs.getBoolean(PlayActivity.SHOW_TIMER, false)) {
                handler.post(updateTimeTask);
            }
        }
    }

    private void onTimerUpdate() {
        if (prefs.getBoolean(PlayActivity.SHOW_TIMER, false)) {
            handler.postDelayed(updateTimeTask, 1000);
        }
        // TODO: put this info into an observable?
        // atmo doing nothing
    }

    // TODO: do we want to repeat this logic?
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

    // TODO: put this somewhere
    // public void onPlayboardChange(
    //     boolean wholeBoard, Word currentWord, Word previousWord
    // ) {
    //     Puzzle puz = getPuzzle();
    //     ImaginaryTimer timer = getTimer();

    //     if (puz != null &&
    //         puz.getPercentComplete() == 100 &&
    //         timer != null) {

    //         timer.stop();
    //         puz.setTime(timer.getElapsed());
    //         setTimer(null);
    //         Intent i = new Intent(PuzzleActivity.this,
    //                               PuzzleFinishedActivity.class);
    //         this.startActivity(i);
    //     }
    // }
}
