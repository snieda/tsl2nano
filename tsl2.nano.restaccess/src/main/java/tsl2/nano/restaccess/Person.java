package tsl2.nano.restaccess;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "##default")
//@XmlAccessorType(XmlAccessType.FIELD)
public class Person implements Serializable {

  private static final long serialVersionUID = -5295809408014144886L;

  private Long id = 1l;
  private String name = "test";
  private String description = "test...";

  public Person() {
    super();
  }

  public Person(
                String id) {
    this(Long.valueOf(id));
  }

  public Person(
                Long id) {
    super();
    this.id = id;
  }

/**
 * @return Returns the {@link #id}.
 */
public Long getId() {
    return id;
}

/**
 * @param id The {@link #id} to set.
 */
public void setId(Long id) {
    this.id = id;
}

/**
 * @return Returns the {@link #name}.
 */
public String getName() {
    return name;
}

/**
 * @param name The {@link #name} to set.
 */
public void setName(String name) {
    this.name = name;
}

/**
 * @return Returns the {@link #description}.
 */
public String getDescription() {
    return description;
}

/**
 * @param description The {@link #description} to set.
 */
public void setDescription(String description) {
    this.description = description;
}

}