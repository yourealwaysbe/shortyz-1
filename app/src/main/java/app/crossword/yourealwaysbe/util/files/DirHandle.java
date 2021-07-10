package app.crossword.yourealwaysbe.util.files;

import java.io.File;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class DirHandle implements Parcelable {
    // common to all implementations
    private Uri uri;

    Uri getUri() { return uri; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DirHandle))
            return false;

        DirHandle otherDir = (DirHandle) other;
        return getUri().equals(otherDir.getUri());
    }

    @Override
    public int hashCode() {
        return getUri().hashCode();
    }

    @Override
    public String toString() { return uri.toString(); }

    //////////////////////////////////////////////////////////////
    // for FileHandlerJavaFile

    private File file;

    DirHandle(File file) {
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

    DirHandle(Uri uri) {
        this.uri = uri;
    }

    ///////////////////////////////////////////////////////////////
    // For both

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(uri, flags);
    }

    public DirHandle(Parcel in) {
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        // getFile will create the file object if needed
    }
}

