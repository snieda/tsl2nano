package de.tsl2.nano.incubation.terminal;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.print.attribute.standard.MediaSizeName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.codegen.ACodeGenerator;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.secure.Crypt;
import de.tsl2.nano.core.secure.Permutator;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.gp.EvolutionalAlgorithm;
import de.tsl2.nano.incubation.terminal.TextTerminal.Frame;
import de.tsl2.nano.incubation.terminal.item.Action;
import de.tsl2.nano.incubation.terminal.item.Container;
import de.tsl2.nano.incubation.terminal.item.Input;
import de.tsl2.nano.incubation.terminal.item.MainAction;
import de.tsl2.nano.incubation.terminal.item.Option;
import de.tsl2.nano.incubation.terminal.item.selector.AntTaskSelector;
import de.tsl2.nano.incubation.terminal.item.selector.DirSelector;
import de.tsl2.nano.incubation.terminal.item.selector.FieldSelector;
import de.tsl2.nano.incubation.terminal.item.selector.FileSelector;
import de.tsl2.nano.incubation.terminal.item.selector.PropertySelector;
import de.tsl2.nano.incubation.terminal.item.selector.Sequence;
import de.tsl2.nano.incubation.terminal.item.selector.XPathSelector;
import de.tsl2.nano.incubation.vnet.NetCommunicator;
import de.tsl2.nano.incubation.vnet.neuron.VNeuron;
import de.tsl2.nano.incubation.vnet.routing.RoutingAStar;
import de.tsl2.nano.incubation.vnet.workflow.Condition;
import de.tsl2.nano.incubation.vnet.workflow.VActivity;
import de.tsl2.nano.util.PrintUtil;
import de.tsl2.nano.util.XmlGenUtil;

public class TerminalTest implements ENVTestPreparation {

    private static String TEST_DIR;

	@BeforeClass
    public static void setUp() {
    	TEST_DIR = ENVTestPreparation.setUp() + TARGET_TEST;
    }

    @AfterClass
    public static void tearDown() {
//    	ENVTestPreparation.tearDown();
    }
    
    
  @Ignore
  @Test
  public void testTerminal() throws Exception {
      Container root = new Container("selection1", null, new ArrayList<IItem>(), null);
      root.add(new Option(root, "option1", null, false, "Option 1"));
      root.add(new Option(root, "option2", null, false, "Option 2"));
      root.add(new Option(root, "option3", null, false, "Option 3"));
      root.add(new Input<Object>("input1", null, "Input 1", null));
      root.add(new Action(this.getClass(), "getName") {
    	  @Override
    	public Object run(Properties context) {
    		  name = "test";
    		return context.toString();
    	}
      });

      InputStream in = ByteUtil.getInputStream("1\n2\n3\n4\n5\n\n".getBytes());
      new SIShell(root, in, System.out, 79, 10, Frame.TEXT_LINE).run();

      SIShell.main(new String[] { SIShell.DEFAULT_NAME });

      //admin console
      SIShell.main(new String[] { SIShell.DEFAULT_NAME, TerminalAdmin.ADMIN });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testSIShellTools() throws Exception {

	  System.setProperty("user.dir", new File(TEST_DIR).getAbsolutePath());
	  
      FileUtil.removeToBackup(TEST_DIR + SIShell.DEFAULT_NAME);

      Container root = new Container("Toolbox", "Helpful Utilities");

      Container printing = new Container("Printing", null);
      printing.add(new FileSelector("source", "printer-info", ".*"));
      printing.add(new Input("printer", "PDFCreator", "printer to use"));
      printing.add(new Input("jobname", "tsl2nano", "print job name"));
      printing.add(new Input("mimetype", "MIME_PCL", "mime type"));
      printing.add(new FieldSelector("papersize", "ISO_A4", "paper size", MediaSizeName.class, MediaSizeName.class));
      printing.add(new Input("quality", new Constraint<String>(String.class, Arrays.asList("NORMAL", "HIGH")),
          "NORMAL", "print quality"));
      printing.add(new Input("priority", "1", "print priority (1-100)"));
      printing.add(new Input("xsltfile", null, "xsl-fo transformation file to do a apache fop"));
      printing.add(new Input("username", null, "user name to be used by the printer"));
      Action<?> mainAction;
      printing
          .add(mainAction =
              new MainAction("print", PrintUtil.class, "source", "printer", "jobname", "papersize", "quality",
                  "priority",
                  "xsltfile", "mimetype", "jobname", "username"));
      mainAction.setCondition(new Condition("quality=NORMAL"));
      root.add(printing);

      Container crypt = new Container("Crypt", null);
      crypt.add(new Input("password", null, "password for encryption - if needed by algorithm"));
      crypt.add(new Input("algorithm", "PBEWithMD5AndDES", "encryption algorithm"));
      crypt.add(new Input("text", null, "text to be encrypted. if it starts with 'file:' the file will be read",
          false));
      crypt.add(new Input("base64", true, "whether base64 encoding should be used"));
      crypt.add(new Input("include", ".*", "regular expression to constrain text parts to be encrypted"));
      crypt
          .add(new MainAction(Crypt.class, "password", "algorithm", "text", "base64", "include"));
      root.add(crypt);

      Container perm = new Container("Permutator", null);
      perm.add(new Input("source", null, "source collection", false));
      perm.add(new Input("transformer", null, "transforming action", false));
      perm.add(new Input("swap", null, "whether to swap key and values in destination-map"));
      perm.add(new Input("backward", null, "action to do a back-transformation for each keys value"));
      perm
          .add(new MainAction(Permutator.class, "source", "transformer", "swap", "backward"));
      root.add(perm);

      Container xml = new Container("Xml", null);
      xml.add(new FileSelector("source", null, ".*xml", "${user.dir}"));
      xml.add(new FileSelector("xsl-transformation", null, ".*xsl.*", "${user.dir}"));
      xml.add(new Input("xsl-destination", "${user.dir}/${source}.html", "xsl destination file", false));
      xml.add(new Input("xpath-expression", null, "xpath expression", false));
      xml.add(new Action(XmlGenUtil.class, "transformVel", "source", Action.KEY_ENV));
      xml.add(new Action(XmlGenUtil.class, "transformXsl", "source", "xsl-transformation", "xsl-destination"));
      xml.add(new Action(XmlGenUtil.class, "xpath", "xpath-expression", "source"));
      xml.add(new Sequence(new de.tsl2.nano.incubation.terminal.item.Command("sequential echo command", "echo"),
          new XPathSelector(
              "xpathselector", null, "bin/" + SIShell.DEFAULT_NAME, "//@name"),
          null));
      root.add(xml);

      Container html = new Container("Html", null);
      html.add(new FileSelector("source", null, ".*.markdown", "${user.dir}"));
      html.add(new MainAction("Markdown (TxtMark)", "com.github.rjeschke.txtmark.cmd.Run", "source"));
      root.add(html);

      Container ant = new Container("Ant", null);
      ant.add(new AntTaskSelector("task", "Jar", "pack given filesets to zip"));
      ant.add(new PropertySelector<String>("properties", "ant task properties",
          null/*MapUtil.asMap(new TreeMap(), "destFile", "mynew.jar")*/));
      ant.add(new Input("filesets", "./:{**/*.*ml}**/*.xml;${user.dir}:{*.txt}", "filesets expression", false));
      ant.add(new Action(AntRunner.class, "runTask", "task", "properties", "filesets"));
      root.add(ant);

      Container vnet = new Container("vNet", null);
      Container impl = new Container("implementation", "vNet implementation");
      impl.add(new Option(vnet, "NeuralNet", null, VNeuron.class.getName(), "NeuralNet implementation class"));
      impl.add(new Option(vnet, "RoutingStar", null, RoutingAStar.class.getName(), "Routing implementation class"));
      impl.add(new Option(vnet, "Workflow", null, VActivity.class.getName(), "Workflow implementation class"));
      vnet.add(impl);
      vnet.add(new Action(NetCommunicator.class, "main", "implementation"));
      root.add(vnet);

//      Tree getjar = new Tree("getJar", null);
//      getjar.add(new Input("name", null, "name, jar-file or class package to load with dependencies from web"));
//      getjar.add(new MainAction(BeanClass.createBeanClass("de.tsl2.nano.jarresolver.JarResolver", null).getClazz(), "name"));
//      root.add(getjar);
//
      Container net = new Container("Net", "wrench.png");
      Container scan = new Container("Scan", null);
      net.add(scan);
      scan.add(new Input("ip", NetUtil.getMyIP(), "internet address to be scanned"));
      scan.add(new Input("lowest-port", 0, "lowest port to be scanned"));
      scan.add(new Input("highest-port", 100, "highest port to be scanned"));
      scan.add(new Action(NetUtil.class, "scans", "(int)lowest-port", "(int)highest-port", "(java.lang.String[])ip"));
      Container wcopy = new Container("WCopy", "Downloads a site");
      net.add(wcopy);
      wcopy.add(new Input("url", null, "url to get files from", false));
      wcopy.add(new Input("dir", null, "local directory to save the downloaded files"));
      wcopy.add(new Input("include", null, "regular expression for files to download"));
      wcopy.add(new Input("exclude", null, "regular exression for files to be filtered"));
      wcopy.add(new Action(NetUtil.class, "wcopy", "url", "dir", "include", "exclude"));
      Container proxy = new Container("Proxy", null);
      net.add(proxy);
      proxy.add(new Input("uri", null, "uri to evaluate proxy for (http, https, ftp or socket)", false));
      proxy.add(new Input("proxy", null, "new proxy (e.g.: myproxy.myorg.org)"));
      proxy.add(new Input("user", null, "new proxies user"));
      proxy.add(new Input("password", null, "new proxies password"));
      proxy.add(new Action(NetUtil.class, "proxy", "uri", "proxy", "user", "password"));

      Container download = new Container("Download", "Downloads a single file");
      net.add(download);
      download.add(new Input("url", null, "url to be loaded", false));
      download.add(new Input("dir", null, "local directory to save the downloaded file"));
      download.add(new Action(NetUtil.class, "download", "url", "dir"));

      Container browse = new Container("Browse", "Shows the given URL");
      net.add(browse);
      browse.add(new Input("url", null, "url to be loaded", false));
      browse.add(new Action(NetUtil.class, "browse", "url", "out"));

      Container restful = new Container("Restful", "Calls a RESTful service");
      net.add(restful);
      restful.add(new Input("url", "http://echo.jsontest.com/title/ipsum", "URL of a RESTful service", false));
      restful
          .add(new PropertySelector<String>("arguments", "RESTful arguments",
              null/*MapUtil.asMap(new TreeMap(), "destFile", "mynew.jar")*/));
      restful.add(new Action(NetUtil.class, "getRest", "url", "arguments"));

      net.add(new Action(NetUtil.class, "getNetInfo"));
      net.add(new Action(NetUtil.class, "getFreePort"));
      root.add(net);

      Container file = new Container("File-Operation", null);
      file.add(new DirSelector("directory", "${user.dir}", ".*"));
      file.add(new Input("file", "**/[\\w]+\\.txt", "regular expression (with ant-like path **) as file filter"));
      file.add(new Input("destination", "${user.dir}", "destination directory for file operations"));
      file.add(new Action("Details", FileUtil.class, "getDetails", "(java.io.File)file"));
      file.add(new FileSelector("List", "file", "directory"));
      file.add(new Action("Delete", FileUtil.class, "forEach", "directory", "file", "(" + IRunnable.class.getName()
          + ")" + Action.createReferenceName(FileUtil.class, "DO_DELETE")));
      file.add(new Action("Copy", FileUtil.class, "forEach", "directory", "file", "(" + IRunnable.class.getName()
          + ")" + Action.createReferenceName(FileUtil.class, "DO_COPY")));
      file.add(new MainAction("Imageviewer", AsciiImage.class, "file", "image.out", "sishell.width",
          "sishell.height"));
      root.add(file);

      Container shell = new Container("Shell", "Starts OS Shell commands");
      shell.add(new de.tsl2.nano.incubation.terminal.item.Command("command", "cmd /C"));
      root.add(shell);

      Container generator = new Container("Generator", "Generate Code with Velocity");
      generator.add(new Input("algorithm", "de.tsl2.nano.codegen.PackageGenerator", "generator implementation"));
      generator.add(new Input("model", "${user.dir}", "package file path of source code used as model"));
      generator.add(new Input("template", "codegen/bean-const.vm", "velocity template file path"));
      generator.add(new Input("filter", "", "optional package  of source code files"));
      generator.add(new Input("propertyFile", "", "optional property file"));
      generator.add(new Input("outputPath", "${user.dir}", "optional output path"));
      generator.add(new Input("destinationPrefix", "", "optional destination file prefix"));
      generator.add(new Input("destinationPostfix", "${user.dir}", "optional destination file postfix"));
      generator.add(new Input("unpackaged", "false", "whether to store flat without package structure"));
      generator.add(new Input("singleFile", "false", "whether to generate only a single file"));
      generator.add(new Action(ACodeGenerator.class, "start", "algorithm", "model", "template", "filter", "propertyFile"));
      root.add(generator);

      Container evolutionalAlg = new Container("EvolutionalAlgorithm", "Starts an Evolutional Algorithm");
      evolutionalAlg.add(new Input("fitnessFunction", "de.tsl2.nano.gp.PolyglottFitnessFunction", "Fitness function implementation"));
      evolutionalAlg.add(new Input("evolutionalalgorithm.fitnessfunction.script", "fit.ts", "Script implementation if fitnessFunction is PolyglottFitnessFunction"));
      evolutionalAlg.add(new Input("geneticRangeLow", "10", ""));
      evolutionalAlg.add(new Input("geneticRangeHigh", "10", ""));
      evolutionalAlg.add(new MainAction("start", EvolutionalAlgorithm.class, "fitnessFunction=${fitnessFunction}", "geneticRangeLow=${geneticRangeLow}", "geneticRangeHigh=${geneticRangeHigh}"));
      root.add(evolutionalAlg);

      Map<String, Object> defs = new HashMap<String, Object>();
      defs.put("image.out", "-out");
      System.getProperties().put(SIShell.KEY_SEQUENTIAL, true);

      InputStream in = SystemUtil.createBatchStream("Printing", "jobname", "test", "10", "", ":quit");
      new SIShell(root, in, System.out, 79, 22, Frame.TEXT_LINE, defs).run();

      //check, if serialization was ok
      System.setIn(SystemUtil.createBatchStream("", "Printing", "jobname", "test", "10", "", ":quit"));
      SIShell.main(new String[] { TEST_DIR + SIShell.DEFAULT_NAME });

      //ok? --> use it in our project!
      FileUtil.copy(TEST_DIR + SIShell.DEFAULT_NAME, "src/resources/" + SIShell.DEFAULT_NAME);

      //admin console
//      SIShell.main(new String[] { SIShell.DEFAULT_NAME, TerminalAdmin.ADMIN });
  }

  @Test
  public void testTerminalAdmin() throws Exception {
      //admin console
      //TODO: test
//      SIShell.main(new String[] { SIShell.DEFAULT_NAME, TerminalAdmin.ADMIN });
  }
}
