package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import java.util.logging.Logger;

public class ClueTabs extends TabHost {
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

        setup();

        TabSpec acrossTab = newTabSpec("TAB1");
        acrossTab.setIndicator("Across",
                               ContextCompat.getDrawable(context, R.drawable.across));
        acrossTab.setContent(R.id.acrossClueTab);

        TabSpec downTab = newTabSpec("TAB2");
        downTab.setIndicator("Down",
                             ContextCompat.getDrawable(context, R.drawable.down));
        downTab.setContent(R.id.downClueTab);

        TabSpec historyTab  = newTabSpec("TAB3");
        historyTab.setIndicator("Recent");
        historyTab.setContent(R.id.historyTab);

        addTab(acrossTab);
        addTab(downTab);
        addTab(historyTab);
    }

    public void setBoard(Playboard board) {
        if (board == null)
            return;

        this.board = board;
        this.acrossView = (ListView) this.findViewById(R.id.acrossClueTab);
        this.downView = (ListView) this.findViewById(R.id.downClueTab);

        acrossView.setAdapter(new ArrayAdapter(context,
                //android.R.layout.simple_list_item_single_choice,
                R.layout.clue_list_item,
                board.getAcrossClues()) {
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
        // TODO: do we need this? why only for across?
        // acrossView.setFocusableInTouchMode(true);$

        downView.setAdapter(new ArrayAdapter(context,
                //android.R.layout.simple_list_item_single_choice,
                R.layout.clue_list_item,
                board.getDownClues()) {
            @Override
            public View getView(int position,
                                View convertView,
                                ViewGroup parent) {
                return getClueListView(position,
                                       convertView,
                                       parent,
                                       (Clue) getItem(position),
                                       false);
            }
        });

        acrossView.invalidateViews();
        downView.invalidateViews();
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
        if (board == null)
            return null;

        LayoutInflater inflater = LayoutInflater.from(context);

        View view;
        TextView tv;
        if (board.isFilled(position, across)) {
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

