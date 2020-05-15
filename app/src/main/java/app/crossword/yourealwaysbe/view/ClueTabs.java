package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;

import android.content.Context;
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

        viewPager.setAdapter(new ClueTabsPagerAdapter(board));

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                case 0: tab.setText("Across"); break;
                case 1: tab.setText("Down"); break;
                case 2: tab.setText("History"); break;
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

    public void onPlayboardChange() {
        if (viewPager != null) {
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    private class ClueTabsPagerAdapter extends RecyclerView.Adapter<ClueListHolder> {
        private Playboard board;

        public ClueTabsPagerAdapter(Playboard board) {
            this.board = board;
        }

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
            switch (position) {
            case 0: holder.setContents(board, true); break;
            case 1: holder.setContents(board, false); break;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    private static class ClueListHolder extends RecyclerView.ViewHolder {
        private RecyclerView clueList;
        private ClueListAdapter clueListAdapter;
        private Playboard board;
        private boolean across;

        public ClueListHolder(View view) {
            super(view);
            Context context = itemView.getContext();
            clueList = view.findViewById(R.id.tabClueList);

            RecyclerView.LayoutManager layoutManager
                 = new LinearLayoutManager(context);
             clueList.setLayoutManager(layoutManager);
             clueList.setItemAnimator(new DefaultItemAnimator());
             clueList.addItemDecoration(
                 new DividerItemDecoration(context,
                                           DividerItemDecoration.VERTICAL)
             );
        }

        public void setContents(Playboard board, boolean across) {
            if (this.board != board || this.across != across) {
                this.board = board;
                this.across = across;

                List<Clue> clues = Arrays.asList(across ?
                                                 board.getAcrossClues() :
                                                 board.getDownClues());

                clueListAdapter = new ClueListAdapter(clues, across, board);
                clueList.setAdapter(clueListAdapter);
            }
            clueListAdapter.notifyDataSetChanged();
        }
    }

    private static class ClueListAdapter
           extends RecyclerView.Adapter<ClueViewHolder> {

        private List<Clue> clueList;
        private boolean across;
        private Playboard board;

        public ClueListAdapter(List<Clue> clueList,
                               boolean across,
                               Playboard board) {
            this.clueList = clueList;
            this.across = across;
            this.board = board;
        }

        @Override
        public ClueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View clueView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.clue_list_item,
                                                   parent,
                                                   false);

            return new ClueViewHolder(clueView, board);
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

    private static class ClueViewHolder extends RecyclerView.ViewHolder {
        private CheckedTextView clueView;
        private Playboard board;

        public ClueViewHolder(View view, Playboard board) {
            super(view);
            this.clueView = view.findViewById(R.id.clue_text_view);
            this.board = board;
        }

        private void setClue(Clue clue, boolean across, int position) {
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
    }


}

