package app.crossword.yourealwaysbe;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;

import java.io.File;
import java.io.IOException;

public class ClueListActivity extends ForkyzActivity
                              implements ClueTabs.ClueTabsListener,
                                         Playboard.PlayboardListener {
    private Configuration configuration;
    private File baseFile;
    private ImaginaryTimer timer;
    private KeyboardManager keyboardManager;
    private Puzzle puz;
    private ScrollingImageView imageView;
    private boolean useNativeKeyboard = false;
    private PlayboardRenderer renderer;
    private ClueTabs clueTabs;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.configuration = newConfig;
        keyboardManager.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item == null || item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getTitle().toString().equals("Notes")) {
            launchNotes();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        utils.holographic(this);
        utils.finishOnHomeButton(this);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.renderer = new PlayboardRenderer(getBoard(), metrics.densityDpi, metrics.widthPixels,
                !prefs.getBoolean("supressHints", false),
                ContextCompat.getColor(this, R.color.boxColor), ContextCompat.getColor(this, R.color.blankColor),
                ContextCompat.getColor(this, R.color.errorColor));

        scaleRendererToCurWord();

        try {
            this.configuration = getBaseContext().getResources()
                    .getConfiguration();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to read device configuration.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
        if(ForkyzApplication.getInstance().getBoard() == null || ForkyzApplication.getInstance().getBoard().getPuzzle() == null){
            finish();
        }
        this.timer = new ImaginaryTimer(
                ForkyzApplication.getInstance().getBoard().getPuzzle().getTime());

        Uri u = this.getIntent().getData();

        if (u != null) {
            if (u.getScheme().equals("file")) {
                baseFile = new File(u.getPath());
            }
        }

        puz = ForkyzApplication.getInstance().getBoard().getPuzzle();
        timer.start();
        setContentView(R.layout.clue_list);

        keyboardManager
            = new KeyboardManager(this, (KeyboardView) findViewById(R.id.clueKeyboard));

        this.imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);

        this.imageView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                onTap(e);
                launchNotes();
            }

            public void onTap(Point e) {
                Playboard board = getBoard();

                if (board == null)
                    return;

                Word current = board.getCurrentWord();
                boolean across = board.isAcross();

                int newAcross = current.start.across;
                int newDown = current.start.down;
                int box = renderer.findBox(e).across;

                if (box < current.length) {
                    if (across) {
                        newAcross += box;
                    } else {
                        newDown += box;
                    }
                }

                Position newPos = new Position(newAcross, newDown);

                if (!newPos.equals(board.getHighlightLetter())) {
                    board.setHighlightLetter(newPos);
                }

                displayKeyboard();
            }
        });

        this.clueTabs = this.findViewById(R.id.clueListClueTabs);
        this.clueTabs.setBoard(getBoard());

        this.render();
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardManager.onResume((KeyboardView) findViewById(R.id.clueKeyboard));

        Playboard board = getBoard();
        if (board != null)
            board.addListener(this);

        if (clueTabs != null)
            clueTabs.addListener(this);

        this.render();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Notes").setIcon(android.R.drawable.ic_menu_agenda);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onPlayboardChange(Word currentWord, Word previousWord) {
        this.render();
    }

    @Override
    public void onClueTabsClick(Clue clue, int index, boolean across) {
        Playboard board = getBoard();
        if (board != null) {
            Word old = board.getCurrentWord();
            board.jumpTo(index, across);
            displayKeyboard(old);
        }
    }

    @Override
    public void onClueTabsLongClick(Clue clue, int index, boolean across) {
        Playboard board = getBoard();
        if (board != null) {
            board.jumpTo(index, across);
            launchNotes();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Playboard board = getBoard();
        Word w = board.getCurrentWord();
        Position last = new Position(w.start.across
                + (w.across ? (w.length - 1) : 0), w.start.down
                + ((!w.across) ? (w.length - 1) : 0));

        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_BACK:
            this.finish();
            return true;

        case KeyEvent.KEYCODE_DPAD_LEFT:

            if (!board.getHighlightLetter().equals(
                    board.getCurrentWord().start)) {
                board.previousLetter();
            }

            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:

            if (!board.getHighlightLetter().equals(last)) {
                board.nextLetter();
            }

            return true;

        case KeyEvent.KEYCODE_DEL:
            w = board.getCurrentWord();
            board.deleteLetter();

            Position p = board.getHighlightLetter();

            if (!w.checkInWord(p.across, p.down)) {
                board.setHighlightLetter(w.start);
            }

            return true;

        case KeyEvent.KEYCODE_SPACE:

            if (!prefs.getBoolean("spaceChangesDirection", true)) {
                board.playLetter(' ');

                Position curr = board.getHighlightLetter();

                if (!board.getCurrentWord().equals(w)
                        || (board.getBoxes()[curr.across][curr.down] == null)) {
                    board.setHighlightLetter(last);
                }

                return true;
            }
        }

        char c = Character
                .toUpperCase(((configuration.hardKeyboardHidden
                                   == Configuration.HARDKEYBOARDHIDDEN_NO) ||
                               keyboardManager.getUseNativeKeyboard()) ? event
                        .getDisplayLabel() : ((char) keyCode));

        if (PlayActivity.ALPHA.indexOf(c) != -1) {
            board.playLetter(c);

            Position p = board.getHighlightLetter();

            if (!board.getCurrentWord().equals(w)
                    || (board.getBoxes()[p.across][p.down] == null)) {
                board.setHighlightLetter(last);
            }

            if ((puz.getPercentComplete() == 100) && (timer != null)) {
                timer.stop();
                puz.setTime(timer.getElapsed());
                this.timer = null;
                Intent i = new Intent(ClueListActivity.this, PuzzleFinishedActivity.class);
                this.startActivity(i);

            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            if ((puz != null) && (baseFile != null)) {
                if ((timer != null) && (puz.getPercentComplete() != 100)) {
                    this.timer.stop();
                    puz.setTime(timer.getElapsed());
                    this.timer = null;
                }

                IO.save(puz, baseFile);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        keyboardManager.onPause();

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (clueTabs != null)
            clueTabs.removeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardManager.onDestroy();
    }

    private void displayKeyboard() {
        keyboardManager.render();
    }

    private void displayKeyboard(Word previousWord) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previousWord != null) &&
                previousWord.checkInWord(newPos.across, newPos.down)) {
                keyboardManager.render();
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void render() {
        scaleRendererToCurWord();
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        this.imageView.setBitmap(renderer.drawWord(displayScratch, displayScratch));
    }

    private Playboard getBoard(){
        return ForkyzApplication.getInstance().getBoard();
    }

    /**
     * Scale the current renderer to fit the length of the currently
     * selected word.
     */
    private void scaleRendererToCurWord() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int curWordLen = getBoard().getCurrentWord().length;
        double scale = this.renderer.fitTo(metrics.widthPixels, curWordLen);
        if (scale > 1)
            this.renderer.setScale((float) 1);
    }

    private void launchNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        ClueListActivity.this.startActivityForResult(i, 0);
    }
}
