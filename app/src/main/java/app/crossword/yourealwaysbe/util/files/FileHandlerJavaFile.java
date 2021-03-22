
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Base implementation of java.io.File-based file access from a given
 * root directory.
 */
public abstract class FileHandlerJavaFile extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerJavaFile.class.getCanonicalName());

    private File rootDirectory;

    public FileHandlerJavaFile(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public DirHandle getCrosswordsDirectory() {
        File cwDir =
            new File(getRootDirectory(), "crosswords");
        cwDir.mkdirs();
        return new DirHandle(cwDir);
    }

    @Override
    public DirHandle getArchiveDirectory() {
        File arDir = new File(getRootDirectory(), "crosswords/archive");
        arDir.mkdirs();
        return new DirHandle(arDir);
    }

    @Override
    public DirHandle getTempDirectory() {
        File tempDir = new File(getRootDirectory(), "temp");
        tempDir.mkdirs();
        return new DirHandle(tempDir);
    }

    @Override
    public FileHandle getFileHandle(Uri uri) {
        return new FileHandle(new File(uri.getPath()));
    }

    @Override
    public boolean exists(DirHandle dir) {
        return dir.getFile().exists();
    }

    @Override
    public boolean exists(FileHandle file) {
        return file.getFile().exists();
    }

    @Override
    public Iterable<FileHandle> listFiles(final DirHandle dir) {
        return new Iterable<FileHandle>() {
            public Iterator<FileHandle> iterator() {
                return new Iterator<FileHandle>() {
                    private int pos = 0;
                    private File[] files = dir.getFile().listFiles();

                    public boolean hasNext() {
                        return files == null ? false : pos < files.length;
                    }

                    public FileHandle next() {
                        return new FileHandle(files[pos++]);
                    }
                };
            }
        };
    }

    @Override
    public Uri getUri(DirHandle d) {
        return d.getUri();
    }

    @Override
    public Uri getUri(FileHandle f) {
        return f.getUri();
    }

    @Override
    public String getName(FileHandle f) {
        return f.getFile().getName();
    }

    @Override
    public long getLastModified(FileHandle file) {
        return file.getFile().lastModified();
    }

    @Override
    public FileHandle createFileHandle(
        DirHandle dir, String fileName, String mimeType
    ) {
        File file = new File(dir.getFile(), fileName);
        if (file.exists())
            return null;
        return new FileHandle(new File(dir.getFile(), fileName));
    }

    @Override
    public void delete(FileHandle fileHandle) {
        if (exists(fileHandle))
            fileHandle.getFile().delete();
    }

    @Override
    public void moveTo(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        File file = fileHandle.getFile();
        File directory = destDirHandle.getFile();
        file.renameTo(new File(directory, file.getName()));
    }

    @Override
    public OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }

    @Override
    public InputStream getInputStream(FileHandle fileHandle)
            throws IOException {
        return new FileInputStream(fileHandle.getFile());
    }

    private File getRootDirectory() {
        return rootDirectory;
    }
}
