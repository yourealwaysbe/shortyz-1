package app.crossword.yourealwaysbe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.Spinner;
import android.widget.TextView;

import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.net.DummyDownloader;
import app.crossword.yourealwaysbe.forkyz.R;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;


/**
 * Custom dialog for choosing puzzles to download.
 */
public class DownloadPickerDialogBuilder {
    private static final Logger LOGGER = Logger.getLogger(DownloadPickerDialogBuilder.class.getCanonicalName());
    private Activity mActivity;
    private Dialog mDialog;
    private List<Downloader> mAvailableDownloaders;
    private OnDateChangedListener dateChangedListener = new DatePicker.OnDateChangedListener() {
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                LOGGER.info("OnDateChanged " + year + " " + monthOfYear + " " + dayOfMonth);
                downloadDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
                updateDayOfWeek();
                updatePuzzleSelect();
            }
        };

    private Downloaders downloaders;
    private Spinner mPuzzleSelect;
    private LocalDate downloadDate;
    private int selectedItemPosition = 0;
    private final TextView dayOfWeek;

    public DownloadPickerDialogBuilder(Activity a, final OnDownloadSelectedListener downloadButtonListener, int year,
        int monthOfYear, int dayOfMonth, Downloaders downloaders) {
        mActivity = a;

        downloadDate = LocalDate.of(year, monthOfYear,  dayOfMonth);

        this.downloaders = downloaders;

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.download_dialog, (ViewGroup) mActivity.findViewById(R.id.download_root));


        final DatePicker datePicker = layout.findViewById(R.id.datePicker);
        dayOfWeek = layout.findViewById(R.id.dayOfWeek);
        updateDayOfWeek();

        datePicker.init(year, monthOfYear - 1, dayOfMonth, dateChangedListener);

        mPuzzleSelect = layout.findViewById(R.id.puzzleSelect);
        mPuzzleSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               selectedItemPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedItemPosition = 0;
            }
        });
        updatePuzzleSelect();

        OnClickListener clickHandler = new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dateChangedListener.onDateChanged(datePicker, datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    downloadButtonListener.onDownloadSelected(getCurrentDate(), mAvailableDownloaders,
                           selectedItemPosition);
                }
            };

        AlertDialog.Builder builder
            = new AlertDialog.Builder(mActivity)
                .setPositiveButton("Download", clickHandler)
                .setNegativeButton("Cancel", null);

        builder.setView(layout);
        mDialog = builder.create();
        mDialog.setOnShowListener(new OnShowListener() {
                public void onShow(DialogInterface arg0) {
                    updatePuzzleSelect();
                }
            });
    }

    private void updateDayOfWeek() {
        if (dayOfWeek == null) return;

        String dayName
            = downloadDate.getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, Locale.getDefault());
        dayOfWeek.setText(dayName);
    }

    public Dialog getInstance() {
        return mDialog;
    }

	private LocalDate getCurrentDate() {
        return downloadDate;
    }


    private void updatePuzzleSelect() {
        mAvailableDownloaders = downloaders.getDownloaders(getCurrentDate());
        mAvailableDownloaders.add(0, new DummyDownloader());

        ArrayAdapter<Downloader> adapter = new ArrayAdapter<Downloader>(mActivity,
                android.R.layout.simple_spinner_item, mAvailableDownloaders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPuzzleSelect.setAdapter(adapter);
    }

    public interface OnDownloadSelectedListener {
        void onDownloadSelected(LocalDate date, List<Downloader> availableDownloaders, int selected);
    }
}
