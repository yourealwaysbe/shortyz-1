
package app.crossword.yourealwaysbe.util.files;

import java.time.LocalDate;
import java.util.AbstractList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

public class MetaCache {

    public static class Converters {
        @TypeConverter
        public static LocalDate fromEpochDay(Long value) {
            return value == null ? null : LocalDate.ofEpochDay(value);
        }

        @TypeConverter
        public static Long dateToEpochDay(LocalDate date) {
            return date == null ? null : date.toEpochDay();
        }

        @TypeConverter
        public static Uri fromUriString(String value) {
            return value == null ? null : Uri.parse(value);
        }

        @TypeConverter
        public static String uriToString(Uri uri) {
            return uri == null ? null : uri.toString();
        }
    }

    @Entity(tableName = "cachedMeta")
    @TypeConverters({Converters.class})
    public static class CachedMeta {
        @PrimaryKey
        @NonNull
        public Uri mainFileUri;

        // currently unused but may be used to speed up dir listing in
        // browse activity
        @ColumnInfo
        public Uri metaFileUri;

        @ColumnInfo
        @NonNull
        public Uri directoryUri;

        @ColumnInfo
        public boolean isUpdatable;

        @ColumnInfo
        public LocalDate date;

        @ColumnInfo
        public int percentComplete;

        @ColumnInfo
        public int percentFilled;

        @ColumnInfo
        public String source;

        @ColumnInfo
        public String title;

        @ColumnInfo(defaultValue = "0")
        public boolean isDummy;
    }

    @Dao
    @TypeConverters({Converters.class})
    public static interface CachedMetaDao {
        @Query("SELECT * FROM cachedMeta WHERE directoryUri = :directory")
        public List<CachedMeta> getDirCache(Uri directory);

        @Query("SELECT * FROM cachedMeta WHERE mainFileUri = :mainFileUri")
        public CachedMeta getCache(Uri mainFileUri);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public void insertAll(CachedMeta... metas);

        @Query(
            "DELETE FROM cachedMeta" +
            " WHERE directoryUri = :dirUri" +
            "  AND mainFileUri NOT IN (:mainFileUris)")
        public void deleteOutside(Uri dirUri, List<Uri> mainFileUris);

        @Query("DELETE FROM cachedMeta WHERE mainFileUri IN (:mainFileUris)")
        public void delete(Uri... mainFileUris);

        @Query("DELETE FROM cachedMeta")
        public void deleteAll();
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE cachedMeta" +
                " ADD COLUMN isDummy INTEGER NOT NULL" +
                " DEFAULT 0"
            );
        }
    };

    @Database(entities = {CachedMeta.class}, version = 2)
    public static abstract class CachedMetaDB extends RoomDatabase {
        private static CachedMetaDB instance = null;

        public static CachedMetaDB getInstance(Context applicationContext) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    applicationContext,
                    CachedMetaDB.class,
                    "meta-cache-db"
                ).addMigrations(MIGRATION_1_2)
                    .build();
            }
            return instance;
        }

        public abstract CachedMetaDao cachedMetaDao();
    }

    public class MetaRecord {
        private CachedMeta dbRow;

        public MetaRecord(CachedMeta dbRow) {
            this.dbRow = dbRow;
        }

        public boolean isUpdatable() { return dbRow.isUpdatable; }
        public String getCaption() { return dbRow.title; }
        public LocalDate getDate() { return dbRow.date; }
        public int getPercentComplete() { return dbRow.percentComplete; }
        public int getPercentFilled() { return dbRow.percentFilled; }
        public String getSource() { return dbRow.source; }
        public String getTitle() { return dbRow.title; }
        public boolean isDummy() { return dbRow.isDummy; }
    }

    private Context applicationContext;
    private FileHandler fileHandler;

    public MetaCache(Context applicationContext, FileHandler fileHandler) {
        this.applicationContext = applicationContext;
        this.fileHandler = fileHandler;
    }

    /**
     * Returns all cached meta data records for the given directory
     */
    public Map<Uri, MetaRecord> getDirCache(DirHandle dirHandle) {
        Uri dirUri = fileHandler.getUri(dirHandle);
        Map<Uri, MetaRecord> cache = new HashMap<>();
        for (CachedMeta cm : getDao().getDirCache(dirUri)) {
            cache.put(cm.mainFileUri, new MetaRecord(cm));
        }
        return  cache;
    }

    /**
     * Return cached meta for given handle
     *
     * @return null if no entry in cache
     */
    public MetaRecord getCache(PuzHandle puzHandle) {
        Uri puzFileUri = fileHandler.getUri(puzHandle.getPuzFileHandle());
        CachedMeta cm = getDao().getCache(puzFileUri);
        return (cm == null) ? null : new MetaRecord(cm);
    }

    /**
     * Cache meta for a file URI, returns new record
     */
    public MetaRecord addRecord(PuzHandle puzHandle, PuzzleMeta meta) {
        CachedMeta cm = new CachedMeta();
        cm.mainFileUri = fileHandler.getUri(puzHandle.getPuzFileHandle());
        FileHandle mfh = puzHandle.getMetaFileHandle();
        if (mfh != null)
            cm.metaFileUri = fileHandler.getUri(mfh);
        cm.directoryUri = fileHandler.getUri(puzHandle.getDirHandle());
        cm.isUpdatable = meta.updatable;
        cm.date = meta.date;
        cm.percentComplete = meta.percentComplete;
        cm.percentFilled = meta.percentFilled;
        cm.source = meta.source;
        cm.title = meta.title;
        cm.isDummy = false;
        getDao().insertAll(cm);
        return new MetaRecord(cm);
    }

    /**
     * Cache meta for a file URI, returns new record
     */
    public MetaRecord addRecord(PuzHandle puzHandle, Puzzle puz) {
        CachedMeta cm = new CachedMeta();
        cm.mainFileUri = fileHandler.getUri(puzHandle.getPuzFileHandle());
        FileHandle mfh = puzHandle.getMetaFileHandle();
        if (mfh != null)
            cm.metaFileUri = fileHandler.getUri(mfh);
        cm.directoryUri = fileHandler.getUri(puzHandle.getDirHandle());
        cm.isUpdatable = puz.isUpdatable();
        cm.date = puz.getDate();
        cm.percentComplete = puz.getPercentComplete();
        cm.percentFilled = puz.getPercentFilled();
        cm.source = puz.getSource();
        cm.title = puz.getTitle();
        cm.isDummy = false;
        getDao().insertAll(cm);
        return new MetaRecord(cm);
    }

     /**
     * Create a dummy cache for a file without meta
     *
     * @param lastModified a good estimate of the last modified time,
     * usually the last modified time of the main puz file
     */
    public MetaRecord addDummyRecord(
        PuzHandle puzHandle, LocalDate lastModified
    ) {
        CachedMeta cm = new CachedMeta();
        cm.mainFileUri = fileHandler.getUri(puzHandle.getPuzFileHandle());
        FileHandle mfh = puzHandle.getMetaFileHandle();
        if (mfh != null)
            cm.metaFileUri = fileHandler.getUri(mfh);
        cm.directoryUri = fileHandler.getUri(puzHandle.getDirHandle());
        cm.isUpdatable = false;
        cm.date = lastModified;
        cm.percentComplete = 0;
        cm.percentFilled = 0;
        cm.source = applicationContext.getString(R.string.unknown_source);
        cm.title = null;
        cm.isDummy = true;
        getDao().insertAll(cm);
        return new MetaRecord(cm);
    }

    /**
     * Remove a record from the cache
     */
    public void deleteRecord(PuzHandle puzHandle) {
        getDao().delete(fileHandler.getUri(puzHandle.getPuzFileHandle()));
    }

    /**
     * Remove all records from directory that are not in the handles
     */
    public void cleanupCache(
        DirHandle dir, List<PuzMetaFile> puzMetaFiles
    ) {
        Uri dirUri = fileHandler.getUri(dir);
        getDao().deleteOutside(dirUri, new AbstractList<Uri>() {
            public int size() {
                return puzMetaFiles.size();
            }

            public Uri get(int i) {
                PuzMetaFile pm = puzMetaFiles.get(i);
                return fileHandler.getUri(
                    pm.getPuzHandle().getPuzFileHandle()
                );
            }
        });
    }

    private CachedMetaDao getDao() {
        return CachedMetaDB.getInstance(applicationContext).cachedMetaDao();
    }
}
