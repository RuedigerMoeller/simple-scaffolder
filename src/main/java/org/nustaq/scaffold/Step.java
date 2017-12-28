package org.nustaq.scaffold;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Step implements Serializable {

    String ignoredDirs[] = { "build", "target","node_modules" };
    String acceptedDirs[] = {};

    String ignoredFiles[] = { "*.MD" };
    String acceptedFiles[] = {};

    // map of base-relative dirs to transform
    // { "src/main/java/templatepackage" : "src/main/java/org/wombat/tada" }
    Map<String,String> dirTransforms = new HashMap<>();

    // map of filenametransforms
    Map<String,String> fileNameTransforms = new HashMap<>();

    // map of source transforms
    Map<String,String> srcTransforms = new HashMap<>();

    public String[] getIgnoredFiles() {
        return ignoredFiles;
    }

    public String[] getAcceptedFiles() {
        return acceptedFiles;
    }

    public String[] getIgnoredDirs() {
        return ignoredDirs;
    }

    public String[] getAcceptedDirs() {
        return acceptedDirs;
    }

    public Map<String, String> getDirTransforms() {
        return dirTransforms;
    }

    public Map<String, String> getFileNameTransforms() {
        return fileNameTransforms;
    }

    public Map<String, String> getSrcTransforms() {
        return srcTransforms;
    }
}
