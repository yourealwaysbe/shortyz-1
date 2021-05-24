
package app.crossword.yourealwaysbe.util.files;

import java.time.LocalDate;
import java.util.AbstractList;
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

import app.crossword.yourealwaysbe.puz.Puzzle;

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
    }

    @Database(entities = {CachedMeta.class}, version = 1)
    public static abstract class CachedMetaDB extends RoomDatabase {
        private static CachedMetaDB instance = null;

        public static CachedMetaDB getInstance(Context applicationContext) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    applicationContext,
                    CachedMetaDB.class,
                    "meta-cache-db"
                ).build();
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
    public MetaRecord getCache(Uri puzFileUri) {
        CachedMeta cm = getDao().getCache(puzFileUri);
        return (cm == null) ? null : new MetaRecord(cm);
    }

    /**
     * Cache meta for a file URI, returns new record
     */
    public MetaRecord addRecord(PuzHandle puzHandle, Puzzle puz) {
        CachedMeta cm = new CachedMeta();
        cm.mainFileUri = fileHandler.getUri(puzHandle.getPuzFileHandle());

        FileHandle metaHandle = puzHandle.getMetaFileHandle();
        if (metaHandle == null)
            cm.metaFileUri = null;
        else
            cm.metaFileUri = fileHandler.getUri(puzHandle.getMetaFileHandle());

        cm.directoryUri = fileHandler.getUri(puzHandle.getDirHandle());
        cm.isUpdatable = puz.isUpdatable();
        cm.date = puz.getDate();
        cm.percentComplete = puz.getPercentComplete();
        cm.percentFilled = puz.getPercentFilled();
        cm.source = puz.getSource();
        cm.title = puz.getTitle();

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
