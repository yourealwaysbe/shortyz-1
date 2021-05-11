package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PuzzleFinishedActivity extends ForkyzActivity {
    private static final long SECONDS = 1000;
    private static final long MINUTES = SECONDS * 60;
    private static final long HOURS = MINUTES * 60;

    /** Percentage varying from 0 to 100. */
    private int cheatLevel = 0;
    private long finishedTime = 0L;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils.holographic(this);
        setContentView(R.layout.completed);
        this.getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        Puzzle puz = ForkyzApplication.getInstance().getBoard().getPuzzle();

        long elapsed = puz.getTime();
        finishedTime = elapsed;

        long hours = elapsed / HOURS;
        elapsed = elapsed % HOURS;

        long minutes = elapsed / MINUTES;
        elapsed = elapsed % MINUTES;

        long seconds = elapsed / SECONDS;

        String elapsedString;
        if (hours > 0) {
            elapsedString = getString(
                R.string.completed_time_format_with_hours,
                hours, minutes, seconds
            );
        } else {
            elapsedString = getString(
                R.string.completed_time_format_no_hours,
                minutes, seconds
            );
        }

        int totalClues = puz.getNumberOfClues();
        int totalBoxes = 0;
        int cheatedBoxes = 0;
        for(Box b : puz.getBoxesList()){
            if(b == null){
                continue;
            }
            if(b.isCheated()){
                cheatedBoxes++;
            }
            totalBoxes++;
        }

        this.cheatLevel = cheatedBoxes * 100 / totalBoxes;
        if(this.cheatLevel == 0 && cheatedBoxes > 0){
            this.cheatLevel = 1;
        }
        String cheatedString = getString(
            R.string.num_hinted_boxes, cheatedBoxes, cheatLevel
        );

        String source = puz.getSource();
        if (source == null)
            source = puz.getTitle();
        if (source == null)
            source = "";

        final String shareMessage;
        if (puz.getDate() != null) {
            DateTimeFormatter dateFormat
                = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

            shareMessage = getResources().getQuantityString(
                R.plurals.share_message_with_date,
                cheatedBoxes,
                source, dateFormat.format(puz.getDate()), cheatedBoxes
            );
        } else {
            shareMessage = getResources().getQuantityString(
                R.plurals.share_message_no_date,
                cheatedBoxes,
                source, cheatedBoxes
            );
        }

        TextView elapsedTime = this.findViewById(R.id.elapsed);
        elapsedTime.setText(elapsedString);

        TextView totalCluesView = this.findViewById(R.id.totalClues);
        totalCluesView.setText(String.format(
            Locale.getDefault(), "%d", totalClues)
        );

        TextView totalBoxesView = this.findViewById(R.id.totalBoxes);
        totalBoxesView.setText(String.format(
            Locale.getDefault(), "%d", totalBoxes
        ));

        TextView cheatedBoxesView = this.findViewById(R.id.cheatedBoxes);
        cheatedBoxesView.setText(cheatedString);

        Button share = this.findViewById(R.id.share);
        share.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(
                    sendIntent, getString(R.string.share_your_time)
                ));
            }
        });

        Button done = this.findViewById(R.id.done);
        done.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                finish();
            }
        });
    }
}
