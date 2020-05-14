package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.logging.Logger;

public class ClueTabs extends LinearLayout {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private ListView acrossView;
    private ListView downView;
    private RecyclerView historyView;
    private Playboard board;
    private Context context;

    public ClueTabs(Context context, AttributeSet as) {
        super(context, as);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.clue_tabs, this);
    }

    public void setBoard(Playboard board) {
        if (board == null)
            return;

        TabLayout tabLayout = findViewById(R.id.clueTabsTabLayout);
        ViewPager2 viewPager = findViewById(R.id.clueTabsPager);

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

    public class ClueListHolder extends RecyclerView.ViewHolder {
        private ListView clueList;
        private Playboard board;
        private boolean across;

        public ClueListHolder(View view) {
            super(view);
            clueList = view.findViewById(R.id.tabClueList);
        }

        public void setContents(Playboard board, boolean across) {
            this.board = board;
            this.across = across;

            Clue[] clues = across ?
                           board.getAcrossClues() :
                           board.getDownClues();

            clueList.setAdapter(
                new ArrayAdapter(context,
                                 //android.R.layout.simple_list_item_single_choice,
                                 R.layout.clue_list_item,
                                 clues) {
                    @Override
                    public View getView(int position,
                                        View convertView,
                                        ViewGroup parent) {
                        return getClueListView(position,
                                               convertView,
                                               parent,
                                               (Clue) getItem(position),
                                               true);
                    }
            });

            clueList.invalidateViews();
        }

        /**
         * Get the right view for a clue list item
         *
         * @param clue the clue at the position
         * @param across if is across
         */
        public View getClueListView(int position,
                                    View convertView,
                                    ViewGroup parent,
                                    Clue clue,
                                    boolean across) {
            LayoutInflater inflater = LayoutInflater.from(context);

            View view;
            TextView tv;
            if (board != null && board.isFilled(position, across)) {
                view = inflater.inflate(R.layout.clue_list_item_filled,
                                        parent,
                                        false);
                tv = (TextView) view.findViewById(R.id.clue_text_view_filled);
            } else {
                view = inflater.inflate(R.layout.clue_list_item,
                                        parent,
                                        false);
                tv = (TextView) view.findViewById(R.id.clue_text_view);
            }

            tv.setText(clue.toString());

            return view;
        }
    }
}

