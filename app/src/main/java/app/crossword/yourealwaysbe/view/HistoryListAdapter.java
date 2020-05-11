package app.crossword.yourealwaysbe;

import app.crossword.yourealwaysbe.HistoryItem;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

// based on tutorial: https://www.androidhive.info/2016/01/android-working-with-recycler-view/

public class HistoryListAdapter
       extends RecyclerView.Adapter<HistoryListAdapter.ClueViewHolder> {

    private List<HistoryItem> historyList;

    public class ClueViewHolder extends RecyclerView.ViewHolder {
        private TextView clueView;

        public ClueViewHolder(View view) {
            super(view);
            clueView = view.findViewById(R.id.history_text_view);
        }

        private void setHistoryItem(HistoryItem item) {
            String direction = item.getAcross() ? "a" : "d";
            clueView.setText(item.getClue().number + direction + ". " +
                             item.getClue().hint);
        }
    }

    public HistoryListAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @Override
    public ClueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View clueView = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.history_list_item,
                                               parent,
                                               false);

        return new ClueViewHolder(clueView);
    }

    @Override
    public void onBindViewHolder(ClueViewHolder holder, int position) {
        // plus one because first item not shown (it is current clue)
        HistoryItem item = historyList.get(position);
        holder.setHistoryItem(item);
    }

    @Override
    public int getItemCount() {
        // minus one because first item is current clue
        return historyList.size();
    }
}
