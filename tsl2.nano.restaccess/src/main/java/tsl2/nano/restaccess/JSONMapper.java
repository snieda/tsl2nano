/**
 * 
 */
package tsl2.nano.restaccess;

import java.io.IOException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * This JOMapper implementation uses simple class names as type and json as content, using jackson.
 * @author schneith
 *
 */
public class JSONMapper<T> extends JOMapper<T> {
  private static final long serialVersionUID = -7056788058308961179L;

  /** package holding type */
  private String basePath;
  
  /**
   * @param basePath see {@link #basePath}
   */
  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }
  
  public JSONMapper() {
    super();
  }

  
  public JSONMapper(String type, String content) {
    super(type, content);
  }


  public JSONMapper(T object) {
    super(object);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<T> loadClass(String type) {
    if (basePath == null)
      basePath = System.getProperty("jsonmapper.basepath");
    if (basePath == null)
      throw new IllegalStateException("jsonmapper.basepath must not be null! Please define a system property 'jsonmapper.basepath' or call setBasePath() pointing to the package path of the desired entities!");
    if (!basePath.endsWith("."))
      basePath += ".";
    try {
      return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(basePath + type);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T toObject() {
    ObjectMapper om = new ObjectMapper();
    try {
      return om.readValue(content.getBytes(), getObjectType());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

/**
 * constructor
 * @param content
 */
public JSONMapper(String content) {
    super(content);
    // TODO Auto-generated constructor stub
}

}