package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Puzzle.HistoryItem;
import app.crossword.yourealwaysbe.puz.util.WeakSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.CheckedTextView;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ClueTabs extends LinearLayout
                      implements Playboard.PlayboardListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final int ACROSS_PAGE_INDEX = 0;
    private static final int DOWN_PAGE_INDEX = 1;
    private static final int HISTORY_PAGE_INDEX = 2;

    private static enum PageType {
        ACROSS, DOWN, HISTORY;
    }

    private ViewPager2 viewPager;
    private Playboard board;
    private Context context;
    private boolean listening = false;
    private Set<ClueTabsListener> listeners = WeakSet.buildSet();
    private GestureDetectorCompat tabSwipeDetector;
    private OnGestureListener tabSwipeListener;

    public static interface ClueTabsListener {
        /**
         * When the user clicks a clue
         *
         * @param clue the clue clicked
         * @param index the clue index in the across/down list
         * @param across if the clue is an across clue (or down)
         * @param view the view calling
         */
        default void onClueTabsClick(Clue clue,
                                     int index,
                                     boolean across,
                                     ClueTabs view) { }

        /**
         * When the user long-presses a clue
         *
         * @param clue the clue clicked
         * @param the clue index
         * @param across if the clue is an across clue (or down)
         * @param view the view calling
         */
        default void onClueTabsLongClick(Clue clue,
                                         int index,
                                         boolean across,
                                         ClueTabs view) { }

        /**
         * When the user swipes up on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarSwipeUp(ClueTabs view) { }

        /**
         * When the user swipes down on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarSwipeDown(ClueTabs view) { }

        /**
         * When the user swipes down on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarLongclick(ClueTabs view) { }

        /**
         * When the user changes the page being viewed
         *
         * @param view the view calling
         */
        default void onClueTabsPageChange(ClueTabs view, int pageNumber) { }
    }

    public ClueTabs(Context context, AttributeSet as) {
        super(context, as);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.clue_tabs, this);
    }

    public void setBoard(Playboard board) {
        if (board == null)
            return;

        this.board = board;

        TabLayout tabLayout = findViewById(R.id.clueTabsTabLayout);
        viewPager = findViewById(R.id.clueTabsPager);

        viewPager.setAdapter(new ClueTabsPagerAdapter());

        viewPager.registerOnPageChangeCallback(
            new ViewPager2.OnPageChangeCallback() {
                public void onPageSelected(int position) {
                    ClueTabs.this.notifyListenersPageChanged(position);
                }
            }
        );

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                case ACROSS_PAGE_INDEX: tab.setText("Across"); break;
                case DOWN_PAGE_INDEX: tab.setText("Down"); break;
                case HISTORY_PAGE_INDEX: tab.setText("History"); break;
                }
            }
        ).attach();

        tabLayout.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                return tabSwipeDetector.onTouchEvent(e);
            }
        });

        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClueTabs.this.notifyListenersTabsBarLongClick();
                    return true;
                }
            });
        }

        tabSwipeListener = new SimpleOnGestureListener() {
            // as recommended by the docs
            // https://developer.android.com/training/gestures/detector
            public boolean onDown(MotionEvent e) {
                return true;
            }

            public boolean onFling(MotionEvent e1,
                                   MotionEvent e2,
                                   float velocityX,
                                   float velocityY) {
                if (Math.abs(velocityY) < Math.abs(velocityX))
                    return false;

                if (velocityY > 0)
                    ClueTabs.this.notifyListenersTabsBarSwipeDown();
                else
                    ClueTabs.this.notifyListenersTabsBarSwipeUp();

                return true;
            }
        };
        tabSwipeDetector = new GestureDetectorCompat(tabLayout.getContext(),
                                                     tabSwipeListener);
    }

    public void setPage(int pageNumber) {
        if (viewPager != null) {
            viewPager.setCurrentItem(pageNumber);
        }
    }

    public void addListener(ClueTabsListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClueTabsListener listener) {
        listeners.remove(listener);
    }

    public void listenBoard() {
        if (board != null && !listening) {
            board.addListener(this);
            listening = true;
        }
    }

    public void unlistenBoard() {
        if (board != null && listening) {
            board.removeListener(this);
            listening = false;
        }
    }

    public void onResume() {

    }

    public void onPause() {

    }

    public void onPlayboardChange(Word currentWord, Word previousWord) {
        if (viewPager != null) {
            viewPager.getAdapter().notifyDataSetChanged();

            SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(ClueTabs.this.context);

            if (prefs.getBoolean("snapClue", false)) {
                if (board.isAcross())
                    viewPager.setCurrentItem(ACROSS_PAGE_INDEX);
                else
                    viewPager.setCurrentItem(DOWN_PAGE_INDEX);
            }
        }
    }

    private void notifyListenersClueClick(Clue clue,
                                          int index,
                                          boolean across) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsClick(clue, index, across, this);
    }

    private void notifyListenersClueLongClick(Clue clue,
                                              int index,
                                              boolean across) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsLongClick(clue, index, across, this);
    }

    private void notifyListenersTabsBarSwipeUp() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeUp(this);
    }

    private void notifyListenersTabsBarSwipeDown() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeDown(this);
    }

    private void notifyListenersTabsBarLongClick() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeDown(this);
    }

    private void notifyListenersPageChanged(int pageNumber) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsPageChange(this, pageNumber);
    }

    private class ClueTabsPagerAdapter extends RecyclerView.Adapter<ClueListHolder> {
        @Override
        public ClueListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.clue_tabs_page,
                                               parent,
                                               false);
            return new ClueListHolder(page);
        }

        @Override
        public void onBindViewHolder(ClueListHolder holder, int position) {
            Playboard board = ClueTabs.this.board;
            switch (position) {
            case ACROSS_PAGE_INDEX:
                holder.setContents(PageType.ACROSS);
                break;
            case DOWN_PAGE_INDEX:
                holder.setContents(PageType.DOWN);
                break;
            case HISTORY_PAGE_INDEX:
                holder.setContents(PageType.HISTORY);
                break;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    private class ClueListHolder extends RecyclerView.ViewHolder {
        private RecyclerView clueList;
        private ClueListAdapter clueListAdapter;
        private LinearLayoutManager layoutManager;
        private PageType pageType;

        public ClueListHolder(View view) {
            super(view);
            Context context = itemView.getContext();
            clueList = view.findViewById(R.id.tabClueList);

            layoutManager
                = new LinearLayoutManager(context);
            clueList.setLayoutManager(layoutManager);
            clueList.setItemAnimator(new DefaultItemAnimator());
            clueList.addItemDecoration(
                new DividerItemDecoration(context,
                                          DividerItemDecoration.VERTICAL)
            );
        }

        public void setContents(PageType pageType) {
            Playboard board = ClueTabs.this.board;


            if (board != null && this.pageType != pageType) {
                switch (pageType) {
                case ACROSS:
                case DOWN:
                    boolean across = pageType == PageType.ACROSS;

                    List<Clue> clues = Arrays.asList(across ?
                                                     board.getAcrossClues() :
                                                     board.getDownClues());

                    clueListAdapter = new AcrossDownAdapter(clues, across);
                    clueList.setAdapter(clueListAdapter);
                    break;

                case HISTORY:
                    Puzzle puz = board.getPuzzle();
                    if (puz != null) {
                        clueListAdapter = new HistoryListAdapter(puz.getHistory());
                    } else {
                        clueListAdapter
                            = new HistoryListAdapter(new LinkedList<>());
                    }
                    clueList.setAdapter(clueListAdapter);
                    break;
                }

                this.pageType = pageType;
            }

            clueListAdapter.notifyDataSetChanged();

            if (board != null) {
                SharedPreferences prefs
                    = PreferenceManager.getDefaultSharedPreferences(ClueTabs.this.context);

                if (prefs.getBoolean("snapClue", false)) {

                    switch (pageType) {
                    case ACROSS:
                    case DOWN:
                        int position = board.getCurrentClueIndex();
                        layoutManager.scrollToPositionWithOffset(position, 5);
                        break;
                    case HISTORY:
                        layoutManager.scrollToPositionWithOffset(0, 5);
                        break;
                    }
                }
            }
        }
    }

    private abstract class ClueListAdapter
            extends RecyclerView.Adapter<ClueViewHolder> {
        boolean showDirection;

        public ClueListAdapter(boolean showDirection) {
            this.showDirection = showDirection;
        }

        @Override
        public ClueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View clueView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.clue_list_item,
                                                   parent,
                                                   false);
            return new ClueViewHolder(clueView, showDirection);
        }
    }

    private class AcrossDownAdapter extends ClueListAdapter {
        private List<Clue> clueList;
        private boolean across;

        public AcrossDownAdapter(List<Clue> clueList,
                                 boolean across) {
            super(false);
            this.clueList = clueList;
            this.across = across;
        }

        @Override
        public void onBindViewHolder(ClueViewHolder holder, int position) {
            // plus one because first item not shown (it is current clue)
            Clue clue = clueList.get(position);
            holder.setClue(clue, across, position);
        }

        @Override
        public int getItemCount() {
            return clueList.size();
        }
    }

    public class HistoryListAdapter
           extends ClueListAdapter {

        private List<HistoryItem> historyList;

        public HistoryListAdapter(List<HistoryItem> historyList) {
            super(true);
            this.historyList = historyList;
        }

        @Override
        public void onBindViewHolder(ClueViewHolder holder, int position) {
            HistoryItem item = historyList.get(position);
            Playboard board = ClueTabs.this.board;
            if (board != null) {
                int number = item.getClueNumber();
                boolean across = item.getAcross();
                Clue clue = board.getClue(number, across);
                Puzzle puz = board.getPuzzle();
                if (puz != null) {
                    int idx = across ?
                              puz.getAcrossClueIndex(number) :
                              puz.getDownClueIndex(number);

                    holder.setClue(clue, across, idx);
                }
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }
    }

    private class ClueViewHolder extends RecyclerView.ViewHolder {
        private CheckedTextView clueView;
        private Clue clue;
        private boolean across;
        private int index;
        private boolean showDirection;

        public ClueViewHolder(View view, boolean showDirection) {
            super(view);
            this.clueView = view.findViewById(R.id.clue_text_view);
            this.showDirection = showDirection;

            this.clueView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClueTabs.this.notifyListenersClueClick(clue,
                                                           index,
                                                           across);
                }
            });

            this.clueView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClueTabs.this.notifyListenersClueLongClick(clue,
                                                               index,
                                                               across);
                    return true;
                }
            });
        }

        /**
         * Set the clue in the holder
         *
         * @param clue the clue
         * @param across if clue is across
         * @param index the index of the clue in the across/down clues list
         */
        public void setClue(Clue clue, boolean across, int index) {
            if (clue == null)
                return;

            Playboard board = ClueTabs.this.board;

            this.clue = clue;
            this.index = index;
            this.across = across;

            String direction = "";
            if (showDirection)
                direction = across ? "a" : "d";

            clueView.setText(clue.number + direction + ". " + clue.hint);

            int color = R.color.textColorPrimary;
            if (board != null && board.isFilled(index, across)) {
                color = R.color.textColorFilled;
            }

            clueView.setTextColor(itemView.getContext()
                                          .getResources()
                                          .getColor(color,
                                                    itemView.getContext()
                                                            .getTheme()));

            if (board != null) {
                Clue selectedClue = board.getClue();
                clueView.setChecked(board.isAcross() == across &&
                                    selectedClue.number == clue.number);
            }
        }
    }
}

