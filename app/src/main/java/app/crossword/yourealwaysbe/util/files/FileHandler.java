
package app.crossword.yourealwaysbe.util.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Abstraction layer for file operations
 *
 * Implementations provided for different file backends
 */
public abstract class FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandler.class.getCanonicalName());

    // private for now because downloaders shouldn't be directly
    // creating puzzle files but instead saving Puzzle objects with
    // names
    private static final String MIME_TYPE_PUZ = "application/x-crossword";
    private static final String MIME_TYPE_META = "application/octet-stream";

    public static final String MIME_TYPE_PLAIN_TEXT = "text/plain";
    public static final String MIME_TYPE_GENERIC = "application/octet-stream";
    public static final String MIME_TYPE_GENERIC_XML = "text/xml";

    public static final String FILE_EXT_PUZ = ".puz";
    public static final String FILE_EXT_FORKYZ = ".forkyz";

    // used for saving meta cache to DB since we currently save puzzles
    // on the main thread (can be removed if/when a better save solution
    // is implemented)
    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();

    private Context applicationContext;
    private MetaCache metaCache;

    protected FileHandler(Context applicationContext) {
        this.applicationContext = applicationContext;
        this.metaCache = new MetaCache(applicationContext, this);
    }

    public abstract DirHandle getCrosswordsDirectory();
    public abstract DirHandle getArchiveDirectory();
    public abstract boolean isStorageMounted();
    public abstract boolean isStorageFull();

    protected abstract FileHandle getFileHandle(Uri uri);
    protected abstract boolean exists(DirHandle dir);
    protected abstract boolean exists(FileHandle file);
    protected abstract Iterable<FileHandle> listFiles(final DirHandle dir);
    protected abstract Uri getUri(DirHandle f);
    protected abstract Uri getUri(FileHandle f);
    protected abstract String getName(FileHandle f);
    protected abstract long getLastModified(FileHandle file);
    protected abstract OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException;
    protected abstract InputStream getInputStream(FileHandle fileHandle)
        throws IOException;

    public Uri getUri(PuzHandle puzHandle) {
        return getUri(puzHandle.getPuzFileHandle());
    }

    public String getName(PuzHandle puzHandle) {
        return getName(puzHandle.getPuzFileHandle());
    }

    public boolean exists(PuzMetaFile pm) {
        return exists(pm.getPuzHandle());
    }

    public boolean exists(PuzHandle ph) {
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null) {
            return exists(ph.getPuzFileHandle())
                && exists(ph.getMetaFileHandle());
        } else {
            return exists(ph.getPuzFileHandle());
        }
    }

    public synchronized void delete(PuzMetaFile pm) {
        delete(pm.getPuzHandle());
    }

    public synchronized void delete(PuzHandle ph) {
        delete(ph.getPuzFileHandle());
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            delete(metaHandle);
        metaCache.deleteRecord(ph);
    }

    public synchronized void moveTo(
        PuzMetaFile pm, DirHandle destDirHandle
    ) {
        moveTo(pm.getPuzHandle(), destDirHandle);
    }

    public synchronized void moveTo(
        PuzHandle ph, DirHandle destDirHandle
    ) {
        DirHandle srcDirHandle = ph.getDirHandle();
        moveTo(ph.getPuzFileHandle(), srcDirHandle, destDirHandle);
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            moveTo(metaHandle, srcDirHandle, destDirHandle);
        // TODO: can we move record instead? What is new Uri?
        metaCache.deleteRecord(ph);
    }

    /**
     * Get puz files in directory, will create meta files when missing
     */
    public List<PuzMetaFile> getPuzFiles(DirHandle dirHandle) {
        long start = System.currentTimeMillis();

        ArrayList<PuzMetaFile> files = new ArrayList<>();

        // Load files into data structures to avoid repeated interaction
        // with filesystem (which is good for content resolver)
        Set<FileHandle> puzFiles = new HashSet<>();
        Map<String, FileHandle> metaFiles = new HashMap<>();

        for (FileHandle f : listFiles(dirHandle)) {
            String fileName = getName(f);
            if (fileName.endsWith(FILE_EXT_PUZ)) {
                puzFiles.add(f);
            } else if (fileName.endsWith(FILE_EXT_FORKYZ)) {
                metaFiles.put(fileName, f);
            } else {
                // ignore
            }
        }

        Map<Uri, MetaCache.MetaRecord> cachedMetas
            = metaCache.getDirCache(dirHandle);

        for (FileHandle puzFile : puzFiles) {
            String metaName = getMetaFileName(puzFile);
            FileHandle metaFile = null;

            if (metaFiles.containsKey(metaName)) {
                metaFile = metaFiles.get(metaName);
            }

            PuzHandle ph = new PuzHandle(dirHandle, puzFile, metaFile);

            Uri puzFileUri = getUri(puzFile);
            MetaCache.MetaRecord metaRecord = cachedMetas.get(puzFileUri);

            PuzMetaFile pm = null;
            if (metaRecord != null) {
                pm = new PuzMetaFile(ph, metaRecord);
            } else {
                try {
                    pm = loadPuzMetaFile(ph);
                } catch (IOException e) {
                    LOGGER.warning("Could not load puz meta for " + ph +": " + e);
                    pm = new PuzMetaFile(ph, null);
                }
            }

            files.add(pm);
        }

        metaCache.cleanupCache(dirHandle, files);

        long end = System.currentTimeMillis();

        return files;
    }

    /**
     * Gets the set of puzzle names stored by Forkyz
     *
     * File names are names without the file extension or directories
     */
    public Set<String> getPuzzleNames() {
        Set<String> puzzleNames = new HashSet<>();
        for (FileHandle fh : listFiles(getCrosswordsDirectory())) {
            String puzzleName = getPuzzleFileName(getName(fh));
            if (puzzleName != null)
                puzzleNames.add(puzzleName);
        }
        for (FileHandle fh : listFiles(getArchiveDirectory())) {
            String puzzleName = getPuzzleFileName(getName(fh));
            if (puzzleName != null)
                puzzleNames.add(puzzleName);
        }
        return puzzleNames;
    }

    /**
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    public PuzMetaFile loadPuzMetaFile(PuzHandle puzHandle) throws IOException {
        FileHandle metaHandle = puzHandle.getMetaFileHandle();
        PuzzleMeta meta = null;

        if (metaHandle != null) {
            try (
                DataInputStream is = new DataInputStream(
                    getBufferedInputStream(metaHandle)
                )
            ) {
                meta = IO.readMeta(is);
            }
        }

        MetaCache.MetaRecord metaRecord = null;
        if (meta != null)
            metaRecord = metaCache.addRecord(puzHandle, meta);

        return new PuzMetaFile(puzHandle, metaRecord);
    }

    /**
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    public synchronized Puzzle load(PuzMetaFile pm) throws IOException {
        return load(pm.getPuzHandle());
    }

    /**
     * Loads puzzle with meta
     *
     * If the meta file of puz handle is null, loads without meta
     *
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    public synchronized Puzzle load(PuzHandle ph) throws IOException {
        FileHandle metaFile = ph.getMetaFileHandle();

        if (metaFile == null)
            return load(ph.getPuzFileHandle());

        Puzzle puz = null;

        try (
            DataInputStream pis
                = new DataInputStream(
                    getBufferedInputStream(ph.getPuzFileHandle())
                );
            DataInputStream mis
                = new DataInputStream(
                    getBufferedInputStream(ph.getMetaFileHandle())
                )
        ) {
            puz = IO.load(pis, mis);
        }

        // puz will only be null if there was an exception thrown, but
        // check anyway
        if (puz != null)
            metaCache.addRecord(ph, puz);

        return puz;
    }

    /**
     * Loads without any meta data
     *
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    public synchronized Puzzle load(FileHandle fileHandle) throws IOException {
        // don't update meta cache here as we don't know the dir handle
        // or really have any meta to cache
        try (
            DataInputStream fis
                = new DataInputStream(getBufferedInputStream(fileHandle))
        ) {
            return IO.loadNative(fis);
        }
    }

    public synchronized void save(Puzzle puz, PuzMetaFile puzMeta)
            throws IOException {
        save(puz, puzMeta.getPuzHandle());
    }

    /** Save puzzle and meta data
     *
     * If puzHandle's meta handle is null, a new meta file will be created and
     * puzHandle is updated with the new meta file handle
     *
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    public synchronized void save(Puzzle puz, PuzHandle puzHandle)
            throws IOException {
        FileHandle puzFile = puzHandle.getPuzFileHandle();
        FileHandle metaFile = puzHandle.getMetaFileHandle();
        DirHandle puzDir = puzHandle.getDirHandle();

        boolean success = false;
        boolean metaCreated = false;

        if (metaFile == null) {
            String metaName = getMetaFileName(puzFile);
            metaFile = createFileHandle(
                puzHandle.getDirHandle(), metaName, MIME_TYPE_META
            );
            if (metaFile == null)
                throw new IOException("Could not create meta file");
            metaCreated = true;
        }

        try (
            DataOutputStream puzzle
                = new DataOutputStream(getBufferedOutputStream(puzFile));
            DataOutputStream meta
                = new DataOutputStream(getBufferedOutputStream(metaFile));
        ) {
            IO.save(puz, puzzle, meta);
            success = true;
        } finally {
            if (!success && metaCreated)
                delete(metaFile);
            else
                puzHandle.setMetaFileHandle(metaFile);
        }

        // Cannot be done on main thread (and you save puzzles on
        // the main thread).
        if (success) {
            executorService.execute(() -> {
                metaCache.addRecord(puzHandle, puz);
            });
        }
    }

    /**
     * Save a (new) puzzle to the given directory
     *
     * Use this instead of createFile to save puzzles -- let the file
     * handler decide the backend file format.
     *
     * @param puz the puzzle to save
     * @param dirHandle the directory to save under
     * @param fileNameBody the name to give the file without file
     * extension
     * @return false if the save failed
     */
    public synchronized boolean saveNewPuzzle(
        Puzzle puz, String fileNameBody
    ) throws IOException {
        DirHandle dirHandle = getCrosswordsDirectory();

        FileHandle mainFile = createFileHandle(
            dirHandle, fileNameBody + FILE_EXT_PUZ, MIME_TYPE_PUZ
        );

        if (mainFile == null)
            return false;

        try {
            save(puz, new PuzHandle(dirHandle, mainFile, null));
            return true;
        } catch (Exception e) {
            delete(mainFile);
            throw e;
        }
    }

    protected LocalDate getModifiedDate(FileHandle file) {
        return Instant.ofEpochMilli(getLastModified(file))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
    }

    /**
     * Create a new file in the directory with the given display name
     *
     * Do not save puzzles using these file handles, instead go via
     * save(Puzzle, DirHandle, String) that will use the preferred
     * backend file format for puzzles.
     *
     * Return null if could not be created. E.g. if the file already
     * exists.
     */
    protected abstract FileHandle createFileHandle(
        DirHandle dir, String fileName, String mimeType
    );

    /**
     * Provide a moveTo implementation
     *
     * Assume in a synced class
     */
    protected abstract void moveToUnsync(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    );

    /**
     * Provide a delete implementation
     *
     * Assume in a synced class
     */
    protected abstract void deleteUnsync(FileHandle fileHandle);

    protected BufferedOutputStream getBufferedOutputStream(FileHandle fileHandle)
        throws IOException {
        return new BufferedOutputStream(getOutputStream(fileHandle));
    }

    protected BufferedInputStream getBufferedInputStream(FileHandle fileHandle)
        throws IOException {
        return new BufferedInputStream(getInputStream(fileHandle));
    }

    protected synchronized void delete(FileHandle fileHandle) {
        deleteUnsync(fileHandle);
    }

    protected synchronized void moveTo(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        moveToUnsync(fileHandle, srcDirHandle, destDirHandle);
    }

    protected String getMetaFileName(FileHandle puzFile) {
        String name = getName(puzFile);
        return name.substring(0, name.lastIndexOf(".")) + FileHandler.FILE_EXT_FORKYZ;
    }

    protected Context getApplicationContext() {
        return applicationContext;
    }

    /**
     * Extract puzzle file name from fileName
     *
     * Checks whether the file name is a backend puzzle file (by
     * extension), and returns the name without the extension if so.
     *
     * @return puzzle file name or null if not a puzzle file
     */
    private String getPuzzleFileName(String fileName) {
        if (fileName.endsWith(FILE_EXT_PUZ)) {
            return fileName.substring(
                0, fileName.length() - FILE_EXT_PUZ.length()
            );
        } else {
            return null;
        }
    }
}
