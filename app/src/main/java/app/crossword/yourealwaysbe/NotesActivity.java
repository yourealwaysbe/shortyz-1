package app.crossword.yourealwaysbe;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import android.os.Bundle;
import android.net.Uri;
import android.widget.EditText;
import android.widget.TextView;
import android.util.TypedValue;
import android.view.MenuItem;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.content.Intent;
import android.widget.Toast;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.view.BoardEditText;
import app.crossword.yourealwaysbe.view.BoardEditText.BoardEditFilter;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.util.KeyboardManager;

public class NotesActivity extends PuzzleActivity {
    private static final Logger LOG = Logger.getLogger(NotesActivity.class.getCanonicalName());

    protected KeyboardManager keyboardManager;
    private ScrollingImageView imageView;
    private BoardEditText scratchView;
    private BoardEditText anagramSourceView;
    private BoardEditText anagramSolView;
    private PlayboardRenderer renderer;

    private Random rand = new Random();

    private int numAnagramLetters = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item == null || item.getItemId() == android.R.id.home) {
            finish();
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
        this.renderer = new PlayboardRenderer(getBoard(), metrics.densityDpi, metrics.widthPixels, !prefs.getBoolean("supressHints", false), this);

        final int curWordLen = getBoard().getCurrentWord().length;
        double scale = this.renderer.fitTo(metrics.widthPixels, curWordLen);
        if (scale > 1)
            this.renderer.setScale((float) 1);

        Playboard board = getBoard();
        Puzzle puz = board.getPuzzle();

        setContentView(R.layout.notes);

        keyboardManager = new KeyboardManager(this);

        Clue c = board.getClue();

        boolean showCount = prefs.getBoolean("showCount", false);

        TextView clue = (TextView) this.findViewById(R.id.clueLine);
        if (clue != null && clue.getVisibility() != View.GONE) {
            clue.setVisibility(View.GONE);
            clue = (TextView) utils.onActionBarCustom(this,
                R.layout.clue_line_only).findViewById(R.id.clueLine);
        }

        clue.setAutoSizeTextTypeUniformWithConfiguration(
            5, prefs.getInt("clueSize", 12), 1, TypedValue.COMPLEX_UNIT_SP
        );

        clue.setText("("
            + (board.isAcross() ? "across" : "down")
            + ") "
            + c.number
            + ". "
            + c.hint
            + (showCount ? ("  ["
            + curWordLen + "]") : ""));

        imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);
        imageView.setAllowOverScroll(false);
        this.imageView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                // TODO Auto-generated method stub
            }

            public void onTap(Point e) {
                imageView.requestFocus();

                Word current = getBoard().getCurrentWord();
                int newAcross = current.start.across;
                int newDown = current.start.down;
                int box = renderer.findBox(e).across;

                if (box < current.length) {
                    if (getBoard().isAcross()) {
                        newAcross += box;
                    } else {
                        newDown += box;
                    }
                }

                Position newPos = new Position(newAcross, newDown);

                if (!newPos.equals(getBoard().getHighlightLetter())) {
                    getBoard().setHighlightLetter(newPos);
                }
            }
        });

        Note note = puz.getNote(c.number, getBoard().isAcross());
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);

        if (note != null) {
            notesBox.setText(note.getText());
        }

        notesBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotesActivity.this.moveScratchToNote();
                return true;
            }
        });

        scratchView = (BoardEditText) this.findViewById(R.id.scratchMiniboard);
        if (note != null) {
            scratchView.setFromString(note.getScratch());
        }
        scratchView.setRenderer(renderer);
        scratchView.setLength(curWordLen);
        scratchView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                copyBoardViewToBoard(scratchView);
            }

            public void onTap(Point e) {
                NotesActivity.this.render();
            }
        });

        anagramSourceView = (BoardEditText) this.findViewById(R.id.anagramSource);
        if (note != null) {
            String src = note.getAnagramSource();
            if (src != null) {
                anagramSourceView.setFromString(src);
                for (int i = 0; i < src.length(); i++) {
                    if (Character.isLetter(src.charAt(i))) {
                        numAnagramLetters++;
                    }
                }
            }
        }

        anagramSolView = (BoardEditText) this.findViewById(R.id.anagramSolution);
        if (note != null) {
            String sol = note.getAnagramSolution();
            if (sol != null) {
                anagramSolView.setFromString(sol);
                for (int i = 0; i < sol.length(); i++) {
                    if (Character.isLetter(sol.charAt(i))) {
                        numAnagramLetters++;
                    }
                }
            }
        }

        BoardEditFilter sourceFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (Character.isLetter(oldChar)) {
                    numAnagramLetters--;
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                if (Character.isLetter(newChar)) {
                    if (Character.isLetter(oldChar)) {
                        return newChar;
                    } else if (numAnagramLetters < curWordLen) {
                        numAnagramLetters++;
                        return newChar;
                    } else {
                        return '\0';
                    }
                } else {
                    return '\0';
                }
            }
        };

        anagramSourceView.setRenderer(renderer);
        anagramSourceView.setLength(curWordLen);
        anagramSourceView.setFilters(new BoardEditFilter[]{sourceFilter});
        anagramSourceView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                // reshuffle squares
                int len = anagramSourceView.getLength();
                for (int i = 0; i < len; i++) {
                    int j = rand.nextInt(len);
                    char ci = anagramSourceView.getResponse(i);
                    char cj = anagramSourceView.getResponse(j);
                    anagramSourceView.setResponse(i, cj);
                    anagramSourceView.setResponse(j, ci);
                }
                NotesActivity.this.render();
            }

            public void onTap(Point e) {
                NotesActivity.this.render();
            }
        });

        BoardEditFilter solFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (Character.isLetter(oldChar)) {
                    for (int i = 0; i < curWordLen; i++) {
                        if (anagramSourceView.getResponse(i) == ' ') {
                            anagramSourceView.setResponse(i, oldChar);
                            return true;
                        }
                    }
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                if (Character.isLetter(newChar)) {
                    for (int i = 0; i < curWordLen; i++) {
                        if (anagramSourceView.getResponse(i) == newChar) {
                            anagramSourceView.setResponse(i, oldChar);
                            return newChar;
                        }
                    }
                    // if failed to find it in the source view, see if we can
                    // find one to swap it with one in the solution
                    for (int i = 0; i < curWordLen; i++) {
                        if (anagramSolView.getResponse(i) == newChar) {
                            anagramSolView.setResponse(i, oldChar);
                            return newChar;
                        }
                    }
                }
                return '\0';
            }
        };

        anagramSolView.setRenderer(renderer);
        anagramSolView.setLength(curWordLen);
        anagramSolView.setFilters(new BoardEditFilter[]{solFilter});
        anagramSolView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                copyBoardViewToBoard(anagramSolView);
            }

            public void onTap(Point e) {
                NotesActivity.this.render();
            }
        });

        keyboardManager.disableForView(this.findViewById(R.id.notesBox));

        this.render();
    }

    public void onPause() {
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String text = notesBox.getText().toString();

        String scratch = scratchView.toString();
        String anagramSource = anagramSourceView.toString();
        String anagramSolution = anagramSolView.toString();

        Puzzle puz = getPuzzle();
        if (puz != null) {
            Note note = new Note(scratch, text, anagramSource, anagramSolution);
            Clue c = getBoard().getClue();
            puz.setNote(note, c.number, getBoard().isAcross());
        }

        super.onPause();

        keyboardManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish();
            return true;
        }

        View focused = getWindow().getCurrentFocus();

        switch (focused.getId()) {
        case R.id.miniboard:
            return onMiniboardKeyUp(keyCode, event);

        case R.id.scratchMiniboard:
            return scratchView.onKeyUp(keyCode, event);

        case R.id.anagramSource:
            return anagramSourceView.onKeyUp(keyCode, event);

        case R.id.anagramSolution:
            return anagramSolView.onKeyUp(keyCode, event);

        default:
            return false;
        }
    }

    private boolean onMiniboardKeyUp(int keyCode, KeyEvent event) {
        Word w = getBoard().getCurrentWord();
        Position last = new Position(w.start.across
                + (w.across ? (w.length - 1) : 0), w.start.down
                + ((!w.across) ? (w.length - 1) : 0));

        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT:

            if (!getBoard().getHighlightLetter().equals(
                    getBoard().getCurrentWord().start)) {
                getBoard().previousLetter();
            }

            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:

            if (!getBoard().getHighlightLetter().equals(last)) {
                getBoard().nextLetter();
            }

            return true;

        case KeyEvent.KEYCODE_DEL:
            w = getBoard().getCurrentWord();
            getBoard().deleteLetter();

            Position p = getBoard().getHighlightLetter();

            if (!w.checkInWord(p.across, p.down)) {
                getBoard().setHighlightLetter(w.start);
            }

            return true;

        case KeyEvent.KEYCODE_SPACE:

            if (!prefs.getBoolean("spaceChangesDirection", true)) {
                getBoard().playLetter(' ');

                Position curr = getBoard().getHighlightLetter();

                if (!getBoard().getCurrentWord().equals(w)
                        || (getBoard().getBoxes()[curr.across][curr.down] == null)) {
                    getBoard().setHighlightLetter(last);
                }

                return true;
            }
        }

        char c = Character .toUpperCase(event.getDisplayLabel());

        if (PlayActivity.ALPHA.indexOf(c) != -1) {
            getBoard().playLetter(c);

            Position p = getBoard().getHighlightLetter();

            if (!getBoard().getCurrentWord().equals(w)
                    || (getBoard().getBoxes()[p.across][p.down] == null)) {
                getBoard().setHighlightLetter(last);
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onPlayboardChange(Word currentWord, Word previousWord) {
        super.onPlayboardChange(currentWord, previousWord);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardManager.onResume();

        this.render();
    }

    protected void render() {
        keyboardManager.showKeyboard();

        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        boolean displayScratchAcross = displayScratch && !getBoard().isAcross();
        boolean displayScratchDown = displayScratch && getBoard().isAcross();
        this.imageView.setBitmap(renderer.drawWord(displayScratchAcross,
                                                   displayScratchDown));
    }

    private void copyBoardViewToBoard(final BoardEditText view) {
        Playboard board = getBoard();
        if (board == null)
            return;

        final Box[] curWordBoxes = board.getCurrentWordBoxes();
        final String response = view.toString();

        boolean conflicts = false;

        for (int i = 0; i < curWordBoxes.length && i < response.length(); i++) {
            char oldResponse = curWordBoxes[i].getResponse();
            if (Character.isLetter(oldResponse) &&
                response.charAt(i) != oldResponse) {
                conflicts = true;
                break;
            }
        }

        if (conflicts) {
            AlertDialog.Builder builder
                = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, R.style.dialogStyle)
                );

            builder.setTitle("Copy Conflict");
            builder.setMessage("The new solution conflicts with existing entries.  Overwrite anyway?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    board.setCurrentWord(response);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        } else {
            board.setCurrentWord(response);
        }
    }

    private void moveScratchToNote() {
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String notesText = notesBox.getText().toString();

        String scratchText = scratchView.toString();

        if (notesText.length() > 0)
            notesText += "\n";
        notesText += scratchText;

        scratchView.clear();
        notesBox.setText(notesText);

        render();
    }
}
