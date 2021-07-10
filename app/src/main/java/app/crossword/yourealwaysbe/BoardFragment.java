package app.crossword.yourealwaysbe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ScaleListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

import java.util.logging.Logger;

public class BoardFragment extends Fragment
                          implements Playboard.PlayboardListener,
                                     ClueTabs.ClueTabsListener {
    private static final Logger LOG = Logger.getLogger(
        BoardFragment.class.getCanonicalName()
    );

    private static final double BOARD_DIM_RATIO = 1.0;
    private static final String SHOW_CLUES_TAB = "showCluesOnPlayScreen";
    private static final String CLUE_TABS_PAGE = "playActivityClueTabsPage";
    public static final String SHOW_TIMER = "showTimer";
    public static final String SCALE = "scale";

    private ClueTabs clueTabs;
    private ConstraintLayout constraintLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private KeyboardManager keyboardManager;
    private MovementStrategy movement = null;
    private ScrollingImageView boardView;
    private CharSequence boardViewDescriptionBase;

    private boolean showErrors = false;
    private boolean scratchMode = false;
    private long lastTap = 0;
    private int screenWidthInInches;
    private Runnable fitToScreenTask = new Runnable() {
        @Override
        public void run() {
            fitToScreen();
        }
    };

    private DisplayMetrics metrics;
    private SharedPreferences prefs;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        metrics = getResources().getDisplayMetrics();
        this.screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);
        LOG.info("Configuration Changed "+this.screenWidthInInches+" ");
        if(this.screenWidthInInches >= 7){
            this.handler.post(this.fitToScreenTask);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.prefs
            = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.board, container, false);

        metrics = getResources().getDisplayMetrics();
        this.screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);

        this.showErrors = this.prefs.getBoolean("showErrors", false);
        this.scratchMode = this.prefs.getBoolean("scratchMode", false);

        MovementStrategy movement = getMovementStrategy();

        // TODO: delete below with proper view model
        // board is loaded by BrowseActivity and put into the
        // Application, onResume sets up BoardFragment for current board
        // as it may change!
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        this.constraintLayout
            = (ConstraintLayout) view.findViewById(R.id.playConstraintLayout);

        this.boardView = (ScrollingImageView) view.findViewById(R.id.board);
        this.boardViewDescriptionBase = this.boardView.getContentDescription();
        this.clueTabs = view.findViewById(R.id.playClueTab);

        ForkyzKeyboard keyboardView
            = (ForkyzKeyboard) view.findViewById(R.id.keyboard);
        keyboardManager
            = new KeyboardManager(getActivity(), keyboardView, null);

        // TODO: shouldn't really be here, view model blah blah
        ForkyzApplication.getInstance().setRenderer(
            new PlayboardRenderer(
                board,
                metrics.densityDpi,
                metrics.widthPixels,
                !prefs.getBoolean("supressHints", false),
                getActivity()
            )
        );

        float scale = prefs.getFloat(SCALE, 1.0F);

        if (scale > getRenderer().getDeviceMaxScale()) {
            scale = getRenderer().getDeviceMaxScale();
        } else if (scale < getRenderer().getDeviceMinScale()) {
            scale = getRenderer().getDeviceMinScale();
        } else if (Float.isNaN(scale)) {
            scale = 1F;
        }
        prefs.edit().putFloat(SCALE, scale).apply();

        getRenderer().setScale(scale);
        board.setSkipCompletedLetters(
            this.prefs.getBoolean("skipFilled", false)
        );

        // TODO: needs a different mechanism
        //if(this.clue != null) {
        //    this.clue.setClickable(true);
        //    this.clue.setOnClickListener(new OnClickListener() {
        //        public void onClick(View arg0) {
        //            if (BoardFragment.this.prefs.getBoolean(SHOW_CLUES_TAB, true)) {
        //                BoardFragment.this.hideClueTabs();
        //                BoardFragment.this.render(true);
        //            } else {
        //                BoardFragment.this.showClueTabs();
        //                BoardFragment.this.render(true);
        //            }
        //        }
        //    });
        //    this.clue.setOnLongClickListener(new OnLongClickListener() {
        //        public boolean onLongClick(View arg0) {
        //            BoardFragment.this.launchClueList();
        //            return true;
        //        }
        //    });
        //}

        this.boardView.setCurrentScale(scale);
        this.boardView.setFocusable(true);
        this.registerForContextMenu(boardView);
        boardView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(final Point e) {
                handler.post(() -> {
                    try {
                        Position p = getRenderer().findBox(e);
                        Word w = getBoard().setHighlightLetter(p);
                        boolean displayScratch = prefs.getBoolean("displayScratch", false);
                        getRenderer().draw(w,
                                           displayScratch,
                                           displayScratch);

                        launchNotes();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            public void onTap(Point e) {
                try {
                    if (prefs.getBoolean("doubleTap", false)
                            && ((System.currentTimeMillis() - lastTap) < 300)) {
                        fitToScreen();
                    } else {
                        Position p = getRenderer().findBox(e);
                        if (getBoard().isInWord(p)) {
                            Word previous = getBoard().setHighlightLetter(p);
                            displayKeyboard(previous);
                        }
                    }

                    lastTap = System.currentTimeMillis();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        // constrain to 1:1 if clueTabs is showing
        boardView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v,
              int left, int top, int right, int bottom,
              int leftWas, int topWas, int rightWas, int bottomWas
            ) {
                boolean constrainedDims = false;

                ConstraintSet set = new ConstraintSet();
                set.clone(constraintLayout);

                boolean showCluesTab = BoardFragment.this.prefs.getBoolean(
                    SHOW_CLUES_TAB, true
                );

                if (showCluesTab) {
                    int height = bottom - top;
                    int width = right - left;

                    int orientation
                        = BoardFragment.this
                            .getResources()
                            .getConfiguration()
                            .orientation;

                    boolean portrait
                        = orientation == Configuration.ORIENTATION_PORTRAIT;

                    if (portrait && height > width) {
                        constrainedDims = true;
                        set.constrainMaxHeight(
                            boardView.getId(),
                            (int)(BOARD_DIM_RATIO * width)
                        );
                    }
                } else {
                    set.constrainMaxHeight(boardView.getId(), 0);
                }

                set.applyTo(constraintLayout);

                // if the view changed size, then rescale the view
                // cannot change layout during a layout change, so
                // use a predraw listener that requests a new layout
                // (via render) and returns false to cancel the
                // current draw
                if (constrainedDims ||
                    left != leftWas || right != rightWas ||
                    top != topWas || bottom != bottomWas) {
                    boardView.getViewTreeObserver()
                             .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        public boolean onPreDraw() {
                            BoardFragment.this.render(true);
                            BoardFragment.this
                                        .boardView
                                        .getViewTreeObserver()
                                        .removeOnPreDrawListener(this);
                            return false;
                        }
                    });
                }
            }
        });

        this.boardView.setFocusable(true);
        this.boardView.setScaleListener(new ScaleListener() {
            public void onScale(float newScale, final Point center) {
                int w = boardView.getImageView().getWidth();
                int h = boardView.getImageView().getHeight();
                float scale = getRenderer().fitTo((w < h) ? w : h);
                prefs.edit().putFloat(SCALE, scale).apply();
                lastTap = System.currentTimeMillis();
            }
        });

        if (this.prefs.getBoolean("fitToScreen", false) || (ForkyzApplication.isLandscape(metrics)) && (ForkyzApplication.isTabletish(metrics) || ForkyzApplication.isMiniTabletish(metrics))) {
            this.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boardView.scrollTo(0, 0);

                    int v = (boardView.getWidth() < boardView.getHeight()) ? boardView
                            .getWidth() : boardView.getHeight();
                    if (v == 0) {
                        handler.postDelayed(this, 100);
                    }
                    float newScale = getRenderer().fitTo(v);
                    boardView.setCurrentScale(newScale);

                    prefs.edit().putFloat(SCALE, newScale).apply();
                    render();
                }
            }, 100);

        }

        registerBoard();

        return view;
    }

    private static String neverNull(String val) {
        return val == null ? "" : val.trim();
    }

    // TODO: needs to use eventbus or similar
    // basic solution activity passes on onKeyDown
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_SEARCH:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_SPACE:
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DEL:
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (PlayActivity.ALPHA.indexOf(c) != -1)
            return true;

        return false;
    }

    // TODO: needs to use eventbus or similar
    // basic solution activity passes on onKeyDown
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        boolean handled = false;

        // handle back separately as it we shouldn't block a keyboard
        // hide because of it
        if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (!keyboardManager.handleBackKey()) {
                getActivity().finish();
            }
            handled = true;
        }

        keyboardManager.pushBlockHide();

        if (getBoard() != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH:
                    getBoard().setMovementStrategy(
                        MovementStrategy.MOVE_NEXT_CLUE
                    );
                    getBoard().nextWord();
                    getBoard().setMovementStrategy(this.getMovementStrategy());
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    getBoard().moveDown();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    getBoard().moveUp();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    getBoard().moveLeft();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    getBoard().moveRight();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    getBoard().toggleDirection();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_SPACE:
                    if (prefs.getBoolean("spaceChangesDirection", true)) {
                        getBoard().toggleDirection();
                    } else if (this.scratchMode) {
                        getBoard().playScratchLetter(' ');
                    } else {
                        getBoard().playLetter(' ');
                    }
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    if (prefs.getBoolean("enterChangesDirection", true)) {
                        getBoard().toggleDirection();
                    } else {
                        getBoard().nextWord();
                    }
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DEL:
                    if (this.scratchMode) {
                        getBoard().deleteScratchLetter();
                    } else {
                        getBoard().deleteLetter();
                    }
                    handled = true;
                    break;
            }

            char c = Character.toUpperCase(event.getDisplayLabel());

            if (!handled && PlayActivity.ALPHA.indexOf(c) != -1) {
                if (this.scratchMode) {
                    getBoard().playScratchLetter(c);
                } else {
                    getBoard().playLetter(c);
                }
                handled = true;
            }
        }

        keyboardManager.popBlockHide();

        return handled;
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
    public void onClueTabsBarSwipeDown(ClueTabs view) {
        hideClueTabs();
        render(true);
    }

    @Override
    public void onClueTabsBarLongclick(ClueTabs view) {
        hideClueTabs();
        render(true);
    }

    @Override
    public void onClueTabsPageChange(ClueTabs view, int pageNumber) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(CLUE_TABS_PAGE, pageNumber);
        editor.apply();
    }

    // TODO: this shouldn't really be needed, the view model listens to
    // the board
    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        // TODO: handle view view model chicanery
        // super.onPlayboardChange(wholeBoard, currentWord, previousWord);

        // hide keyboard when moving to a new word
        Position newPos = getBoard().getHighlightLetter();
        if ((previousWord == null) ||
            !previousWord.checkInWord(newPos.across, newPos.down)) {
            keyboardManager.hideKeyboard();
        }

        if (!wholeBoard)
            render(previousWord, false);
        else
            render(false);
    }

    private void fitToScreen() {
        this.boardView.scrollTo(0, 0);

        int v = (this.boardView.getWidth() < this.boardView.getHeight()) ? this.boardView
                .getWidth() : this.boardView.getHeight();
        float newScale = getRenderer().fitTo(v);
        this.prefs.edit().putFloat(SCALE, newScale).apply();
        boardView.setCurrentScale(newScale);
        this.render(true);
    }

    // TODO: view model should somehow take care of this
    //protected void onTimerUpdate() {
    //    Puzzle puz = getPuzzle();
    //    ImaginaryTimer timer = getTimer();

    //    if (puz != null && timer != null) {
    //        getWindow().setTitle(timer.time());
    //    }
    //}

    @Override
    public void onPause() {
        super.onPause();

        keyboardManager.onPause();

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        this.onConfigurationChanged(getResources().getConfiguration());

        if (keyboardManager != null)
            keyboardManager.onResume();

        if (prefs.getBoolean(SHOW_CLUES_TAB, false)) {
            showClueTabs();
        } else {
            hideClueTabs();
        }

        registerBoard();
    }

    // TODO: move to view model handling
    private void registerBoard() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        // TODO: playactivity should monitor puzzle
        //setTitle(getString(
        //    R.string.play_activity_title,
        //    neverNull(puz.getTitle()),
        //    neverNull(puz.getAuthor()),
        //    neverNull(puz.getCopyright())
        //));

        if (puz.isUpdatable()) {
            this.showErrors = false;
        }

        if (board.isShowErrors() != this.showErrors) {
            board.toggleShowErrors();
        }

        if (clueTabs != null) {
            clueTabs.setBoard(board);
            clueTabs.setPage(prefs.getInt(CLUE_TABS_PAGE, 0));
            clueTabs.addListener(this);
            clueTabs.listenBoard();
            clueTabs.refresh();
        }

        if (board != null) {
            board.setSkipCompletedLetters(this.prefs.getBoolean("skipFilled", false));
            board.setMovementStrategy(this.getMovementStrategy());
            board.addListener(this);

            keyboardManager.attachKeyboardToView(boardView);

            render(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    protected MovementStrategy getMovementStrategy() {
        if (movement != null) {
            return movement;
        } else {
            return ForkyzApplication.getInstance().getMovementStrategy();
        }
    }

    /**
     * Change keyboard display if the same word has been selected twice
     */
    private void displayKeyboard(Word previous) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previous != null) &&
                previous.checkInWord(newPos.across, newPos.down)) {
                keyboardManager.showKeyboard(boardView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void render() {
        render(null);
    }

    private void render(boolean rescale) {
        this.render(null, rescale);
    }

    private void render(Word previous) {
        this.render(previous, false);
    }

    private void render(Word previous, boolean rescale) {
        if (getBoard() == null)
            return;

        boolean displayScratch = this.prefs.getBoolean("displayScratch", false);
        this.boardView.setBitmap(
            getRenderer().draw(previous, displayScratch, displayScratch),
            rescale
        );
        this.boardView.setContentDescription(
            getRenderer().getContentDescription(this.boardViewDescriptionBase)
        );
        this.boardView.requestFocus();
        /*
         * If we jumped to a new word, ensure the first letter is visible.
         * Otherwise, insure that the current letter is visible. Only necessary
         * if the cursor is currently off screen.
         */
        if (this.prefs.getBoolean("ensureVisible", true)) {
            Playboard board = getBoard();
            Word currentWord = board.getCurrentWord();
            Position cursorPos = board.getHighlightLetter();
            PlayboardRenderer renderer = getRenderer();

            Point topLeft;
            Point bottomRight;
            Point cursorTopLeft;
            Point cursorBottomRight;

            cursorTopLeft = renderer.findPointTopLeft(cursorPos);
            cursorBottomRight = renderer.findPointBottomRight(cursorPos);

            if ((previous != null) && previous.equals(currentWord)) {
                topLeft = cursorTopLeft;
                bottomRight = cursorBottomRight;
            } else {
                topLeft = renderer.findPointTopLeft(currentWord);
                bottomRight = renderer.findPointBottomRight(currentWord);
            }

            this.boardView.ensureVisible(bottomRight);
            this.boardView.ensureVisible(topLeft);

            // ensure the cursor is always on the screen.
            this.boardView.ensureVisible(cursorBottomRight);
            this.boardView.ensureVisible(cursorTopLeft);
        }

        // TODO: to live data in playactivity
        //Clue c = getBoard().getClue();
        //if (c != null) {
        //    this.clue.setText(getLongClueText(
        //        c, getBoard().getCurrentWord().length
        //    ));
        //}

        this.boardView.requestFocus();
    }

    private void launchNotes() {
        // TODO
        // Intent i = new Intent(getActivity(), NotesActivity.class);
        // startActivity(i);
    }

    private void launchClueList() {
        // TODO
        // Intent i = new Intent(getActivity(), ClueListActivity.class);
        // startActivity(i);
    }

    /**
     * Changes the constraints on clue tabs to show.
     *
     * Call render(true) after to rescale board. Updates shared prefs.
     */
    private void showClueTabs() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVisibility(clueTabs.getId(), ConstraintSet.VISIBLE);
        set.applyTo(constraintLayout);

        clueTabs.setPage(prefs.getInt(CLUE_TABS_PAGE, 0));

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SHOW_CLUES_TAB, true);
        editor.apply();
    }

    /**
     * Changes the constraints on clue tabs to hide.
     *
     * Call render(true) after to rescale board. Updates shared prefs.
     */
    private void hideClueTabs() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVisibility(clueTabs.getId(), ConstraintSet.GONE);
        set.applyTo(constraintLayout);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SHOW_CLUES_TAB, false);
        editor.apply();
    }

    private void showInfoDialog() {
        DialogFragment dialog = new InfoDialog();
        dialog.show(getActivity().getSupportFragmentManager(), "InfoDialog");
    }

    private void showRevealPuzzleDialog() {
        DialogFragment dialog = new RevealPuzzleDialog();
        dialog.show(
            getActivity().getSupportFragmentManager(), "RevealPuzzleDialog"
        );
    }

    public static class InfoDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder
                = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.puzzle_info_dialog, null);

            PlayActivity activity = (PlayActivity) getActivity();

            Puzzle puz = activity.getPuzzle();
            if (puz != null) {
                TextView title = view.findViewById(R.id.puzzle_info_title);
                title.setText(puz.getTitle());

                TextView author = view.findViewById(R.id.puzzle_info_author);
                author .setText(puz.getAuthor());

                TextView copyright
                    = view.findViewById(R.id.puzzle_info_copyright);
                copyright.setText(puz.getCopyright());

                TextView time = view.findViewById(R.id.puzzle_info_time);

                ImaginaryTimer timer = activity.getTimer();
                if (timer != null) {
                    timer.stop();
                    time.setText(getString(
                        R.string.elapsed_time, timer.time()
                    ));
                    timer.start();
                } else {
                    time.setText(getString(
                        R.string.elapsed_time,
                        new ImaginaryTimer(puz.getTime()).time()
                    ));
                }

                ProgressBar progress = (ProgressBar) view
                        .findViewById(R.id.puzzle_info_progress);
                progress.setProgress(puz.getPercentComplete());

                TextView filename
                    = view.findViewById(R.id.puzzle_info_filename);
                FileHandler fileHandler
                    = ForkyzApplication.getInstance().getFileHandler();
                filename.setText(
                    fileHandler.getUri(activity.getPuzHandle()).toString()
                );

                addNotes(view);
            }

            builder.setView(view);

            return builder.create();
        }

        private void addNotes(View dialogView) {
            TextView view = dialogView.findViewById(R.id.puzzle_info_notes);

            Puzzle puz = ((PlayActivity) getActivity()).getPuzzle();
            if (puz == null)
                return;

            String puzNotes = puz.getNotes();
            if (puzNotes == null)
                puzNotes = "";

            final String notes = puzNotes;

            String[] split =
                notes.split("(?i:(?m:^\\s*Across:?\\s*$|^\\s*\\d))", 2);

            final String text = split[0].trim();

            if (text.length() > 0) {
                view.setText(getString(
                    R.string.tap_to_show_full_notes_with_text, text
                ));
            } else {
                view.setText(getString(
                    R.string.tap_to_show_full_notes_no_text
                ));
            }

            view.setOnClickListener(new OnClickListener() {
                private boolean showAll = true;

                public void onClick(View view) {
                    TextView tv = (TextView) view;

                    if (showAll) {
                        if (notes == null || notes.length() == 0) {
                            tv.setText(getString(
                                R.string.tap_to_hide_full_notes_no_text
                            ));
                        } else {
                            tv.setText(getString(
                                R.string.tap_to_hide_full_notes_with_text,
                                notes
                            ));
                        }
                    } else {
                        if (text == null || text.length() == 0) {
                            tv.setText(getString(
                                R.string.tap_to_show_full_notes_no_text
                            ));
                        } else {
                            tv.setText(getString(
                                R.string.tap_to_show_full_notes_with_text,
                                text
                            ));
                        }
                    }

                    showAll = !showAll;
                }
            });
        }
    }

    public static class RevealPuzzleDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder
                = new AlertDialog.Builder(getActivity());

            builder.setTitle(getString(R.string.reveal_puzzle))
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Playboard board
                                = ((PlayActivity) getActivity()).getBoard();
                            if (board != null)
                                 board.revealPuzzle();
                        }
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
                );

            return builder.create();
        }
    }

    private void actionSupportSource() {
        Puzzle puz = getPuzzle();
        if (puz != null) {
            String supportUrl = puz.getSupportUrl();
            if (supportUrl != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(supportUrl));
                startActivity(i);
            }
        }
    }

    private PlayboardRenderer getRenderer(){
        return ForkyzApplication.getInstance().getRenderer();
    }

    // TODO: will be deleted on view model?
    private Playboard getBoard() {
        return ForkyzApplication.getInstance().getBoard();
    }

    // TODO: will be deleted on view model?
    private Puzzle getPuzzle() {
        return ForkyzApplication.getInstance().getBoard().getPuzzle();
    }
}
