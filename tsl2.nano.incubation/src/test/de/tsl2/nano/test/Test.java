package de.tsl2.nano.test;

import de.tsl2.nano.agent.AttachAgent;

public class Test {
  public int methodOne(int c) {
    System.out.println("methodOne" + "(" + c + ")");
    return c; 
  }
  public static void main(String[] args) {
    AttachAgent.main(new String[0]);
    Test t = new Test();
    for (int i = 0; i < 10; i++) {
      t.methodOne(i);
    }
  }
}
