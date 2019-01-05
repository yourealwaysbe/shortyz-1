package com.totsp.crossword;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.totsp.crossword.io.IO;
import com.totsp.crossword.puz.Playboard;
import com.totsp.crossword.puz.Playboard.Position;
import com.totsp.crossword.puz.Playboard.Word;
import com.totsp.crossword.puz.Puzzle;
import com.totsp.crossword.shortyz.R;
import com.totsp.crossword.shortyz.ShortyzApplication;
import com.totsp.crossword.view.PlayboardRenderer;
import com.totsp.crossword.view.ScrollingImageView;
import com.totsp.crossword.view.ScrollingImageView.ClickListener;
import com.totsp.crossword.view.ScrollingImageView.Point;

import java.io.File;
import java.io.IOException;

public class ClueListActivity extends ShortyzActivity {
	private Configuration configuration;
	private File baseFile;
	private ImaginaryTimer timer;
	private KeyboardManager keyboardManager;
	private ListView across;
	private ListView down;
	private Puzzle puz;
	private ScrollingImageView imageView;
	private TabHost tabHost;
	private boolean useNativeKeyboard = false;
	private PlayboardRenderer renderer;

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
        if(ShortyzApplication.getInstance().getBoard() == null || ShortyzApplication.getInstance().getBoard().getPuzzle() == null){
            finish();
        }
		this.timer = new ImaginaryTimer(
				ShortyzApplication.getInstance().getBoard().getPuzzle().getTime());

		Uri u = this.getIntent().getData();

		if (u != null) {
			if (u.getScheme().equals("file")) {
				baseFile = new File(u.getPath());
			}
		}

		puz = ShortyzApplication.getInstance().getBoard().getPuzzle();
		timer.start();
		setContentView(R.layout.clue_list);

		keyboardManager
            = new KeyboardManager(this, findViewById(R.id.clueKeyboard));

        this.imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);

		this.imageView.setContextMenuListener(new ClickListener() {
			public void onContextMenu(Point e) {
                onTap(e);
                launchNotes();
			}

			public void onTap(Point e) {
				Word current = getBoard().getCurrentWord();
				int newAcross = current.start.across;
				int newDown = current.start.down;
				int box = renderer.findBox(e).across;

				if (box < current.length) {
					if (tabHost.getCurrentTab() == 0) {
						newAcross += box;
					} else {
						newDown += box;
					}
				}

				Position newPos = new Position(newAcross, newDown);

				if (!newPos.equals(getBoard().getHighlightLetter())) {
					getBoard().setHighlightLetter(newPos);
				}
                ClueListActivity.this.render(true);
			}
		});

		this.tabHost = this.findViewById(R.id.tabhost);
		this.tabHost.setup();

		TabSpec ts = tabHost.newTabSpec("TAB1");

		ts.setIndicator("Across",
				ContextCompat.getDrawable(this, R.drawable.across));

		ts.setContent(R.id.acrossList);

		this.tabHost.addTab(ts);

		ts = this.tabHost.newTabSpec("TAB2");

		ts.setIndicator("Down", ContextCompat.getDrawable(this, R.drawable.down));

		ts.setContent(R.id.downList);
		this.tabHost.addTab(ts);

		this.tabHost.setCurrentTab(getBoard().isAcross() ? 0 : 1);

		this.across = (ListView) this.findViewById(R.id.acrossList);
		this.down = (ListView) this.findViewById(R.id.downList);

		across.setAdapter(new ArrayAdapter<>(this,
				//android.R.layout.simple_list_item_single_choice,
                R.layout.clue_list_item,
                getBoard().getAcrossClues()));
		across.setFocusableInTouchMode(true);
		down.setAdapter(new ArrayAdapter<>(this,
				//android.R.layout.simple_list_item_single_choice,
                R.layout.clue_list_item,
                getBoard().getDownClues()));

        int index = getBoard().getCurrentClueIndex();
        if (getBoard().isAcross())
            across.setItemChecked(index, true);
        else
            down.setItemChecked(index, true);

		across.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent,
                                           View view,
                                           int pos,
					                       long id) {
                onItemClickSelect(parent, view, pos, id, true);
                launchNotes();
                return true;
			}
		});
		across.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
                onItemClickSelect(arg0, arg1, arg2, arg3, true);
			}
		});
		across.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
                onItemClickSelect(arg0, arg1, arg2, arg3, true);
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		down.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent,
                                           View view,
                                           int pos,
					                       long id) {
                onItemClickSelect(parent, view, pos, id, false);
                launchNotes();
                return true;
			}
		});
		down.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1,
					final int arg2, long arg3) {
                onItemClickSelect(arg0, arg1, arg2, arg3, false);
			}
		});

		down.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
                onItemClickSelect(arg0, arg1, arg2, arg3, false);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		this.render(false);
	}

    @Override
    public void onResume() {
        super.onResume();
        keyboardManager.onResume(findViewById(R.id.clueKeyboard));
        this.render(false);
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

				this.render(true);
			}

			return true;

		case KeyEvent.KEYCODE_DPAD_RIGHT:

			if (!board.getHighlightLetter().equals(last)) {
				board.nextLetter();
				this.render(true);
			}

			return true;

		case KeyEvent.KEYCODE_DEL:
			w = board.getCurrentWord();
			board.deleteLetter();

			Position p = board.getHighlightLetter();

			if (!w.checkInWord(p.across, p.down)) {
				board.setHighlightLetter(w.start);
			}

			this.render(true);

			return true;

		case KeyEvent.KEYCODE_SPACE:

			if (!prefs.getBoolean("spaceChangesDirection", true)) {
				board.playLetter(' ');

				Position curr = board.getHighlightLetter();

				if (!board.getCurrentWord().equals(w)
						|| (board.getBoxes()[curr.across][curr.down] == null)) {
					board.setHighlightLetter(last);
				}

				this.render(true);

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

			this.render(true);

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

	private void render(boolean showKeyboard) {
        if (showKeyboard)
            keyboardManager.render();
        else
            keyboardManager.hideKeyboard();

		boolean displayScratch = prefs.getBoolean("displayScratch", false);
		this.imageView.setBitmap(renderer.drawWord(displayScratch, displayScratch));
	}

    private Playboard getBoard(){
        return ShortyzApplication.getInstance().getBoard();
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

    /**
     * Callback for on click / select listeners for clues.
     *
     * Args as in onItemClick except:
     *
     * @param isAcross true if across clues are being selected
     */
    private void onItemClickSelect(AdapterView<?> parent,
                                   View view,
					               int position,
                                   long id,
                                   boolean isAcross) {
        Playboard board = getBoard();
        if (board.isAcross() != isAcross
            || (board.getCurrentClueIndex() != position)) {
            parent.setSelected(true);

            board.jumpTo(position, isAcross);
            imageView.scrollTo(0, 0);
            scaleRendererToCurWord();
            render(false);

            ListView clueList = isAcross ? across : down;
            ListView otherList = isAcross ? down : across;

            if (prefs.getBoolean("snapClue", false)) {
                clueList.setSelectionFromTop(position, 5);
                clueList.setSelection(position);
            }

            clueList.setItemChecked(position, true);
            otherList.clearChoices();
        }
    }

    private void launchNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        ClueListActivity.this.startActivityForResult(i, 0);
    }
}
