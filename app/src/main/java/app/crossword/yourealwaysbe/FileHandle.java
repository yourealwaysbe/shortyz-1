package app.crossword.yourealwaysbe;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import app.crossword.yourealwaysbe.puz.PuzzleMeta;


public class FileHandle implements Comparable<FileHandle> {
    File file;
    PuzzleMeta meta;

    FileHandle(File f, PuzzleMeta meta) {
        this.file = f;
        this.meta = meta;
    }

    public int compareTo(FileHandle another) {
        FileHandle h = (FileHandle) another;

        try {
            // because LocalDate is day-month-year, fall back to file
            // modification time
            int dateCmp = h.getDate().compareTo(this.getDate());
            if (dateCmp != 0)
                return dateCmp;
            return Long.compare(
                this.file.lastModified(), another.file.lastModified()
            );
        } catch (Exception e) {
            return 0;
        }
    }

    String getCaption() {
        return (meta == null) ? "" : meta.title;
    }

    LocalDate getDate() {
        if (meta == null) {
            return Instant.ofEpochMilli(file.lastModified())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
        } else {
            return meta.date;
        }
    }

    int getComplete() {
        return (meta == null) ? 0 : (meta.updatable ? (-1) : meta.percentComplete);
    }

    int getFilled() {
        return (meta == null) ? 0 : (meta.updatable ? (-1) : meta.percentFilled);
    }

    String getSource() {
        return ((meta == null) || (meta.source == null)) ? "Unknown" : meta.source;
    }

    String getTitle() {
        return ((meta == null) || (meta.source == null) || (meta.source.length() == 0))
        ? file.getName()
              .substring(0, file.getName().lastIndexOf(".")) : meta.source;
    }

    @Override
    public String toString(){
        return file.getAbsolutePath();
    }
}
