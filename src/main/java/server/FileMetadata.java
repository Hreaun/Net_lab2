package server;

import java.io.Serializable;

public class FileMetadata implements Serializable {
    long fileSize;
    String filename;

    public FileMetadata(long fileSize, String filename) {
        this.fileSize = fileSize;
        this.filename = filename;
    }
}
