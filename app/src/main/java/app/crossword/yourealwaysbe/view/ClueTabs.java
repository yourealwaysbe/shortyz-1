package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Word;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.CheckedTextView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ClueTabs extends LinearLayout
                      implements Playboard.PlayboardListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final int ACROSS_PAGE_INDEX = 0;
    private static final int DOWN_PAGE_INDEX = 1;
    private static final int HISTORY_PAGE_INDEX = 2;

    private ViewPager2 viewPager;
    private Playboard board;
    private Context context;
    private boolean listening = false;

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

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                case ACROSS_PAGE_INDEX: tab.setText("Across"); break;
                case DOWN_PAGE_INDEX: tab.setText("Down"); break;
                case HISTORY_PAGE_INDEX: tab.setText("History"); break;
                }
            }
        ).attach();

        listenBoard();
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
            case ACROSS_PAGE_INDEX: holder.setContents(true); break;
            case DOWN_PAGE_INDEX: holder.setContents(false); break;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    private class ClueListHolder extends RecyclerView.ViewHolder {
        private RecyclerView clueList;
        private ClueListAdapter clueListAdapter;
        private LinearLayoutManager layoutManager;
        private boolean across;

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

        public void setContents(boolean across) {
            Playboard board = ClueTabs.this.board;

            // the second check is "either we haven't initialised or
            // we're changing direction"
            if (board != null &&
                (clueListAdapter == null || this.across != across)) {
                this.across = across;

                List<Clue> clues = Arrays.asList(across ?
                                                 board.getAcrossClues() :
                                                 board.getDownClues());

                clueListAdapter = new ClueListAdapter(clues, across);
                clueList.setAdapter(clueListAdapter);
            }

            clueListAdapter.notifyDataSetChanged();

            if (board != null && board.isAcross() == across) {
                SharedPreferences prefs
                    = PreferenceManager.getDefaultSharedPreferences(ClueTabs.this.context);

                if (prefs.getBoolean("snapClue", false)) {
                    int position = board.getCurrentClueIndex();
                    layoutManager.scrollToPositionWithOffset(position, 5);
                }
            }
        }
    }

    private class ClueListAdapter
            extends RecyclerView.Adapter<ClueViewHolder> {

        private List<Clue> clueList;
        private boolean across;

        public ClueListAdapter(List<Clue> clueList,
                               boolean across) {
            this.clueList = clueList;
            this.across = across;
        }

        @Override
        public ClueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View clueView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.clue_list_item,
                                                   parent,
                                                   false);

            return new ClueViewHolder(clueView);
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

    private class ClueViewHolder extends RecyclerView.ViewHolder {
        private CheckedTextView clueView;
        private Clue clue;
        private boolean across;
        private int position;

        public ClueViewHolder(View view) {
            super(view);
            this.clueView = view.findViewById(R.id.clue_text_view);

            this.clueView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LOG.info("Clicked");
                    onClueClicked();
                }
            });
        }

        private void setClue(Clue clue, boolean across, int position) {
            if (clue == null)
                return;

            Playboard board = ClueTabs.this.board;

            this.clue = clue;
            this.position = position;
            this.across = across;

            clueView.setText(clue.number + ". " + clue.hint);

            int color = R.color.textColorPrimary;
            if (board != null && board.isFilled(position, across)) {
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

        private void onClueClicked() {
            Playboard board = ClueTabs.this.board;
            if (board != null) {
                board.jumpTo(position, across);
            } else {
                // TODO show keyboard when clicked selected clue
            }
        }

    }


}

