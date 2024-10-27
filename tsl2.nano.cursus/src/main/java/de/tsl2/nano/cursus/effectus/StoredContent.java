package de.tsl2.nano.cursus.effectus;

import de.tsl2.nano.core.util.ByteUtil;

/**
 * NOT USED YET!<p/>
 * Holds content, calculated once and reused on re-doing the same command again - without re-calcuation
 * @author Tom
 */
public class StoredContent<T> implements IStoredContent<T> {

	private String identifier;
	private String path;
	private String typeName;
	private byte[] content;

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public byte[] getContent() {
		return content;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T toObject() {
		return (T) ByteUtil.convertToObject(content);
	}

	@Override
	public void fromObject(T instance) {
		typeName = instance.getClass().getName();
		//TODO
	}
}
