package de.tsl2.nano.aspect;

/**
 * add
 * "-javaagent:/$USER_HOME/.m2/repository/org/aspectj/aspectjweaver/1.9.5/aspectjweaver-1.9.5.jar"
 * to your JVM ARGS
 */
public class App {
    @Cover(up = true) Account account = new Account();
    public static void main(String[] args) {
        App app = new App();
        Account account = app.account;
        System.out.println("==> App: Account is allowed? " + account.allowed());
    }
}
