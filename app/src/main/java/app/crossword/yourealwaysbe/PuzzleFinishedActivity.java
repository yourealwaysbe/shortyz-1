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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

public class PuzzleFinishedActivity extends ForkyzActivity {
    private static final long SECONDS = 1000;
    private static final long MINUTES = SECONDS * 60;
    private static final long HOURS = MINUTES * 60;
    private final NumberFormat two_int = NumberFormat.getIntegerInstance();
    private final DateFormat date = DateFormat.getDateInstance(DateFormat.SHORT);

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

        two_int.setMinimumIntegerDigits(2);

        long elapsed = puz.getTime();
        finishedTime = elapsed;

        long hours = elapsed / HOURS;
        elapsed = elapsed % HOURS;

        long minutes = elapsed / MINUTES;
        elapsed = elapsed % MINUTES;

        long seconds = elapsed / SECONDS;

        String elapsedString = (hours > 0 ? two_int.format(hours) + ":" : "") +
                two_int.format(minutes) + ":"+
                two_int.format(seconds);

        int totalClues = puz.getAcrossClues().length + puz.getDownClues().length;
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
        String cheatedString = cheatedBoxes + " (" + cheatLevel + "%)";

        final String shareMessage;
        if(puz.getSource() != null && puz.getDate() != null){
            shareMessage = "I finished the "+puz.getSource()+" crossword for "+ date.format(puz.getDate()) +" in "+
                elapsedString +(cheatedBoxes > 0 ? " but got "+cheatedBoxes+ " hints" : "")+" in #Forkyz!";
        } else {
            shareMessage = "I finished "+puz.getSource()+" in "+
                    elapsedString +(cheatedBoxes > 0 ? "but got "+cheatedBoxes +" hints" : "")+" with #Forkyz!";
        }

        TextView elapsedTime = this.findViewById(R.id.elapsed);
        elapsedTime.setText(elapsedString);

        TextView totalCluesView = this.findViewById(R.id.totalClues);
        totalCluesView.setText(Integer.toString(totalClues));

        TextView totalBoxesView = this.findViewById(R.id.totalBoxes);
        totalBoxesView.setText(Integer.toString(totalBoxes));

        TextView cheatedBoxesView = this.findViewById(R.id.cheatedBoxes);
        cheatedBoxesView.setText(cheatedString);

        Button share = this.findViewById(R.id.share);
        share.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share your time"));
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
