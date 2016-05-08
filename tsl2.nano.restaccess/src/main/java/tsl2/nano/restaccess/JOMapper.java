/**
 * 
 */
package tsl2.nano.restaccess;

import java.io.IOException;
import java.io.Serializable;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Java Object Mapper. maps a simple object through string descriptors to a java object<p/>
 * Used for rest services working on any java object - not knowing the object types at compile time.<p/>
 * 
 * @author schneith
 *
 */
public abstract class JOMapper<T> implements Serializable {
  private static final long serialVersionUID = -5300995754972414131L;

  /** object type short descriptor (normally the pure class name) */
  String type;
  /** attribute values descriptor */
  String content;
  
  /** can be evaluated through the type descriptor */
  transient Class<T> cls;
  
  public JOMapper() {
  }
  
  /**
   * @param type type descriptor to evaluate a java class from 
   * @param content java object content
   */
  public JOMapper(String type, String content) {
    super();
    this.type = type;
    this.content = content;
  }

  /**
   * creates this mapper through a real object. this constructor is inverse to method
   * {@link #toObject()}.
   * @param object real java object
   */
  public JOMapper(T object) {
    this.cls = (Class<T>) object.getClass();
    this.type = cls.getSimpleName();
    try {
      this.content = new ObjectMapper().writeValueAsString(object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  Class<T> getObjectType() {
    if (cls == null) {
      cls = loadClass(type);
    }
    return cls;
  }
  
  /**
   * @param type
   * @return evaluates and loads the java type through the given type descriptor
   */
  abstract protected Class<T> loadClass(String type);

  
  /**
   * @return see {@link #content}
   */
  public String getContent() {
    return content;
  }

  
  /**
   * @return see {@link #type}
   */
  public String getType() {
    return type;
  }

  /**
   * creates a java object through the descriptors 'type' and 'content'. this method is inverse to the constructor {@link #PObject(Object)}
   * @return java object new instance of type 'type' with attribute values defined by 'content'.
   */
  public abstract T toObject();

/**
 * constructor
 * @param content
 */
public JOMapper(String content) {
    this(Serializable.class.getName(), content);
}
  
}
