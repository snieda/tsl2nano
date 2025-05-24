package de.tsl2.nano.h5;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.FilePath;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.specification.Pool;

@Deprecated // replaced by CMSBean - but action #downloadAndExtract0() is refeenced!
@ValueExpression("{name}")
@Attributes(names = { "name", "description", "imagePath", "applicationZipPath", "insideCurrentEnvironment" })
@Presentable()
public class SampleApplicationBean {
    static final String SF_BASE_URL_FILE = "https://sourceforge.net/projects/tsl2nano/files/sample-applications/";
    static final String SF_BASE_URL_WIKI = "https://sourceforge.net/p/tsl2nano/wiki/";

    String name;
    String description;
    boolean insideCurrentEnvironment;

    public SampleApplicationBean(String name) {
        this.name = name;
    }

    public static List<String> evalSampleApplications() {
        File readme = NetUtil.download(SF_BASE_URL_FILE, Pool.getSpecificationRootDir());
        String readmeText = FilePath.read(readme);
        String apps = StringUtil.extract(readmeText, "sample-application-names: .*");
        return Arrays.asList(apps.split(","));
    }


    @SuppressWarnings("rawtypes")
    public static BeanCollector provideSampleApplicationInstallation() {
        List<String> appNames = evalSampleApplications();
        List<SampleApplicationBean> samples = new ArrayList<>(appNames.size());
        appNames.forEach(n -> samples.add(new SampleApplicationBean(n)));
        return new BeanCollector<List<SampleApplicationBean>,SampleApplicationBean>(samples, 1);
    }

    @Action
    public String downloadAndInstall() {
        return downloadAndExtract(name, getApplicationZipPath(), isInsideCurrentEnvironment());
    }

    public static String downloadAndExtract0(Bean<?> selected) {
        return downloadAndExtract(selected.getName(), (String)selected.getValue("value"), false);
    }

    private static String downloadAndExtract(String name, String zipUrl, boolean insideCurrentEnvironment) {
        String path = ENV.getConfigPath();
        if (!insideCurrentEnvironment) {
            path = System.getProperty("user.dir") + "/." + name + ".install/";
            new File(path).mkdirs();
        }
        File zip = NetUtil.download(zipUrl, path);
        FileUtil.extract(zip.getPath(), path, null);
        String info = name + " successfully downloaded and installed on path: " + path;
        Message.info(info + "\n\n" + (insideCurrentEnvironment
                ? "to start it, re-login selecting " + name + ".jar file"
                : "to start it, you have to shutdown and start the application with: ./run.sh ." + name
                        + ".environment"));
        return info;
    }

    public String getName() {
        return name;
    }


    @Presentable(type = IPresentable.TYPE_ATTACHMENT, style = IPresentable.STYLE_DATA_IMG, enabled = false)
    public String getImagePath() {
        return SF_BASE_URL_WIKI + name + "/attachment/" + name + ".jpg";
    }

    public String getApplicationZipPath() {
        return SF_BASE_URL_FILE + name + ".zip";
    }

    @Presentable(type = IPresentable.TYPE_INPUT_MULTILINE, enabled = false)
    public String getDescription() {
        if (description == null) {
            description = NetUtil.get(SF_BASE_URL_FILE + name + ".txt");
        }
        return description;
    }

    public String getDetailsUrl() {
        return SF_BASE_URL_WIKI + name;
    }

    public boolean isInsideCurrentEnvironment() {
        return insideCurrentEnvironment;
    }

    public void setInsideCurrentEnvironment(boolean insideCurrentEnvironment) {
        this.insideCurrentEnvironment = insideCurrentEnvironment;
    }
}
