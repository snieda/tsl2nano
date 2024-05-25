package de.tsl2.nano.core.util;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * provides actions on a path and all its subdirectories and files.<p/>
 * all use the base function {@link #recurse(String, String, String, Function)}
 * 
 * @author Thomas Schneider
 */
public class FilePath {
	/** deletes all files in a given directory */
	public static int deleteDirectory(String dir) {
		return recurse(dir, null, null, f -> {
			Util.trY(() -> Files.delete(f));
			return true;
		});
	}

	/** copies all files in a given directory to the given target */
	public static int copy(String dir, String target) {
		return recurse(dir, null, null, f -> {
			Path targetPath = Paths.get(evaluateTargetFile(dir, f, target));
			if (targetPath.toFile().exists()) {
				return false;
			}
			Util.trY(() -> Files.copy(f, targetPath, StandardCopyOption.COPY_ATTRIBUTES));
			return true;
		});
	}

	private static String evaluateTargetFile(String dir, Path f, String target) {
		File fSource = new File(dir);
		if (fSource.exists() && fSource.isDirectory()) {
			Path targetPath = Paths.get(target);
			try {
				Files.createDirectories(targetPath);
				String relativeFilePath = StringUtil.substring(f.toFile().getCanonicalPath(),
						fSource.getCanonicalPath(), null);
				targetPath = Paths.get(target, relativeFilePath);
				Files.createDirectories(targetPath.getParent());
				target = targetPath.toString();
			} catch (IOException e) {
				throw new IllegalArgumentException("error on accessing file " + targetPath, e);
			}
		}
		log("\t%s -> %s", f, target);
		return target;
	}

	/** moves all files in a given directory to the given target */
	public static int move(String dir, String target) {
		return recurse(dir, null, null, f -> {
			Util.trY(() -> Files.move(f, Paths.get(evaluateTargetFile(dir, f, target))));
			return true;
		});
	}

	/**
	 * @param <RESULT> function result type, to be collected as return value
	 * @param dir base directory to start on
	 * @param includes optional file name include regular expression 
	 * @param excludes optional file name exclude regular expression
	 * @param contentRegEx regular expression to be matched inside the files content
	 * @param fct action to do if file name and content matches
	 * @return all results of all function calls
	 */
	public static <RESULT> Collection<RESULT> foreach(String dir, String includes, String excludes, String contentRegEx,
			Function<Path, RESULT> fct) {
		final LinkedList<RESULT> results = new LinkedList<>();
		recurse(dir, includes, excludes, f -> {
			String l;
			try (BufferedReader reader = Files.newBufferedReader(f)) {
				while ((l = reader.readLine()) != null) {
					if (l.matches(contentRegEx)) {
						RESULT res = fct.apply(f);
						if (res != null) {
							results.add(res);
							return res;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		});
		return results;
	}

	/**
	 * as Paths.get(file).foreach(consumer) walks only the path elements itself from root, this walks through all recursive files/paths starting on given dir.
	 * 
	 * @param dir base directory to start on
	 * @param includes optional file name include regular expression 
	 * @param excludes optional file name exclude regular expression
	 * @param action if file name matches, this function will be executed. if the function returns a value != null, the result count will be increased.
	 * @return count of function calls with result != null
	 */
	public static <R> int recurse(String dir, String includes, String excludes, Function<Path, R> action) {
		log("filepath action (dir=%s, includes=%s, excludes=%s)\n\t=> %s", dir, includes, excludes, action);
		try {
			final AtomicInteger count = new AtomicInteger(0);
			SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (includes(file, includes, excludes))
						if (action.apply(file) != null)
							count.getAndAdd(1);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					if (e == null) {
						if (includes(dir, includes, excludes))
							if (action.apply(dir) != null)
								count.getAndAdd(1);
						return FileVisitResult.CONTINUE;
					} else {
						// directory iteration failed
						throw e;
					}
				}

				private boolean includes(Path file, String includes, String excludes) {
					String p = file.toAbsolutePath().toString();
					return !(excludes != null && p.matches(excludes)) && (includes == null || p.matches(includes));
				}
			};
			Files.walkFileTree(Paths.get(dir), visitor);
			log("\tfile count: %d", count.get());
			return count.get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void log(String txt, Object... args) {
		System.out.println(String.format(txt, args));
	}

	public static byte[] read(String file) {
		// the absolute path is used to respect changes on 'user.dir' (in tests)
		return Util.trY(() -> Files.readAllBytes(Paths.get(absolutePath(file))));
	}

	public static Path write(String file, byte[] bytes) {
		// the absolute path is used to respect changes on 'user.dir' (in tests)
		return Util.trY(() -> Files.write(Paths.get(absolutePath(file)), bytes, CREATE, WRITE, APPEND));
	}

	public static BufferedWriter getFileWriter(String file) {
		// the absolute path is used to respect changes on 'user.dir' (in tests)
		return Util.trY(() -> Files.newBufferedWriter(Paths.get(absolutePath(file)), CREATE, WRITE, APPEND));
	}

	private static String absolutePath(String file) {
		return file;//FileUtil.userDirFile(file).getAbsolutePath();
	}
}
