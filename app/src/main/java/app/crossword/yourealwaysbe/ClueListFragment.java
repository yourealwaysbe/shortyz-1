package app.crossword.yourealwaysbe;

import java.util.logging.Logger;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

public class ClueListFragment extends PuzzleActivity
                              implements ClueTabs.ClueTabsListener {
    private static final Logger LOG = Logger.getLogger(
        ClueListFragment.class.getCanonicalName()
    );

    private KeyboardManager keyboardManager;
    private ScrollingImageView imageView;
    private CharSequence imageViewDescriptionBase;
    private PlayboardRenderer renderer;
    private ClueTabs clueTabs;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.clue_list_menu_notes) {
            launchNotes();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create the activity
     *
     * This only sets up the UI widgets. The set up for the current
     * puzzle/board is done in onResume as these are held by the
     * application and may change while paused!
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        Playboard board = getBoard();
        Puzzle puz = getBoard().getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        setContentView(R.layout.clue_list);

        this.imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);
        this.imageViewDescriptionBase = this.imageView.getContentDescription();
        this.imageView.setAllowOverScroll(false);

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

        ForkyzKeyboard keyboard = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboard, imageView);
    }

    @Override
    public void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = getBoard().getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        this.renderer = new PlayboardRenderer(
            board,
            metrics.densityDpi, metrics.widthPixels,
            !prefs.getBoolean("supressHints", false),
            this
        );

        scaleRendererToCurWord();

        clueTabs.setBoard(board);
        clueTabs.addListener(this);
        clueTabs.listenBoard();
        clueTabs.refresh();

        keyboardManager.onResume();

        this.render();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clue_list_menu, menu);
        return true;
    }

    @Override
    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        super.onPlayboardChange(wholeBoard, currentWord, previousWord);
        this.render();
    }

    @Override
    public void onClueTabsClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board != null) {
            Word old = board.getCurrentWord();
            board.jumpToClue(clue.getNumber(), clue.getIsAcross());
            displayKeyboard(old);
        }
    }

    @Override
    public void onClueTabsLongClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board != null) {
            board.jumpToClue(clue.getNumber(), clue.getIsAcross());
            launchNotes();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (PlayActivity.ALPHA.indexOf(c) != -1)
            return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Playboard board = getBoard();
        Puzzle puz = board.getPuzzle();
        int curClueNumber = board.getClueNumber();
        Word w = board.getCurrentWord();
        boolean across = w.across;
        Position last = new Position(w.start.across
                + (across ? (w.length - 1) : 0), w.start.down
                + ((!across) ? (w.length - 1) : 0));

        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            if (!keyboardManager.handleBackKey())
                this.finish();
            return true;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (!board.getHighlightLetter().equals(
                    board.getCurrentWord().start)) {
                if (across)
                    board.moveLeft();
                else
                    board.moveUp();
            } else {
                clueTabs.prevPage();
                selectFirstClue();
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (!board.getHighlightLetter().equals(last)) {
                if (across)
                    board.moveRight();
                else
                    board.moveDown();
            } else {
                clueTabs.nextPage();
                selectFirstClue();
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_UP:
            int prev
                = puz.getClues(across)
                    .getPreviousClueNumber(curClueNumber, true);
            clueTabs.setForceSnap(true);
            board.jumpToClue(prev, across);
            clueTabs.setForceSnap(false);
            break;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            int next
                = puz.getClues(across)
                    .getNextClueNumber(curClueNumber, true);
            clueTabs.setForceSnap(true);
            board.jumpToClue(next, across);
            clueTabs.setForceSnap(false);
            break;

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
            }
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (PlayActivity.ALPHA.indexOf(c) != -1) {
            board.playLetter(c);

            Position p = board.getHighlightLetter();

            if (!board.getCurrentWord().equals(w)
                    || (board.getBoxes()[p.across][p.down] == null)) {
                board.setHighlightLetter(last);
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        keyboardManager.onPause();

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }
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
        keyboardManager.showKeyboard(imageView);
    }

    private void displayKeyboard(Word previousWord) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previousWord != null) &&
                previousWord.checkInWord(newPos.across, newPos.down)) {
                keyboardManager.showKeyboard(imageView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void render() {
        scaleRendererToCurWord();
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        this.imageView.setBitmap(renderer.drawWord(displayScratch, displayScratch));
        this.imageView.setContentDescription(
            renderer.getContentDescription(this.imageViewDescriptionBase)
        );
    }

    /**
     * Scale the current renderer to fit the length of the currently
     * selected word.
     */
    private void scaleRendererToCurWord() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int curWordLen = getBoard().getCurrentWord().length;
        double scale = this.renderer.fitTo(metrics.widthPixels, curWordLen);
        if (scale > 1)
            this.renderer.setScale((float) 1);
    }

    private void launchNotes() {
        // TODO
        //Intent i = new Intent(this, NotesActivity.class);
        //ClueListActivity.this.startActivity(i);
    }

    private void selectFirstClue() {
        Playboard board = getBoard();
        Puzzle puz = board.getPuzzle();
        int firstClue;

        switch (clueTabs.getCurrentPageType()) {
        case ACROSS:
            firstClue = puz.getClues(true).getFirstClueNumber();
            getBoard().jumpToClue(firstClue, true);
            break;
        case DOWN:
            firstClue = puz.getClues(false).getFirstClueNumber();
            getBoard().jumpToClue(firstClue, false);
            break;
        case HISTORY:
            // nothing to do
            break;
        }
    }
}
