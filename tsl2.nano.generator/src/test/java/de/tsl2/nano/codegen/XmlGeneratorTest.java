package de.tsl2.nano.codegen;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.json.XML;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.codegen.XmlGenerator;

public class XmlGeneratorTest implements ENVTestPreparation {
    private static final Log LOG = LogFactory.getLog(XmlGeneratorTest.class);
    private static String BASE_DIR_GENERATOR;

    @BeforeClass
    public static void setUp() {
        BASE_DIR_GENERATOR = ENVTestPreparation.setUp() /*+ "tsl2.nano.generator/"*/;
    }

    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }
    

    private static JSONObject convertToJson(String yamlFile) throws ParseException {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(FileUtil.getFile(yamlFile));
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(JSONObject.toJSONString(map));
        return jsonObject;
    }

    @Ignore("howto convert json to xml....")
    @Test
    public void testXmlGenerationFromYAML() throws Exception {
        JSONObject json = convertToJson(BASE_DIR_GENERATOR + "src/test/de/tsl2/nano/codegen/petstore-v3.0.yaml");
        String xml = XML.toString(json);

        String file = "tsl2.nano.common/target/petstore-v3.0.xml";
        FileUtil.writeBytes(xml.getBytes(), file, false);
        XmlGenerator.main(new String[] { file, "codegen/schema.vm", "//schema" });
        //TODO: check file creation!
    }
    @Test
    public void testXmlGenerationManPage() throws Exception {
        ACodeGenerator.start(new String[0], null, 0);
        //TODO: check output!
    }

    @Test
    public void testXmlGeneration() throws Exception {
        String file = BASE_DIR_GENERATOR + "pom.xml";
        ACodeGenerator.main(new String[] { XmlGenerator.class.getName(), file, "codegen/schema.vm", "//dependency" });
        //TODO: check file creation!
    }
}
