package de.tsl2.nano.replication.util;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.h2.tools.Server;

@SuppressWarnings("unchecked")
public class H2Util {
	static final int DEFAULT_H2_PORT = 9092;
	
	public static void startH2Datbase() throws Exception {
		startH2Datbase(DEFAULT_H2_PORT);
	}
	public static void startH2Datbase(int tcpPort) throws Exception {
		if (!isOpen(tcpPort)) {
			ULog.call("creating H2 database", 
					() -> Server.createTcpServer("-baseDir", new File("").getAbsolutePath(), "-tcpPort", String.valueOf(tcpPort), "-tcpAllowOthers", "-ifNotExists").start(),
					() -> Server.createWebServer("-webPort", "8082", "-webAllowOthers").start());
		} else {
			ULog.log("H2 database seems already to be open on port " + tcpPort);
		}
	}
	
	public static void stopH2Datbase() throws Exception {
		stopH2Datbase(DEFAULT_H2_PORT);
	}
	public static void stopH2Datbase(int tcpPort) throws Exception {
		ULog.call("stopping H2 database", 
				() -> {Server.createTcpServer("-tcpPort", String.valueOf(tcpPort), "-tcpAllowOthers").stop(); return Optional.empty();},
				() -> {Server.createWebServer("-webPort", "8082", "-webAllowOthers").stop(); return Optional.empty();});
	}
	
	static boolean isOpen(int tcpPort) {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(tcpPort);
			return false;
		} catch (IOException e) {
			//OK, already open, this is only a check...
			return true;
		} finally  {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void exportTableContent(EntityManager em, String... tables) throws Exception {
		ULog.call("exporting datbase tables...", (() -> {Arrays.stream(tables).forEach(t -> em.createNativeQuery("script table " + t)); return null;}));
	}
	public static void disableReferentialIntegrity(EntityManager em) {
		ULog.log("...disabling referential integrity on H2 database...", true);
		em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE");
	}
}

