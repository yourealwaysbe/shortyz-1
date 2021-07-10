package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.util.Objects;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class FileHandle implements Parcelable {
    // common to all implementations
    private Uri uri;

    Uri getUri() { return uri; }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileHandle) {
            FileHandle other = (FileHandle) o;
            return Objects.equals(this.uri, other.uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }

    @Override
    public String toString() { return uri.toString(); }

    //////////////////////////////////////////////////////////////
    // for FileHandlerJavaFile

    private File file;

    FileHandle(File file) {
        this.uri = Uri.parse(file.toURI().toString());
        this.file = file;
    }

    File getFile() {
        if (file == null)
            file = new File(uri.getPath());
        return file;
    }

    //////////////////////////////////////////////////////////////
    // for FileHandlerSAF

    private FileHandlerSAF.Meta safMeta;

    FileHandle(Uri uri, FileHandlerSAF.Meta safMeta) {
        this.uri = uri;
        this.safMeta = safMeta;
    }

    FileHandlerSAF.Meta getSAFMeta() { return safMeta; }

    ///////////////////////////////////////////////////////////////
    // For both

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(uri, flags);
        out.writeParcelable(safMeta, flags);
    }

    public FileHandle(Parcel in) {
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.safMeta = in.readParcelable(
            FileHandlerSAF.Meta.class.getClassLoader()
        );
        // getFile will create the file object if needed
    }
}
