/**
 * 
 */
package tsl2.nano.restaccess;

import java.io.IOException;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * This JOMapper implementation uses simple class names as type and json as content, using jackson.
 * @author Thomas Schneider
 *
 */
public class JSONMapper<T> extends JOMapper<T> {
  private static final long serialVersionUID = -7056788058308961179L;
  private static final Logger logger = Logger.getLogger(JOMapper.class.getSimpleName());
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

  /**
   * constructor
   * @param content
   */
  public JSONMapper(String content) {
      super(content);
      // TODO Auto-generated constructor stub
  }

  
  public JSONMapper(String type, String content) {
    super(type, content);
  }


  /**
   * creates this mapper through a real object. this constructor is inverse to method
   * {@link #toObject()}.
   * @param object real java object
   */
  public JSONMapper(T object) {
    this.cls = (Class<T>) object.getClass();
    this.type = cls.getSimpleName();
    try {
      this.content = new ObjectMapper().writeValueAsString(object);
      logger.info("new JSON object createed: " + content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

}
