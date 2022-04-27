package de.tsl2.nano.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import de.tsl2.nano.core.util.Util;

/**
 * ON CONSTRUCTION
 * <p/>
 * provides actions on a path and all its subdirectories and files.<p/>
 * TODO: rename/move, copy, search/replace
 * 
 * @author Thomas Schneider
 */
public class Filesystem {
	public static int deleteDirectory(String dir) {
		return recurse(dir, null, null, f -> {Util.trY(() -> Files.delete(f)); return true;});
	}
	public static int copy(String dir, String target) {
		return recurse(dir, null, null, f -> {Util.trY(() -> Files.copy(f, Paths.get(target))); return true;});
	}
	public static int move(String dir, String target) {
		return recurse(dir, null, null, f -> {Util.trY(() -> Files.move(f, Paths.get(target))); return true;});
	}
	public static int recurse(String dir, String includes, String excludes, Function<Path, Boolean> action) {
		try {
			final AtomicInteger count = new AtomicInteger(0);
			SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (includes(file, includes, excludes))
						if (action.apply(file))
							count.getAndAdd(1);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					if (e == null) {
						if (includes(dir, includes, excludes))
							if (action.apply(dir))
								count.getAndAdd(1);
						return FileVisitResult.CONTINUE;
					} else {
						// directory iteration failed
						throw e;
					}
				}

				private boolean includes(Path file, String includes, String excludes) {
					String p = file.toAbsolutePath().toString();
					return !(excludes != null && p.matches(excludes)) && (includes != null && p.matches(includes));
				}
			};
			Files.walkFileTree(Paths.get(dir), visitor);
			return count.get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
