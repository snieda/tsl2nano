package de.tsl2.nano.h5;

import java.io.IOException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;

public class NanoH5App extends NanoH5 {
    protected static final Log LOG = LogFactory.getLog(NanoH5App.class);

    public NanoH5App() throws IOException {
        super();
        init();
    }

    public NanoH5App(String serviceURL, IPageBuilder<?, String> builder) throws IOException {
        super(serviceURL, builder);
        init();
    }

    protected void init() {
    }
   @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        startApplication(NanoH5App.class, MapUtil.asMap(0, "service.url"), args);
    }
}