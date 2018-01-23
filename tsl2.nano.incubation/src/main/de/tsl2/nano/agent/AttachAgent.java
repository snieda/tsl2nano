package de.tsl2.nano.agent;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

import com.sun.tools.attach.VirtualMachine;

import de.tsl2.nano.core.util.NumberUtil;

/**
 * is able to attach java agent on runtime (if not done on starting java with e.g. '-javaagent:lib/aspectjweaver.jar').
 * 
 * @author Thomas Schneider
 */
public class AttachAgent {

  /** AspectJ definitions */
  private static final String AGENT_JAR_ASPECTJ = "lib/aspectjweaver.jar";
  private static final String AGENT_CLS_ASPECTJ = "org.aspectj.weaver.loadtime.Agent";
  private static final String AGENT_MTD_ASPECTJ = "getInstrumentation";

  /**
   * @delegates to {@link #attachGivenAgentToThisVM(String, String, String)} without class and method to check.
   */
  public static boolean attachGivenAgentToThisVM(String pathToAgentJar) {
    return attachGivenAgentToThisVM(pathToAgentJar, null, null, null);
  }

  /**
   * attaches the given agent to the current java vm
   * 
   * @param pathToAgentJar agent jar
   * @param agentClass agent class name
   * @param agentMethod optional agent method to call/check
   * @param optional PID (OS process id of VM). if null, the current VM will be used
   * @return if agent was loaded and - if agentClass and/or method was given, if it could be invoked
   */
  public static boolean attachGivenAgentToThisVM(String pathToAgentJar, String agentClass, String agentMethod, String pid) {
    try {
      pid = pid == null ? getPIDofCurrentVM() : pid;
      VirtualMachine vm = VirtualMachine.attach(pid);
      log("attaching agent " + pathToAgentJar + " to VM " + vm + " : " + vm.toString());
      vm.loadAgent(pathToAgentJar, "");
      vm.detach();
      if (agentClass != null)
        return isAgentLoaded(agentClass, agentMethod);
      else
        return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static String getPIDofCurrentVM() {
      String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
      return nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
}

/**
   * check, if given agent is loaded
   * 
   * @param agentClass class name of agent
   * @param agentMethod optional method to be called on agent
   * @return true, if agent class is available and optional if method could be started (will log some agent infos)
   */
  public static boolean isAgentLoaded(String agentClass, String agentMethod) {
    try {
      Class<?> cls = Class.forName("org.aspectj.weaver.loadtime.Agent");
      if (agentMethod != null) {
        Method method = cls.getMethod("getInstrumentation");
        Object instrumentation = method.invoke(null);
        if (instrumentation instanceof Instrumentation)
          log(instrumentation + ", initiated classes: " + ((Instrumentation)instrumentation).getInitiatedClasses(null).length);
        else
          log(instrumentation);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  protected static final void log(Object obj) {
    System.out.println(obj);
  }
  
  /**
   * attaches aspectj without arguments
   * @param args
   */
  public static final void main(String[] args) {
    System.setProperty("LogMeAs.system.out", "true");
    if (args == null)
      args = new String[0];
    if (args.length <= 1 && (args[0].contains("?") || args[0].toLowerCase().contains("help"))) {
      System.out.println(AttachAgent.class.getName() + "{VM-PID or 0} [agent-lib-jar [agent-class [argent-method]]]");
      return;
    }
    int i = 0;
    String pid = args.length > i ? args[i++] : null;
    String jar = args.length > i ? args[i++] : AGENT_JAR_ASPECTJ;
    String cls = args.length > i ? args[i++] : AGENT_CLS_ASPECTJ;
    String mtd = args.length > i ? args[i++] : AGENT_MTD_ASPECTJ;
    
    if (NumberUtil.toNumber(pid) <= 0)
        pid = null;
    if (!attachGivenAgentToThisVM(jar, cls, mtd, pid))
      throw new IllegalStateException("agent couldn't be loaded!");
  }
}
