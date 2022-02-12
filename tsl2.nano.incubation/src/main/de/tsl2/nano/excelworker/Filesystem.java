package de.tsl2.nano.excelworker;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * ON CONSTRUCTION
 * <p/>
 * provides actions on a path and all its subdirectories and files.<p/>
 * TODO: rename/move, copy, search/replace
 * 
 * @author Thomas Schneider
 */
public class Filesystem {
	protected void deleteDirectory(String dir) {
		try {
			Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					if (e == null) {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					} else {
						// directory iteration failed
						throw e;
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
