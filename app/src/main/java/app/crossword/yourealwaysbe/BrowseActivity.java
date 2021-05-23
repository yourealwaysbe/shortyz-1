package app.crossword.yourealwaysbe;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.util.files.Accessor;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.util.files.PuzMetaFile;
import app.crossword.yourealwaysbe.view.CircleProgressBar;
import app.crossword.yourealwaysbe.view.StoragePermissionDialog;
import app.crossword.yourealwaysbe.view.recycler.RemovableRecyclerViewAdapter;
import app.crossword.yourealwaysbe.view.recycler.SeparatedRecyclerViewAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class BrowseActivity extends ForkyzActivity {
    private static final int REQUEST_WRITE_STORAGE = 1002;

    // allow import of all docs (parser will take care of detecting if it's a
    // puzzle that's recognised)
    private static final String IMPORT_MIME_TYPE =  "*/*";

    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivity.class.getCanonicalName());

    private BrowseActivityViewModel model;

    private DirHandle archiveFolder
        = getFileHandler().getArchiveDirectory();
    private DirHandle crosswordsFolder
        = getFileHandler().getCrosswordsDirectory();

    private Accessor accessor = Accessor.DATE_DESC;
    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
        currentAdapter = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView puzzleList;
    private NotificationManager nm;
    private boolean hasWritePermissions;
    private SpeedDialView buttonAdd;
    private Set<PuzMetaFile> selected = new HashSet<>();
    private MenuItem viewCrosswordsArchiveMenuItem;
    private View pleaseWaitView;
    private Uri pendingImport;

    ActivityResultLauncher<String> getImportURI = registerForActivityResult(
        new GetContent(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri uri) {
                onImportURI(uri, false);
            }
        }
    );

    private ActionMode actionMode;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.browse_action_bar_menu, menu);

            if (model.getIsViewArchive()) {
                menu.findItem(R.id.browse_action_archive)
                    .setVisible(false);
            } else {
                menu.findItem(R.id.browse_action_unarchive)
                    .setVisible(false);
            }

            for (int i = 0; i < menu.size(); i++) {
                utils.onActionBarWithText(menu.getItem(i));
            }

            setSpeedDialVisibility(View.GONE);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(
            ActionMode actionMode, MenuItem menuItem
        ) {
            int id = menuItem.getItemId();

            Set<PuzMetaFile> toAction = new HashSet<>(selected);

            if (id == R.id.browse_action_delete) {
                model.deletePuzzles(toAction);
            } else if (id == R.id.browse_action_archive) {
                model.movePuzzles(toAction, archiveFolder);
            } else if (id == R.id.browse_action_unarchive) {
                model.movePuzzles(toAction, crosswordsFolder);
            }

            actionMode.finish();

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelection();
            setSpeedDialVisibility(View.VISIBLE);
            actionMode = null;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browse_menu, menu);

        if (utils.isNightModeAvailable()) {
            MenuItem item = menu.findItem(R.id.browse_menu_app_theme);
            if (item != null) item.setIcon(getNightModeIcon());
        } else {
            menu.removeItem(R.id.browse_menu_app_theme);
        }

        viewCrosswordsArchiveMenuItem
            = menu.findItem(R.id.browse_menu_archives);

        setViewCrosswordsOrArchiveUI();

        return true;
    }

    private int getNightModeIcon() {
        switch (nightMode.getCurrentMode()) {
        case DAY: return R.drawable.day_mode;
        case NIGHT: return R.drawable.night_mode;
        case SYSTEM: return R.drawable.system_daynight_mode;
        }
        return R.drawable.day_mode;
    }

    private void setListItemColor(View v, boolean selected){
        v.setSelected(selected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.browse_menu_app_theme) {
            this.utils.nextNightMode(this);
            item.setIcon(getNightModeIcon());
            return true;
        } else if (id == R.id.browse_menu_settings) {
            Intent settingsIntent = new Intent(this, PreferencesActivity.class);
            this.startActivity(settingsIntent);
            return true;
        } else if (id == R.id.browse_menu_archives) {
            startLoadPuzzleList(!model.getIsViewArchive());
            return true;
        } else if (id == R.id.browse_menu_cleanup) {
            model.cleanUpPuzzles();
            return true;
        } else if (id == R.id.browse_menu_help) {
            Intent helpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/filescreen.html"), this,
                    HTMLActivity.class);
            this.startActivity(helpIntent);
            return true;
        } else if (id == R.id.browse_menu_sort_source) {
            this.accessor = Accessor.SOURCE;
            prefs.edit()
                 .putInt("sort", 2)
                 .apply();
            this.loadPuzzleAdapter();
            return true;
        } else if (id == R.id.browse_menu_sort_date_asc) {
            this.accessor = Accessor.DATE_ASC;
            prefs.edit()
                 .putInt("sort", 1)
                 .apply();
            this.loadPuzzleAdapter();
            return true;
        } else if (id == R.id.browse_menu_sort_date_desc) {
            this.accessor = Accessor.DATE_DESC;
            prefs.edit()
                 .putInt("sort", 0)
                 .apply();
            this.loadPuzzleAdapter();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        this.setContentView(R.layout.browse);
        this.puzzleList = (RecyclerView) this.findViewById(R.id.puzzleList);
        this.puzzleList.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper helper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.START | ItemTouchHelper.END
            ) {
                @Override
                public int getSwipeDirs(
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder
                ) {
                    if (!(viewHolder instanceof FileViewHolder)
                            || prefs.getBoolean("disableSwipe", false)
                            || !selected.isEmpty()) {
                        return 0; // Don't swipe the headers.
                    }
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }

                @Override
                public boolean onMove(
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder viewHolder1
                ) {
                    return false;
                }

                @Override
                public void onSwiped(
                    RecyclerView.ViewHolder viewHolder, int direction
                ) {
                    if(!selected.isEmpty())
                        return;
                    if(!(viewHolder instanceof FileViewHolder))
                        return;

                    PuzMetaFile puzMeta
                        = ((FileViewHolder) viewHolder).getPuzMetaFile();

                    boolean delete = "DELETE".equals(
                        prefs.getString("swipeAction", "DELETE")
                    );
                    if (delete) {
                        model.deletePuzzle(puzMeta);
                    } else {
                        if (model.getIsViewArchive()) {
                            model.movePuzzle(puzMeta, crosswordsFolder);
                        } else {
                            model.movePuzzle(puzMeta, archiveFolder);
                        }
                    }
                }
            });
        helper.attachToRecyclerView(this.puzzleList);
        upgradePreferences();
        this.nm = (NotificationManager)
            this.getSystemService(Context.NOTIFICATION_SERVICE);

        switch (prefs.getInt("sort", 0)) {
        case 2:
            this.accessor = Accessor.SOURCE;
            break;
        case 1:
            this.accessor = Accessor.DATE_ASC;
            break;
        default:
            this.accessor = Accessor.DATE_DESC;
        }

        buttonAdd = findViewById(R.id.speed_dial_add);
        setupSpeedDial();

        if (ForkyzApplication.getInstance().isMissingWritePermission()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogFragment dialog = new StoragePermissionDialog();
                Bundle args = new Bundle();
                args.putInt(
                    StoragePermissionDialog.RESULT_CODE_KEY,
                    REQUEST_WRITE_STORAGE
                );
                dialog.setArguments(args);
                dialog.show(
                    getSupportFragmentManager(), "StoragePermissionDialog"
                );
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_STORAGE);
            }

            return;
        } else {
            hasWritePermissions = true;
        }

        SwipeRefreshLayout swipePuzzleReloadView
            = findViewById(R.id.swipeContainer);

        model = new ViewModelProvider(this).get(BrowseActivityViewModel.class);
        model.getPuzzleFiles().observe(this, (v) -> {
            BrowseActivity.this.setViewCrosswordsOrArchiveUI();
            BrowseActivity.this.loadPuzzleAdapter();
            swipePuzzleReloadView.setRefreshing(false);
        });

        pleaseWaitView = findViewById(R.id.please_wait_notice);
        model.getIsUIBusy().observe(this, (isBusy) -> {
            if (isBusy)
                showPleaseWait();
            else
                hidePleaseWait();
        });

        model.getPuzzleLoadEvents().observe(this, (v) -> {
            Intent i = new Intent(BrowseActivity.this, PlayActivity.class);
            BrowseActivity.this.startActivity(i);
        });

        swipePuzzleReloadView.setOnRefreshListener(
             new SwipeRefreshLayout.OnRefreshListener() {
                 @Override
                 public void onRefresh() {
                     startLoadPuzzleList();
                 }
             }
         );

        setViewCrosswordsOrArchiveUI();
        // populated properly inside onResume or with puzzle list
        // observer
        setPuzzleListAdapter(buildEmptyList(), false);

        // If this was started by a file open
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            // loaded by onResume
            setPendingImport(intent.getData());
        }
    }

    private void setViewCrosswordsOrArchiveUI() {
        boolean viewArchive = model.getIsViewArchive();
        if (viewCrosswordsArchiveMenuItem != null) {
            viewCrosswordsArchiveMenuItem.setTitle(viewArchive
                ? BrowseActivity.this.getString(R.string.title_view_crosswords)
                : BrowseActivity.this.getString(R.string.title_view_archives)
            );
        }
        this.setTitle(viewArchive
            ? BrowseActivity.this.getString(R.string.title_view_archives)
            : BrowseActivity.this.getString(R.string.title_view_crosswords)
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasWritePermissions = true;
                }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // A background update will commonly happen when the user turns
        // on the preference for the first time, so check here to ensure
        // the UI is re-rendered when they exit the settings dialog.
        if (model.getPuzzleFiles().getValue() == null
                || utils.checkBackgroundDownload(prefs, hasWritePermissions)) {

            if (hasPendingImport()) {
                Uri importUri = getPendingImport();
                clearPendingImport();
                onImportURI(importUri, true);

                // won't be triggered by import if archive is shown
                if (model.getIsViewArchive())
                    startLoadPuzzleList();
            } else {
                startLoadPuzzleList();
            }
        } else {
            refreshLastAccessedPuzzle();
        }

        // previous game ended for now
        ForkyzApplication.getInstance().clearBoard();

        checkDownload();
    }

    private void refreshLastAccessedPuzzle() {
        final PuzHandle lastAccessed
            = ForkyzApplication.getInstance().getPuzHandle();
        if (lastAccessed == null)
            return;
        model.refreshPuzzleMeta(lastAccessed);
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
    buildEmptyList() {
        return new SeparatedRecyclerViewAdapter<>(
            R.layout.puzzle_list_header,
            FileViewHolder.class
        );
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
    buildList(List<PuzMetaFile> puzFiles, Accessor accessor) {
        try {
            Collections.sort(puzFiles, accessor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter> adapter
            = new SeparatedRecyclerViewAdapter<>(
                R.layout.puzzle_list_header,
                FileViewHolder.class
            );
        String lastHeader = null;
        ArrayList<PuzMetaFile> current = new ArrayList<PuzMetaFile>();

        for (PuzMetaFile puzMeta : puzFiles) {
            String check = accessor.getLabel(puzMeta);

            if (!((lastHeader == null) || lastHeader.equals(check))) {
                FileAdapter fa = new FileAdapter(current);
                adapter.addSection(lastHeader, fa);
                current = new ArrayList<PuzMetaFile>();
            }

            lastHeader = check;
            current.add(puzMeta);
        }

        if (lastHeader != null) {
            FileAdapter fa = new FileAdapter(current);
            adapter.addSection(lastHeader, fa);
            current = new ArrayList<PuzMetaFile>();
        }

        return adapter;
    }

    private void checkDownload() {
        if (!hasWritePermissions) return;

        long lastDL = prefs.getLong("dlLast", 0);

        if (prefs.getBoolean("dlOnStartup", false) &&
                ((System.currentTimeMillis() - (long) (12 * 60 * 60 * 1000)) > lastDL)) {
            model.download(LocalDate.now(), null, true);
            prefs.edit()
                    .putLong("dlLast", System.currentTimeMillis())
                    .apply();
        }
    }

    private void startLoadPuzzleList() {
        startLoadPuzzleList(model.getIsViewArchive());
    }

    private void startLoadPuzzleList(boolean archive) {
        if (!hasWritePermissions) return;

        utils.clearBackgroundDownload(prefs);

        model.startLoadFiles(archive);
    }

    private void loadPuzzleAdapter() {
        List<PuzMetaFile> puzList = model.getPuzzleFiles().getValue();
        if (puzList != null) {
            setPuzzleListAdapter(buildList(puzList, accessor), true);
        } else {
            setPuzzleListAdapter(buildEmptyList(), true);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void upgradePreferences() {
        // do nothing now no keyboard
    }

    public void onItemClick(final View v, final PuzMetaFile puzMeta) {
        if (!selected.isEmpty()) {
            updateSelection(v, puzMeta);
        } else {
            if (puzMeta == null)
                return;
            model.loadPuzzle(puzMeta);
        }
    }

    public void onItemLongClick(View v, PuzMetaFile puzMeta) {
        if (actionMode == null) {
            startSupportActionMode(actionModeCallback);
        }
        updateSelection(v, puzMeta);
    }

    private void updateSelection(View v, PuzMetaFile puzMeta) {
        if (selected.contains(puzMeta)) {
            setListItemColor(v, false);
            selected.remove(puzMeta);
        } else {
            setListItemColor(v, true);
            selected.add(puzMeta);
        }
        if (selected.isEmpty() && actionMode != null) {
            actionMode.finish();
        }
    }

    private void clearSelection() {
        selected.clear();
        currentAdapter.notifyDataSetChanged();
    }

    private boolean hasCurrentPuzzleListAdapter() {
        return currentAdapter != null;
    }

    /**
     * Set the puzzle list adapter
     * @param showEmptyMsgs give feedback to user when no files (used to
     * avoid doing so during loading)
     */
    private void setPuzzleListAdapter(
        SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter> adapter,
        boolean showEmptyMsgs
    ) {
        currentAdapter = adapter;
        puzzleList.setAdapter(adapter);

        TextView emptyMsg = findViewById(R.id.empty_listing_msg);
        TextView storageMsg = findViewById(R.id.internal_storage_msg);

        if (adapter.isEmpty() && showEmptyMsgs) {
            if (model.getIsViewArchive()) {
                emptyMsg.setText(R.string.no_puzzles);
            } else {
                emptyMsg.setText(
                    R.string.no_puzzles_download_or_configure_storage
                );
            }
            emptyMsg.setVisibility(View.VISIBLE);

            if (ForkyzApplication.getInstance().isInternalStorage())
                storageMsg.setVisibility(View.VISIBLE);
            else
                storageMsg.setVisibility(View.GONE);
        } else {
            emptyMsg.setVisibility(View.GONE);
            storageMsg.setVisibility(View.GONE);
        }
    }

    private void showPleaseWait() {
        pleaseWaitView.setVisibility(View.VISIBLE);
    }

    private void hidePleaseWait() {
        pleaseWaitView.setVisibility(View.GONE);
    }

    private void setSpeedDialVisibility(int visibility) {
        buttonAdd.setVisibility(visibility);
    }

    private void setupSpeedDial() {
        buttonAdd.inflate(R.menu.speed_dial_browse_menu);

        buttonAdd.setOnActionSelectedListener(
            new SpeedDialView.OnActionSelectedListener() {
                @Override
                public boolean onActionSelected(
                    SpeedDialActionItem actionItem
                ) {
                    int id = actionItem.getId();
                    if (id == R.id.speed_dial_download) {
                        buttonAdd.close();
                        DialogFragment dialog = new DownloadDialog();
                        dialog.show(
                            getSupportFragmentManager(),
                            "DownloadDialog"
                        );
                        return true;
                    } else if (id == R.id.speed_dial_import) {
                        getImportURI.launch(IMPORT_MIME_TYPE);
                    } else if (id == R.id.speed_dial_online_sources) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(
                            Uri.parse(getString(R.string.online_sources_url))
                        );
                        startActivity(i);
                    }
                    return false;
                }
            }
        );

        setSpeedDialVisibility(View.VISIBLE);
    }

    /**
     * Import from URI, force reload of puz list if asked
     */
    private void onImportURI(Uri uri, boolean forceReload) {
        if (uri != null)
            model.importURI(uri, forceReload);
    }

    private boolean hasPendingImport() {
        return pendingImport != null;
    }

    private Uri getPendingImport() {
        return pendingImport;
    }

    private void clearPendingImport() {
        pendingImport = null;
    }

    private void setPendingImport(Uri uri) {
        pendingImport = uri;
    }

    private class FileAdapter extends RemovableRecyclerViewAdapter<FileViewHolder> {
        final DateTimeFormatter df
            = DateTimeFormatter.ofPattern("EEEE\n MMM dd, yyyy");
        final ArrayList<PuzMetaFile> objects;

        public FileAdapter(ArrayList<PuzMetaFile> objects) {
            this.objects = objects;
        }

        public PuzMetaFile getPuzMetaFile(int index) {
            return objects.get(index);
        }

        public void setPuzMetaFile(int index, PuzMetaFile puzMeta) {
            objects.set(index, puzMeta);
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.puzzle_list_item, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            View view = holder.itemView;
            PuzMetaFile pm = objects.get(position);

            holder.setPuzMetaFile(pm);

            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    BrowseActivity.this.onItemClick(view, pm);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {notifyDataSetChanged();
                    BrowseActivity.this.onItemLongClick(view, pm);
                    return true;
                }
            });

            TextView date = (TextView) view.findViewById(R.id.puzzle_date);

            date.setText(df.format(pm.getDate()));

            if (accessor == Accessor.SOURCE) {
                date.setVisibility(View.VISIBLE);
            } else {
                date.setVisibility(View.GONE);
            }

            TextView title = (TextView) view.findViewById(R.id.puzzle_name);

            title.setText(pm.getTitle());

            CircleProgressBar bar = (CircleProgressBar) view.findViewById(R.id.puzzle_progress);

            bar.setPercentFilled(pm.getFilled());
            bar.setComplete(pm.getComplete() == 100);

            TextView caption = (TextView) view.findViewById(R.id.puzzle_caption);

            caption.setText(pm.getCaption());

            setListItemColor(view, selected.contains(pm));
        }

        @Override
        public int getItemCount() {
            return objects.size();
        }

        @Override
        public void remove(int position) {
            objects.remove(position);
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        private PuzMetaFile puzMetaFile;

        public FileViewHolder(View itemView) {
            super(itemView);
        }

        public void setPuzMetaFile(PuzMetaFile puzMetaFile) {
            this.puzMetaFile = puzMetaFile;
        }

        public PuzMetaFile getPuzMetaFile() {
            return puzMetaFile;
        }
    }

    public static class DownloadDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DownloadPickerDialogBuilder.OnDownloadSelectedListener downloadButtonListener = new DownloadPickerDialogBuilder.OnDownloadSelectedListener() {
                public void onDownloadSelected(
                    LocalDate d,
                    List<Downloader> downloaders,
                    int selected
                ) {
                    List<Downloader> toDownload
                        = new LinkedList<Downloader>();
                    boolean scrape;

                    if (selected == 0) {
                        // Download all available.
                        toDownload.addAll(downloaders);
                        toDownload.remove(0);
                        scrape = true;
                    } else {
                        // Only download selected.
                        toDownload.add(downloaders.get(selected));
                        scrape = false;
                    }

                    BrowseActivityViewModel model
                        = new ViewModelProvider(getActivity())
                            .get(BrowseActivityViewModel.class);

                    model.download(d, toDownload, scrape);
                }
            };

            LocalDate d = LocalDate.now();
            BrowseActivity activity = (BrowseActivity) getActivity();

            DownloadPickerDialogBuilder dpd
                = new DownloadPickerDialogBuilder(
                    activity,
                    downloadButtonListener,
                    d.getYear(),
                    d.getMonthValue(),
                    d.getDayOfMonth(),
                    new Downloaders(activity.prefs, activity.nm, activity)
            );

            return dpd.getInstance();
        }
    }
}
