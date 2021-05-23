
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

/**
 * Base implementation of java.io.File-based file access from a given
 * root directory.
 */
public abstract class FileHandlerJavaFile extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerJavaFile.class.getCanonicalName());

    private File rootDirectory;

    public FileHandlerJavaFile(
        Context applicationContext, File rootDirectory
    ) {
        super(applicationContext);
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
    protected FileHandle getFileHandle(Uri uri) {
        return new FileHandle(new File(uri.getPath()));
    }

    @Override
    protected boolean exists(DirHandle dir) {
        return dir.getFile().exists();
    }

    @Override
    protected boolean exists(FileHandle file) {
        return file.getFile().exists();
    }

    @Override
    protected Iterable<FileHandle> listFiles(final DirHandle dir) {
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
    protected Uri getUri(DirHandle d) {
        return d.getUri();
    }

    @Override
    protected Uri getUri(FileHandle f) {
        return f.getUri();
    }

    @Override
    protected String getName(FileHandle f) {
        return f.getFile().getName();
    }

    @Override
    protected long getLastModified(FileHandle file) {
        return file.getFile().lastModified();
    }

    @Override
    protected FileHandle createFileHandle(
        DirHandle dir, String fileName, String mimeType
    ) {
        File file = new File(dir.getFile(), fileName);
        if (file.exists())
            return null;
        return new FileHandle(new File(dir.getFile(), fileName));
    }

    @Override
    protected void deleteUnsync(FileHandle fileHandle) {
        if (exists(fileHandle))
            fileHandle.getFile().delete();
    }

    @Override
    protected void moveToUnsync(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        File file = fileHandle.getFile();
        File directory = destDirHandle.getFile();
        file.renameTo(new File(directory, file.getName()));
    }

    @Override
    protected OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }

    @Override
    protected InputStream getInputStream(FileHandle fileHandle)
            throws IOException {
        return new FileInputStream(fileHandle.getFile());
    }

    private File getRootDirectory() {
        return rootDirectory;
    }
}
