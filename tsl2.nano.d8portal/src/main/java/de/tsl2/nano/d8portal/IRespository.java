package de.tsl2.nano.d8portal;

import java.io.File;
import java.util.List;

/**
 * rudimental functions to manage a repository (like git)
 */
public interface IRespository {
    File getBaseDir();
    void create();
    List<String> lsFiles();
    List<String> newFiles();
    void addFile(String filename);
    void removeFile(String filename);
    void refresh();
    void publish();
}
