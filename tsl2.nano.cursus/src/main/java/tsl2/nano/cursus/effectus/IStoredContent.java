package tsl2.nano.cursus.effectus;

/**
 * provides all informations to store/restore any object to/from byte[] stream. may be used for database blobs on instances.
 * @param <T> content type 
 * @author Tom
 */
public interface IStoredContent<T> {
	/** identifer of the root object where the content belongs to */
	String getIdentifier();
	/** path that guides from the root object (given by identifier) to the content object */
	String getPath();
	/** content type description */
	String getTypeName();
	/** the serialized content */
	byte[] getContent();
	/** deserializes the content to the java object */
	T toObject();
	/** evaluates all informations of the given object and serializes it */
	void fromObject(T instance);
}
