
package app.crossword.yourealwaysbe.util.files;

import java.io.IOException;

public abstract class PuzHandle {
    public DirHandle dirHandle;
    public FileHandle mainHandle;

    private PuzHandle(
        DirHandle dirHandle, FileHandle mainHandle
    ) {
        this.dirHandle = dirHandle;
        this.mainHandle = mainHandle;
    }

    public DirHandle getDirHandle() { return dirHandle; }
    public FileHandle getMainFileHandle() { return mainHandle; }

    public abstract <Ret> Ret accept(Visitor<Ret> v);
    public abstract <Ret> Ret accept(VisitorIO<Ret> v) throws IOException;

    static class Puz extends PuzHandle {
        private FileHandle metaHandle;

        public Puz(
            DirHandle dirHandle, FileHandle mainHandle, FileHandle metaHandle
        ) {
            super(dirHandle, mainHandle);
            this.metaHandle = metaHandle;
        }

        public FileHandle getMetaFileHandle() { return metaHandle; }

        // deliberately package-level
        void setMetaFileHandle(FileHandle metaHandle) {
            this.metaHandle = metaHandle;
        }

        @Override
        public <Ret> Ret accept(Visitor<Ret> v) { return v.visit(this); }

        @Override
        public <Ret> Ret accept(VisitorIO<Ret> v) throws IOException {
            return v.visit(this);
        }
    }

    static class IPuz extends PuzHandle {
        public IPuz(DirHandle dirHandle, FileHandle mainHandle) {
            super(dirHandle, mainHandle);
        }

        @Override
        public <Ret> Ret accept(Visitor<Ret> v) { return v.visit(this); }

        @Override
        public <Ret> Ret accept(VisitorIO<Ret> v) throws IOException {
            return v.visit(this);
        }
    }

    static interface Visitor<Ret> {
        Ret visit(Puz puzHandle);
        Ret visit(IPuz ipuzHandle);
    }

    static interface VisitorIO<Ret> {
        Ret visit(Puz puzHandle) throws IOException;
        Ret visit(IPuz ipuzHandle) throws IOException;
    }
}
