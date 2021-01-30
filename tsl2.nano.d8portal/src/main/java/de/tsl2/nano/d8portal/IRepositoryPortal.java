package de.tsl2.nano.d8portal;

import java.util.List;

import de.tsl2.nano.core.util.Period;

/**
 * simple interface for a portal providing repositories managed by an organsation and provided for its clients
 */
public interface IRepositoryPortal {
    void createOrganisation(String name, String email, String remoteUrl, String smtpServer);
    void createRepository(String id, String name, String email, String password);
    String upload(String filename, String id, String name, String type, Period period, String description);
    byte[] download(String filename, String id, String privateKey);
    List<String> synchronize(String id);
    List<String>  find(String id, String search);
    Long createQRCode(String url);
}
