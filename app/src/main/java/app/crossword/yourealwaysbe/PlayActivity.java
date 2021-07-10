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
import androidx.lifecycle.ViewModelProvider;

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
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ScaleListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

import java.util.logging.Logger;

public class PlayActivity extends ForkyzActivity {
    private static final Logger LOG = Logger.getLogger(
        PlayActivity.class.getCanonicalName()
    );

    public static final String PUZ_HANDLE_ARG
        = "app.crossword.yourealwaysbe.PUZ_HANDLE";
    public static final String SHOW_TIMER = "showTimer";
    // TODO: this needs moving to property of puzzle or application or
    // something
    public static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private PlayActivityViewModel model;

    private TextView clue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = new ViewModelProvider(this).get(PlayActivityViewModel.class);

        PuzHandle ph
            = (PuzHandle) getIntent().getExtras().getParcelable(PUZ_HANDLE_ARG);

        if (ph == null) {
            LOG.warning("PlayActivity started with no puzzle to load");
            finish();
        }

        setContentView(R.layout.play);
        setDefaultKeyMode(Activity.DEFAULT_KEYS_DISABLE);
        setFullScreenMode();
        initialiseMainFragment(savedInstanceState);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        try {
            if (!prefs.getBoolean(SHOW_TIMER, false)) {
                if (ForkyzApplication.isLandscape(metrics)) {
                    if (ForkyzApplication.isMiniTabletish(metrics)) {
                        utils.hideWindowTitle(this);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        this.clue = this.findViewById(R.id.clueLine);
        if (clue != null && clue.getVisibility() != View.GONE) {
            this.clue.setVisibility(View.GONE);
            View custom = utils.onActionBarCustom(this, R.layout.clue_line_only);
            if (custom != null) {
                clue = custom.findViewById(R.id.clueLine);
            }
        }

        int smallClueTextSize
            = getResources().getInteger(R.integer.small_clue_text_size);
        this.setClueSize(prefs.getInt("clueSize", smallClueTextSize));

        // TODO: to live data in playactivity
        //Clue c = getBoard().getClue();
        //if (c != null) {
        //    this.clue.setText(getLongClueText(
        //        c, getBoard().getCurrentWord().length
        //    ));
        //}

        invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        model.pauseTimer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        model.resumeTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        model.pauseTimer();
        model.savePuzzle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        model.resumeTimer();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // TODO: handle this...?
        //if (prefs.getBoolean(SHOW_TIMER, false)) {
        //    handler.post(updateTimeTask);
        //}

        // TODO: don't directly change model!
        // public static final String PRESERVE_CORRECT
        //     = "preserveCorrectLettersInShowErrors";
        // public static final String DONT_DELETE_CROSSING = "dontDeleteCrossing";
        //Playboard board = getBoard();
        //if (board != null) {
        //    boolean preserveCorrect = prefs.getBoolean(PRESERVE_CORRECT, true);
        //    board.setPreserveCorrectLettersInShowErrors(preserveCorrect);
        //    boolean noDelCrossing = prefs.getBoolean(DONT_DELETE_CROSSING, false);
        //    board.setDontDeleteCrossing(noDelCrossing);
        //}
    }



    // TODO: something about options menu
    //@Override
    //public boolean onCreateOptionsMenu(Menu menu) {
    //    MenuInflater inflater = getMenuInflater();
    //    inflater.inflate(R.menu.play_menu, menu);

    //    if (getRenderer() == null || getRenderer().getScale() >= getRenderer().getDeviceMaxScale())
    //        menu.removeItem(R.id.play_menu_zoom_in_max);

    //    Puzzle puz = getPuzzle();
    //    if (puz == null || puz.isUpdatable()) {
    //        menu.findItem(R.id.play_menu_show_errors).setEnabled(false);
    //        menu.findItem(R.id.play_menu_reveal).setEnabled(false);
    //    } else {
    //        menu.findItem(R.id.play_menu_show_errors).setChecked(this.showErrors);

    //        if (ForkyzApplication.isTabletish(metrics)) {
    //            menu.findItem(R.id.play_menu_show_errors).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    //            menu.findItem(R.id.play_menu_reveal).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    //        }
    //    }

    //    if (puz == null || puz.getSupportUrl() == null) {
    //        MenuItem support = menu.findItem(R.id.play_menu_support_source);
    //        support.setVisible(false);
    //        support.setEnabled(false);
    //    }

    //    menu.findItem(R.id.play_menu_scratch_mode).setChecked(this.scratchMode);

    //    return true;
    //}
    //
    //private SpannableString createSpannableForMenu(String value){
    //    SpannableString s = new SpannableString(value);
    //    s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorPrimary)), 0, s.length(), 0);
    //    return s;
    //}

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        // TODO: menu system needs deciding
        //if (getBoard() != null) {
        //    if (id == R.id.play_menu_reveal_letter) {
        //        getBoard().revealLetter();
        //        return true;
        //    } else if (id == R.id.play_menu_reveal_word) {
        //        getBoard().revealWord();
        //        return true;
        //    } else if (id == R.id.play_menu_reveal_errors) {
        //        getBoard().revealErrors();
        //        return true;
        //    } else if (id == R.id.play_menu_reveal_puzzle) {
        //        showRevealPuzzleDialog();
        //        return true;
        //    } else if (id == R.id.play_menu_show_errors) {
        //        getBoard().toggleShowErrors();
        //        item.setChecked(getBoard().isShowErrors());
        //        this.prefs.edit().putBoolean(
        //            "showErrors", getBoard().isShowErrors()
        //        ).apply();
        //        return true;
        //    } else if (id == R.id.play_menu_scratch_mode) {
        //        this.scratchMode = !this.scratchMode;
        //        item.setChecked(this.scratchMode);
        //        this.prefs.edit().putBoolean(
        //            "scratchMode", this.scratchMode
        //        ).apply();
        //        return true;
        //    } else if (id == R.id.play_menu_settings) {
        //        Intent i = new Intent(getActivity(), PreferencesActivity.class);
        //        this.startActivity(i);
        //        return true;
        //    } else if (id == R.id.play_menu_zoom_in) {
        //        float newScale = getRenderer().zoomIn();
        //        this.prefs.edit().putFloat(SCALE, newScale).apply();
        //        boardView.setCurrentScale(newScale);
        //        this.render(true);
        //        return true;
        //    } else if (id == R.id.play_menu_zoom_in_max) {
        //        float newScale = getRenderer().zoomInMax();
        //        this.prefs.edit().putFloat(SCALE, newScale).apply();
        //        boardView.setCurrentScale(newScale);
        //        this.render(true);
        //        return true;
        //    } else if (id == R.id.play_menu_zoom_out) {
        //        float newScale = getRenderer().zoomOut();
        //        this.prefs.edit().putFloat(SCALE, newScale).apply();
        //        boardView.setCurrentScale(newScale);
        //        this.render(true);
        //        return true;
        //    } else if (id == R.id.play_menu_zoom_fit) {
        //        fitToScreen();
        //        return true;
        //    } else if (id == R.id.play_menu_zoom_reset) {
        //        float newScale = getRenderer().zoomReset();
        //        boardView.setCurrentScale(newScale);
        //        this.prefs.edit().putFloat(SCALE, newScale).apply();
        //        this.render(true);
        //        return true;
        //    } else if (id == R.id.play_menu_info) {
        //        showInfoDialog();
        //        return true;
        //    } else if (id == R.id.play_menu_clues) {
        //        BoardFragment.this.launchClueList();
        //        return true;
        //    } else if (id == R.id.play_menu_notes) {
        //        launchNotes();
        //        return true;
        //    } else if (id == R.id.play_menu_help) {
        //        Intent helpIntent = new Intent(
        //            Intent.ACTION_VIEW,
        //            Uri.parse("file:///android_asset/playscreen.html"),
        //            this,
        //            HTMLActivity.class
        //        );
        //        startActivity(helpIntent);
        //        return true;
        //    } else if (id == R.id.play_menu_clue_size_s) {
        //        setClueSize(
        //            getResources().getInteger(R.integer.small_clue_text_size)
        //        );
        //        return true;
        //    } else if (id == R.id.play_menu_clue_size_m) {
        //        setClueSize(
        //            getResources().getInteger(R.integer.medium_clue_text_size)
        //        );
        //        return true;
        //    } else if (id == R.id.play_menu_clue_size_l) {
        //        setClueSize(
        //            getResources().getInteger(R.integer.large_clue_text_size)
        //        );
        //        return true;
        //    } else if (id == R.id.play_menu_support_source) {
        //        actionSupportSource();
        //        return true;
        //    }
        //}

        return super.onOptionsItemSelected(item);
    }

    private void initialiseMainFragment(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.play_fragment, BoardFragment.class, null)
                .commit();
        }
    }

    @SuppressWarnings("deprecation")
    private void setFullScreenMode() {
        if (prefs.getBoolean("fullScreen", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController
                    = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                }
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    private void setClueSize(int dps) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.clue.setAutoSizeTextTypeUniformWithConfiguration(
                5, dps, 1, TypedValue.COMPLEX_UNIT_SP
            );
        }

        int smallClueTextSize
            = getResources().getInteger(R.integer.small_clue_text_size);
        if (prefs.getInt("clueSize", smallClueTextSize) != dps) {
            this.prefs.edit().putInt("clueSize", dps).apply();
        }
    }

    protected String getLongClueText(Clue clue, int wordLen) {
        boolean showCount = prefs.getBoolean("showCount", false);

        String hint = (clue == null)
            ? getString(R.string.unknown_hint)
            : clue.getHint();

        if (showCount) {
            int clueFormat = clue.getIsAcross()
                ? R.string.clue_format_across_long_with_length
                : R.string.clue_format_down_long_with_length;
            return getString(
                clueFormat, clue.getNumber(), clue.getHint(), wordLen
            );
        } else {
            int clueFormat = clue.getIsAcross()
                ? R.string.clue_format_across_long
                : R.string.clue_format_down_long;
            return getString(
                clueFormat, clue.getNumber(), clue.getHint()
            );
        }
    }
}
