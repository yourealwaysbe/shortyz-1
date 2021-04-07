
package app.crossword.yourealwaysbe.util.files;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;

public class PuzMetaFile
        implements Comparable<PuzMetaFile> {
    public PuzHandle handle;
    public MetaCache.MetaRecord meta;

    PuzMetaFile(PuzHandle handle, MetaCache.MetaRecord meta) {
        this.handle = handle;
        this.meta = meta;
    }

    public PuzHandle getPuzHandle() { return handle; }

    public int compareTo(PuzMetaFile other) {
        try {
            // because LocalDate is day-month-year, fall back to name
            int dateCmp = other.getDate().compareTo(this.getDate());
            if (dateCmp != 0)
                return dateCmp;
            return getHandler().getName(
                this.handle.getPuzFileHandle()
            ).compareTo(
                getHandler().getName(
                    other.handle.getPuzFileHandle()
                )
            );
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isUpdatable() {
        return (meta == null) ? false : meta.isUpdatable();
    }

    public String getCaption() {
        return (meta == null) ? "" : meta.getTitle();
    }

    public LocalDate getDate() {
        if (meta == null) {
            return getHandler().getModifiedDate(handle.getPuzFileHandle());
        } else {
            return meta.getDate();
        }
    }

    public int getComplete() {
        return (meta == null)
            ? 0
            : (meta.isUpdatable() ? (-1) : meta.getPercentComplete());
    }

    public int getFilled() {
        return (meta == null)
            ? 0
            : (meta.isUpdatable() ? (-1) : meta.getPercentFilled());
    }

    public String getSource() {
        return ((meta == null) || (meta.getSource() == null))
            ? "Unknown"
            : meta.getSource();
    }

    public String getTitle() {
        if ((meta == null)
                || (meta.getSource() == null)
                || (meta.getSource().length() == 0)) {
            String fileName = getHandler().getName(handle.getPuzFileHandle());
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return meta.getSource();
        }
    }

    @Override
    public String toString(){
        return getHandler().getUri(handle.getPuzFileHandle()).toString();
    }

    private FileHandler getHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }
}
