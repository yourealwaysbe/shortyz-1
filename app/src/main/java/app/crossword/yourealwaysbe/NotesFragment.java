package app.crossword.yourealwaysbe;

import java.util.Random;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.view.BoardEditText.BoardEditFilter;
import app.crossword.yourealwaysbe.view.BoardEditText;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

public class NotesFragment extends PuzzleActivity {
    private static final Logger LOG = Logger.getLogger(
        NotesFragment.class.getCanonicalName()
    );

    private static final String TRANSFER_RESPONSE_REQUEST_KEY
        = "transferResponseRequest";

    private enum TransferResponseRequest {
        SCRATCH_TO_BOARD,
        ANAGRAM_SOL_TO_BOARD,
        BOARD_TO_SCRATCH,
        BOARD_TO_ANAGRAM_SOL
    }

    protected KeyboardManager keyboardManager;
    private TextView clueLine;
    private ScrollingImageView imageView;
    private CharSequence imageViewDescriptionBase;
    private EditText notesBox;
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
        Puzzle puz = board.getPuzzle();

        if (board == null || puz == null) {
            LOG.info("NotesFragment resumed but no Puzzle selected, finishing.");
            finish();
            return;
        }

        setContentView(R.layout.notes);

        clueLine = (TextView) this.findViewById(R.id.clueLine);
        if (clueLine != null && clueLine.getVisibility() != View.GONE) {
            clueLine.setVisibility(View.GONE);
            clueLine
                = (TextView) utils
                    .onActionBarCustom(this, R.layout.clue_line_only)
                    .findViewById(R.id.clueLine);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            clueLine.setAutoSizeTextTypeUniformWithConfiguration(
                5, prefs.getInt("clueSize", 12), 1,
                TypedValue.COMPLEX_UNIT_SP
            );
        }

        imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);
        imageViewDescriptionBase = imageView.getContentDescription();
        imageView.setAllowOverScroll(false);
        this.imageView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                View focused = getWindow().getCurrentFocus();
                int id = focused.getId();
                if (id == R.id.scratchMiniboard) {
                    NotesFragment.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_SCRATCH, true
                    );
                } else if (id == R.id.anagramSolution) {
                    NotesFragment.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_ANAGRAM_SOL, true
                    );
                }
            }

            public void onTap(Point e) {
                NotesFragment.this.keyboardManager.showKeyboard(imageView);

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
        this.imageView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP)
                    return NotesFragment.this.onMiniboardKeyUp(keyCode, event);
                else
                    return false;
            }
        });

        notesBox = (EditText) this.findViewById(R.id.notesBox);

        notesBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotesFragment.this.moveScratchToNote();
                return true;
            }
        });
        notesBox.setOnFocusChangeListener(
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean gainFocus) {
                    NotesFragment.this.onNotesBoxFocusChanged(gainFocus);
                }
            }
        );

        scratchView = (BoardEditText) this.findViewById(R.id.scratchMiniboard);
        scratchView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                executeTransferResponseRequest(
                    TransferResponseRequest.SCRATCH_TO_BOARD, true
                );
            }

            public void onTap(Point e) {
                NotesFragment.this.keyboardManager.showKeyboard(scratchView);
                NotesFragment.this.render();
            }
        });

        anagramSourceView = (BoardEditText) this.findViewById(R.id.anagramSource);
        anagramSolView = (BoardEditText) this.findViewById(R.id.anagramSolution);

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
                NotesFragment.this.render();
            }

            public void onTap(Point e) {
                NotesFragment
                    .this
                    .keyboardManager
                    .showKeyboard(anagramSourceView);
                NotesFragment.this.render();
            }
        });

        anagramSolView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                executeTransferResponseRequest(
                    TransferResponseRequest.ANAGRAM_SOL_TO_BOARD, true
                );
            }

            public void onTap(Point e) {
                NotesFragment.this.keyboardManager.showKeyboard(anagramSolView);
                NotesFragment.this.render();
            }
        });

        ForkyzKeyboard keyboardView
            = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboardView, imageView);
        keyboardManager.showKeyboard(imageView);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            if (!keyboardManager.handleBackKey())
                this.finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
            int number = getBoard().getClueNumber();
            boolean across = getBoard().isAcross();
            puz.setNote(number, note, across);
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

    private boolean onMiniboardKeyUp(int keyCode, KeyEvent event) {
        Word w = getBoard().getCurrentWord();
        boolean across = w.across;
        Position last = new Position(w.start.across
                + (w.across ? (w.length - 1) : 0), w.start.down
                + ((!w.across) ? (w.length - 1) : 0));

        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT:

            if (!getBoard().getHighlightLetter().equals(
                    getBoard().getCurrentWord().start)) {
                if (across)
                    getBoard().moveLeft();
                else
                    getBoard().moveUp();
            }

            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:

            if (!getBoard().getHighlightLetter().equals(last)) {
                if (across)
                    getBoard().moveRight();
                else
                    getBoard().moveDown();
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
            getBoard().playLetter(' ');

            Position curr = getBoard().getHighlightLetter();

            if (!getBoard().getCurrentWord().equals(w)
                    || (getBoard().getBoxes()[curr.across][curr.down] == null)) {
                getBoard().setHighlightLetter(last);
            }

            return true;
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

        return false;
    }

    @Override
    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        super.onPlayboardChange(wholeBoard, currentWord, previousWord);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = board.getPuzzle();
        Clue clue = board.getClue();

        if (board == null || puz == null) {
            LOG.info(
                "NotesFragment resumed but no Puzzle or Clue selected, "
                    + "finishing."
            );
            finish();
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        this.renderer = new PlayboardRenderer(
            getBoard(),
            metrics.densityDpi, metrics.widthPixels,
            !prefs.getBoolean("supressHints", false),
            this
        );

        final int curWordLen = getBoard().getCurrentWord().length;

        double scale = renderer.fitTo(metrics.widthPixels, curWordLen);
        if (scale > 1)
            renderer.setScale((float) 1);

        clueLine.setText(getLongClueText(clue, curWordLen));

        // set up and erase any previous data
        notesBox.setText("");

        // set lengths after fully set up
        scratchView.setRenderer(renderer);
        scratchView.clear();
        anagramSourceView.setRenderer(renderer);
        anagramSourceView.clear();
        anagramSolView.setRenderer(renderer);
        anagramSolView.clear();

        numAnagramLetters = 0;

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

        anagramSourceView.setFilters(new BoardEditFilter[]{sourceFilter});

        BoardEditFilter solFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (Character.isLetter(oldChar)) {
                    for (int i = 0; i < curWordLen; i++) {
                        if (anagramSourceView.isBlank(i)) {
                            anagramSourceView.setResponse(i, oldChar);
                            return true;
                        }
                    }
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                boolean changed
                    = NotesFragment.this.preAnagramSolResponse(pos, newChar);
                return changed ? newChar : '\0';
            }
        };

        anagramSolView.setFilters(new BoardEditFilter[]{solFilter});

        Note note = puz.getNote(clue.getNumber(), clue.getIsAcross());
        if (note != null) {
            notesBox.setText(note.getText());

            scratchView.setFromString(note.getScratch());

            String src = note.getAnagramSource();
            if (src != null) {
                anagramSourceView.setFromString(src);
                for (int i = 0; i < src.length(); i++) {
                    if (Character.isLetter(src.charAt(i))) {
                        numAnagramLetters++;
                    }
                }
            }

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

        anagramSolView.setLength(curWordLen);
        scratchView.setLength(curWordLen);
        anagramSourceView.setLength(curWordLen);

        keyboardManager.onResume();

        this.render();
    }

    protected void render() {
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        boolean displayScratchAcross = displayScratch && !getBoard().isAcross();
        boolean displayScratchDown = displayScratch && getBoard().isAcross();
        this.imageView.setBitmap(
            renderer.drawWord(displayScratchAcross, displayScratchDown)
        );
        this.imageView.setContentDescription(
            renderer.getContentDescription(this.imageViewDescriptionBase)
        );
    }

    private void moveScratchToNote() {
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String notesText = notesBox.getText().toString();

        String scratchText = scratchView.toString().trim();

        if (scratchText.length() > 0) {
            if (notesText.length() > 0)
                notesText += "\n";
            notesText += scratchText;

            scratchView.clear();
            notesBox.setText(notesText);
        }

        render();
    }

    private void copyBoxesToAnagramSol(Box[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isBlank()) {
                char newChar = boxes[i].getResponse();
                boolean allowed = preAnagramSolResponse(i, newChar);
                if (allowed)
                    anagramSolView.setResponse(i, newChar);
            }
        }
    }

    private boolean hasConflict(Box[] source,
                                Box[] dest,
                                boolean copyBlanks) {
        int length = Math.min(source.length, dest.length);
        for (int i = 0; i < length; i++) {
            if ((copyBlanks || !source[i].isBlank()) &&
                !dest[i].isBlank() &&
                source[i].getResponse() != dest[i].getResponse()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies non-blank characters from response to the view
     */
    private void overlayBoxesOnBoardView(Box[] boxes,
                                         BoardEditText view) {
        int length = Math.min(boxes.length, view.getLength());
        for (int i = 0; i < length; i++) {
            if (!boxes[i].isBlank()) {
                view.setResponse(i, boxes[i].getResponse());
            }
        }
    }

    /**
     * Make arrangements for anagram letter to be played
     *
     * Changes source/sol boxes by moving required letters around.
     *
     * @return true if play of letter can proceed
     */
    private boolean preAnagramSolResponse(int pos, char newChar) {
        char oldChar = anagramSolView.getResponse(pos);
        if (Character.isLetter(newChar)) {
            int sourceLen = anagramSourceView.getLength();
            for (int i = 0; i < sourceLen; i++) {
                if (anagramSourceView.getResponse(i) == newChar) {
                    anagramSourceView.setResponse(i, oldChar);
                    return true;
                }
            }
            // if failed to find it in the source view, see if we can
            // find one to swap it with one in the solution
            int solLen = anagramSolView.getLength();
            for (int i = 0; i < solLen; i++) {
                if (anagramSolView.getResponse(i) == newChar) {
                    anagramSolView.setResponse(i, oldChar);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Transfer one board view to another
     *
     * Somewhat inelegant as a dialog confirmation has to be robust
     * against activity recreation, so it all gets funnelled through
     * here with little external state.
     *
     * The alternative is to copy-paste the dialog construction several
     * times. I'm not sure which is better.
     */
    private void executeTransferResponseRequest(
        TransferResponseRequest request,
        boolean confirmOverwrite
    ) {
        Playboard board = getBoard();
        if (board == null)
            return;

        boolean conflict = false;
        Box[] curWordBoxes = board.getCurrentWordBoxes();

        if (confirmOverwrite) {
            switch (request) {
            case SCRATCH_TO_BOARD:
                conflict = hasConflict(
                    scratchView.getBoxes(), curWordBoxes, true
                );
                break;
            case ANAGRAM_SOL_TO_BOARD:
                conflict = hasConflict(
                    anagramSolView.getBoxes(), curWordBoxes, true
                );
                break;
            case BOARD_TO_SCRATCH:
                conflict = hasConflict(
                    curWordBoxes, scratchView.getBoxes(), false
                );
                break;
            case BOARD_TO_ANAGRAM_SOL:
                conflict = false;
            }
        }

        if (conflict) {
            confirmAndExecuteTransferRequest(request);
        } else {
            switch (request) {
            case SCRATCH_TO_BOARD:
                board.setCurrentWord(scratchView.getBoxes());
                break;
            case ANAGRAM_SOL_TO_BOARD:
                board.setCurrentWord(anagramSolView.getBoxes());
                break;
            case BOARD_TO_SCRATCH:
                overlayBoxesOnBoardView(curWordBoxes, scratchView);
                break;
            case BOARD_TO_ANAGRAM_SOL:
                copyBoxesToAnagramSol(curWordBoxes);
                break;
            }
        }
    }

    private void confirmAndExecuteTransferRequest(
        TransferResponseRequest request
    ) {
        DialogFragment dialog = new TransferResponseRequestDialog();
        Bundle args = new Bundle();
        args.putSerializable(TRANSFER_RESPONSE_REQUEST_KEY, request);
        dialog.setArguments(args);
        dialog.show(
            getSupportFragmentManager(), "TransferResponseRequestDialog"
        );
    }

    public static class TransferResponseRequestDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final NotesFragment activity = (NotesFragment) getActivity();

            Bundle args = getArguments();
            TransferResponseRequest request
                = (TransferResponseRequest)
                    args.getSerializable(TRANSFER_RESPONSE_REQUEST_KEY);

            AlertDialog.Builder builder
                = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.copy_conflict)
                .setMessage(R.string.transfer_overwrite_warning)
                .setPositiveButton(R.string.yes,
                                      new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NotesFragment activity = ((NotesFragment) getActivity());
                        activity.executeTransferResponseRequest(request, false);
                    }
                })
                .setNegativeButton(R.string.no,
                                          new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            return builder.create();
        }
    }

    /**
     * Force hide in-app keyboard if focus gained, hide soft keyboard if
     * focus lost
     */
    private void onNotesBoxFocusChanged(boolean gainFocus) {
        if (gainFocus) {
            keyboardManager.hideKeyboard(true);
        } else {
            InputMethodManager imm
                = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(notesBox.getWindowToken(), 0);
            }
        }
    }
}
