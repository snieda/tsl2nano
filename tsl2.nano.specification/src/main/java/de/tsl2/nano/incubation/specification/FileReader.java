package de.tsl2.nano.incubation.specification;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tsl2.nano.bean.BeanFileUtil;
import de.tsl2.nano.bean.TransformableBeanReader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.scanner.FieldReader;

/**
 * combines three types of reading data: through simple delimited files (
 * {@link FieldReader} ), files filling beans of given type (
 * {@link BeanFileUtil} or through a markdown file with a full definition of
 * beantypes and human readable expressions.
 * 
 * @author ts
 */
public class FileReader {
	public static Collection<Object> getFileItems(String queryOrFileName) {
		String file = StringUtil.substring(queryOrFileName, null, "=");
		String type = StringUtil.substring(queryOrFileName, ":", null, true, true);
		if (type != null)
			file = StringUtil.substring(queryOrFileName, null, ":");
		else
			file = StringUtil.substring(queryOrFileName, "=", null);
		if (type == null) {
			if (file.endsWith("md")) {
				return new TransformableBeanReader().read(file);
			} else {
				Map<Object, List> table = FieldReader.readTable(file, null);
				List<Object> result = new LinkedList<>();
				table.values().forEach(l -> result.add(l.toArray()));
				return result;
			}
		} else {
			return BeanFileUtil.fromFlatFile(file, BeanClass.load(type));
		}
	}

}
