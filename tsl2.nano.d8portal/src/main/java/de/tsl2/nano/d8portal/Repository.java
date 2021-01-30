package de.tsl2.nano.d8portal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.Util;

public class Repository implements IRespository {

    private static final String DIR_TARGET = "target/";
    private String name;
    private String remoteUrl;
    private StringBuilder publishComment = new StringBuilder();
    private String publishOptions = " ";

    public static final String DRY_RUN = "--dry-run";

    public Repository(String name) {
        this(name, null, null);
    }
    public Repository(String name, StringBuilder publishOptions) {
        this(name, null, publishOptions.toString());
    }
    public Repository(String name, String remoteUrl) {
        this(name, remoteUrl, null);
    }
    public Repository(String name, String remoteUrl, String publishOptions) {
        this.name = name;
        this.remoteUrl = remoteUrl;
        this.publishOptions = publishOptions;
    }

    public void setPublishParameter(String publishOptions) {
        this.publishOptions = publishOptions;
    }
    @Override
    public void create() {
        assert remoteUrl != null : "on create, remoteUrl must not be null!";
        publishComment.append("new repository" + name + " created\n");
        try {
            Files.createDirectories((getBaseDir().toPath()));
            call("init");
            call("remote", "add", "origin", remoteUrl); 
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }

    public File getBaseDir() {
        return FileUtil.userDirFile(name).getAbsoluteFile();
    }

    @Override
    public void addFile(String filename) {
        publishComment.append(filename + " added\n");
        call("add", filename);
    }

    @Override
    public void removeFile(String filename) {
        publishComment.append(filename + " removed\n");
        call("remove", filename);
    }
    
    private void call(String cmd, String... args) {
        args = CollectionUtil.concat(new String[]{"git", cmd}, args);
        SystemUtil.execute(getBaseDir(), args);
    }

    private void shell(String... args) {
        SystemUtil.executeShell(getBaseDir(), args);
    }

    public List<String> newFiles() {
        call("fetch");
        prepareTargetDir();
        String newfilesName = "newfiles.txt";
        call("diff", "--name-only", "..origin", "--output", DIR_TARGET + newfilesName);
        return readTargetFile(newfilesName);
    }

	public List<String> lsFiles() {
        prepareTargetDir();
        String lsFileName = "lsfiles.txt";
        shell("git ls-files > " + DIR_TARGET + lsFileName);
        return readTargetFile(lsFileName);
    }
    private void prepareTargetDir() {
        FileUtil.userDirFile(getBaseDir() + "/" + DIR_TARGET).mkdirs();
    }
    
    private LinkedList<String> readTargetFile(String lsFileName) {
        LinkedList<String> lines = new LinkedList<String>();
        Util.trY(() -> new Scanner(FileUtil.userDirFile(getBaseDir() + "/" +  DIR_TARGET + lsFileName))
            .forEachRemaining(l -> lines.add(l)));
        return lines;
    }

    @Override
    public void refresh() {
        call("pull");
    }

    @Override
    public void publish() {
        call("add", "**");
        call("commit", "-am", publishComment.toString());
        call("push", publishOptions, "--set-upstream", "origin", "master");
    }
}
