
package app.crossword.yourealwaysbe.util.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import app.crossword.yourealwaysbe.io.IPuzIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

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
    // Android messes with application/json by adding .json extension :(
    private static final String MIME_TYPE_IPUZ = "application/octet-stream";

    public static final String MIME_TYPE_PLAIN_TEXT = "text/plain";
    public static final String MIME_TYPE_GENERIC = "application/octet-stream";
    public static final String MIME_TYPE_GENERIC_XML = "text/xml";

    public static final String FILE_EXT_PUZ = ".puz";
    public static final String FILE_EXT_FORKYZ = ".forkyz";
    public static final String FILE_EXT_IPUZ = ".ipuz";

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

    /**
     * Get output stream to file, erasing previous contents
     */
    protected abstract OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException;

    protected abstract InputStream getInputStream(FileHandle fileHandle)
        throws IOException;

    public Uri getUri(PuzHandle puzHandle) {
        return getUri(puzHandle.getMainFileHandle());
    }

    public String getName(PuzHandle puzHandle) {
        return getName(puzHandle.getMainFileHandle());
    }

    public boolean exists(PuzMetaFile pm) {
        return exists(pm.getPuzHandle());
    }

    public boolean exists(PuzHandle ph) {

        if (!exists(ph.getMainFileHandle()))
            return false;

        boolean exists = true;

        exists &= ph.accept(new PuzHandle.Visitor<Boolean>() {
            @Override
            public Boolean visit(PuzHandle.Puz puzHandle) {
                FileHandle metaHandle = puzHandle.getMetaFileHandle();
                boolean exists = true;
                if (metaHandle != null) {
                    exists = exists(metaHandle);
                }
                return exists;
            }
            @Override
            public Boolean visit(PuzHandle.IPuz ipuzHandle) {
                return true;
            }
        });

        return exists;
    }

    public synchronized void delete(PuzMetaFile pm) {
        delete(pm.getPuzHandle());
    }

    public synchronized void delete(PuzHandle ph) {
        delete(ph.getMainFileHandle());

        ph.accept(new PuzHandle.Visitor<Void>() {
            @Override
            public Void visit(PuzHandle.Puz puzHandle) {
                FileHandle metaHandle = puzHandle.getMetaFileHandle();
                if (metaHandle != null)
                    delete(metaHandle);
                return null;
            }
            @Override
            public Void visit(PuzHandle.IPuz ipuzHandle) {
                return null;
            }
        });

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

        moveTo(ph.getMainFileHandle(), srcDirHandle, destDirHandle);

        ph.setDirectory(destDirHandle);

        ph.accept(new PuzHandle.Visitor<Void>() {
            @Override
            public Void visit(PuzHandle.Puz puzHandle) {
                FileHandle metaHandle = puzHandle.getMetaFileHandle();
                if (metaHandle != null)
                    moveTo(metaHandle, srcDirHandle, destDirHandle);
                return null;
            }
            @Override
            public Void visit(PuzHandle.IPuz ipuzHandle) {
                return null;
            }
        });

        // TODO: can we move record instead? What is new Uri?
        metaCache.deleteRecord(ph);
    }

    /**
     * Get puz files in directory, will create meta files when missing
     */
    public List<PuzMetaFile> getPuzMetas(DirHandle dirHandle) {
        ArrayList<PuzMetaFile> metas = new ArrayList<>();

        Iterable<FileHandle> rawFileList = listFiles(dirHandle);

        Map<Uri, MetaCache.MetaRecord> cachedMetas
            = metaCache.getDirCache(dirHandle);

        loadMetasFromPuzFiles(dirHandle, rawFileList, cachedMetas, metas);
        loadMetasFromIPuzFiles(dirHandle, rawFileList, cachedMetas, metas);

        metaCache.cleanupCache(dirHandle, metas);

        return metas;
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
     *
     * @return null if could not be loaded
     */
    public PuzMetaFile loadPuzMetaFile(PuzHandle puzHandle) throws IOException {
        Puzzle puz = load(puzHandle);

        if (puz == null)
            return null;

        MetaCache.MetaRecord metaRecord = metaCache.addRecord(puzHandle, puz);

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
        Puzzle puz = ph.accept(new PuzHandle.VisitorIO<Puzzle>() {
            @Override
            public Puzzle visit(PuzHandle.Puz puzHandle) throws IOException {
                return load(puzHandle);
            }
            @Override
            public Puzzle visit(PuzHandle.IPuz ipuzHandle) throws IOException {
                return load(ipuzHandle);
            }
        });

        if (puz != null)
            metaCache.addRecord(ph, puz);

        return puz;
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
    public synchronized void save(Puzzle puz, PuzHandle ph)
            throws IOException {

        boolean success = ph.accept(new PuzHandle.VisitorIO<Boolean>() {
            @Override
            public Boolean visit(PuzHandle.Puz puzHandle) throws IOException {
                return save(puz, puzHandle);
            }
            @Override
            public Boolean visit(PuzHandle.IPuz ipuzHandle) throws IOException {
                return save(puz, ipuzHandle);
            }
        });

        // Cannot be done on main thread (and you save puzzles on
        // the main thread).
        if (success) {
            executorService.execute(() -> {
                metaCache.addRecord(ph, puz);
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
     * @return new puzzle handle if saved success
     */
    public synchronized PuzHandle saveNewPuzzle(
        Puzzle puz, String fileNameBody
    ) throws IOException {
        DirHandle dirHandle = getCrosswordsDirectory();

        FileHandle mainFile = createFileHandle(
            dirHandle, fileNameBody + FILE_EXT_IPUZ, MIME_TYPE_IPUZ
        );

        if (mainFile == null)
            return null;

        try {
            PuzHandle ph = new PuzHandle.IPuz(dirHandle, mainFile);
            save(puz, ph);
            return ph;
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

    private synchronized Puzzle load(PuzHandle.Puz ph) throws IOException {
        FileHandle metaFile = ph.getMetaFileHandle();
        if (metaFile == null) {
            try (
                DataInputStream fis
                    = new DataInputStream(
                        getBufferedInputStream(
                            ph.getMainFileHandle()))
            ) {
                return IO.loadNative(fis);
            }
        } else {
            try (
                DataInputStream pis
                    = new DataInputStream(
                        getBufferedInputStream(
                            ph.getMainFileHandle()));
                DataInputStream mis
                    = new DataInputStream(
                        getBufferedInputStream(
                            ph.getMetaFileHandle()))
            ) {
                return IO.load(pis, mis);
            }
        }
    }

    private synchronized Puzzle load(PuzHandle.IPuz ph) throws IOException {
        try (
            InputStream is = getBufferedInputStream(ph.getMainFileHandle())
        ) {
            return IPuzIO.readPuzzle(is);
        }
    }

    private synchronized boolean save(Puzzle puz, PuzHandle.Puz ph)
            throws IOException {
        FileHandle puzFile = ph.getMainFileHandle();
        FileHandle metaFile = ph.getMetaFileHandle();
        DirHandle puzDir = ph.getDirHandle();

        boolean metaCreated = false;

        if (metaFile == null) {
            String metaName = getMetaFileName(puzFile);
            metaFile = createFileHandle(
                ph.getDirHandle(), metaName, MIME_TYPE_META
            );
            if (metaFile == null)
                throw new IOException("Could not create meta file");
            metaCreated = true;
        }

        // Write to buffers first so that we don't erase old data in
        // case of a problem encoding puzzle.
        ByteArrayOutputStream baosPuz = new ByteArrayOutputStream();
        ByteArrayOutputStream baosMeta = new ByteArrayOutputStream();

        try (
            DataOutputStream dosPuz = new DataOutputStream(baosPuz);
            DataOutputStream dosMeta = new DataOutputStream(baosMeta);
        ) {
            IO.save(puz, dosPuz, dosMeta);
        }

        // Write meta first, if doesn't throw, write main puzzle

        boolean metaSuccess = false;

        try (
            InputStream baisMeta
                = new ByteArrayInputStream(baosMeta.toByteArray());
            OutputStream meta = getBufferedOutputStream(metaFile);
        ) {
            IO.copyStream(baisMeta, meta);
            metaSuccess = true;
        } finally {
            if (!metaSuccess && metaCreated)
                delete(metaFile);
        }

        boolean puzSuccess = false;

        try (
            InputStream baisPuz
                = new ByteArrayInputStream(baosPuz.toByteArray());
            OutputStream puzzle = getBufferedOutputStream(puzFile);
        ) {
            IO.copyStream(baisPuz, puzzle);
            puzSuccess = true;
        } finally {
            if (!puzSuccess && metaCreated)
                delete(metaFile);
            else
                ph.setMetaFileHandle(metaFile);
        }

        return true;
    }

    private synchronized boolean save(Puzzle puz, PuzHandle.IPuz ph)
            throws IOException {
        FileHandle ipuzFile = ph.getMainFileHandle();
        DirHandle puzDir = ph.getDirHandle();

        // Write to buffer first -- get puzzle written to a stream
        // before truncating the previously saved file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IPuzIO.writePuzzle(puz, baos);

        try (
            InputStream bais = new ByteArrayInputStream(baos.toByteArray());
            OutputStream os = getBufferedOutputStream(ipuzFile)
        ) {
            IO.copyStream(bais, os);
            return true;
        }
    }

    /**
     * Load .puz/.forkyz file data into the loadedPuzMetas array
     *
     * @param dirHandle the directory the files are in
     * @param files the list of files in the directory
     * @param cachedMetas cached meta data for the file URIs
     * @param loadedPuzMetas the list into which to store the metas
     */
    private void loadMetasFromPuzFiles(
        DirHandle dirHandle,
        Iterable<FileHandle> files,
        Map<Uri, MetaCache.MetaRecord> cachedMetas,
        List<PuzMetaFile> loadedPuzMetas
    ) {
        // Load files into data structures to avoid repeated interaction
        // with filesystem (which is good for content resolver)
        Set<FileHandle> puzFiles = new HashSet<>();
        Map<String, FileHandle> metaFiles = new HashMap<>();

        for (FileHandle f : files) {
            String fileName = getName(f);
            if (fileName.endsWith(FILE_EXT_PUZ)) {
                puzFiles.add(f);
            } else if (fileName.endsWith(FILE_EXT_FORKYZ)) {
                metaFiles.put(fileName, f);
            } else {
                // ignore
            }
        }

        for (FileHandle puzFile : puzFiles) {
            String metaName = getMetaFileName(puzFile);
            FileHandle metaFile = null;

            if (metaFiles.containsKey(metaName)) {
                metaFile = metaFiles.get(metaName);
            }

            PuzHandle ph = new PuzHandle.Puz(dirHandle, puzFile, metaFile);

            PuzMetaFile pm = getPuzMetaFile(ph, cachedMetas);
            if (pm != null)
                loadedPuzMetas.add(pm);
        }
    }

    /**
     * Load .puz/.forkyz file data into the loadedPuzMetas array
     *
     * @param dirHandle the directory the files are in
     * @param files the list of files in the directory
     * @param cachedMetas cached meta data for the file URIs
     * @param loadedPuzMetas the list into which to store the metas
     */
    private void loadMetasFromIPuzFiles(
        DirHandle dirHandle,
        Iterable<FileHandle> files,
        Map<Uri, MetaCache.MetaRecord> cachedMetas,
        List<PuzMetaFile> loadedPuzMetas
    ) {
        for (FileHandle f : files) {
            String fileName = getName(f);
            if (fileName.endsWith(FILE_EXT_IPUZ)) {
                PuzHandle ph = new PuzHandle.IPuz(dirHandle, f);
                PuzMetaFile pm = getPuzMetaFile(ph, cachedMetas);
                if (pm != null)
                    loadedPuzMetas.add(pm);
            }
        }
    }

    /**
     * Get the PuzMetaFile from the handle
     *
     * Uses cache if possible, else loads data from file
     */
    private PuzMetaFile getPuzMetaFile(
        PuzHandle ph, Map<Uri, MetaCache.MetaRecord> cachedMetas
    ) {
        Uri puzFileUri = getUri(ph);
        MetaCache.MetaRecord metaRecord = cachedMetas.get(puzFileUri);

        if (metaRecord != null) {
            return new PuzMetaFile(ph, metaRecord);
        } else {
            try {
                return loadPuzMetaFile(ph);
            } catch (IOException e) {
                LOGGER.warning("Could not load puz meta for " + ph +": " + e);
                return new PuzMetaFile(ph, null);
            }
        }
    }
}
