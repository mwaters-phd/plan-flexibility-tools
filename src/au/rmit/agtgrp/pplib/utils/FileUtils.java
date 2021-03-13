package au.rmit.agtgrp.pplib.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileUtils {

	private FileUtils() { }

	public static Path getFile(String name) {
		//first check user dir
		Path path = findFileInDirectory(new File(System.getProperty("user.dir")),  name);
		if (path != null)
			return path;
		//now check classpath
		URL url = ClassLoader.getSystemResource(name);
		if (url == null)
			return null;
		else
			try {
				return resourceToPath(url.toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	public static Path getFileInJar(Path jarPath, String fileName) throws IOException, URISyntaxException {
		URI jarUri = jarPath.toUri();
		return resourceToPath(URI.create("jar:" + jarUri.toString() + "!/" + fileName));
	}

	public static Path resourceToPath(URI uri) throws IOException, URISyntaxException {
		//URI uri = resource.toURI();

		String scheme = uri.getScheme();
		if (scheme.equals("file"))
			return Paths.get(uri);
		if (!scheme.equals("jar"))
			throw new IllegalArgumentException("Cannot convert to Path: " + uri);

		String s = uri.toString();
		int separator = s.indexOf("!/");
		String entryName = s.substring(separator + 2);
		URI fileURI = URI.create(s.substring(0, separator));

		try {
			FileSystem fs = FileSystems.newFileSystem(fileURI, Collections.<String, Object>emptyMap());
			Path p = fs.getPath(entryName);
			//fs.close();
			return p;
		}
		catch (FileSystemAlreadyExistsException e) {
			FileSystem fs =  FileSystems.getFileSystem(fileURI);
			Path p = fs.getPath(entryName);
			//fs.close();
			return p;

		}
	}


	public static Path findFileInDirectory(File dir, String fileName) {
		for (File child : dir.listFiles()) {
			if (child.isFile() && child.getName().equals(fileName))
				return Paths.get(child.getAbsolutePath());
			else if (child.isDirectory()) {
				Path pathToFile = findFileInDirectory(child, fileName);
				if (pathToFile != null)
					return pathToFile;
			}
		}
		return null;
	}

	public static List<String> readFile(File file) {
		try {
			return Files.readAllLines(Paths.get(file.toURI()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeFile(File file, String data) {
		writeFile(Paths.get(file.toURI()), data);
	}
	
	public static void writeFile(Path path, String data) {

		try {
			if (!Files.exists(path.getParent()))
				Files.createDirectories(path.getParent());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
			writer.write(data);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static List<Path> getAllFiles(File directory) {
		List<Path> files = new ArrayList<Path>();
		recursiveGetAllFiles(directory, files);
		return files;
	}

	private static void recursiveGetAllFiles(File f, List<Path> files) {
		if (f.isFile())
			files.add(Paths.get(f.toString()));
		else if (f.isDirectory()) {
			for (File file : f.listFiles())
				recursiveGetAllFiles(file, files);
		}
	}

	public static void recursivelyDelete(File f) throws FileNotFoundException {
		if (!f.exists())
			return;
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				recursivelyDelete(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}

	public static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
		//Check if sourceFolder is a directory or file
		//If sourceFolder is file; then copy the file directly to new location
		if (sourceFolder.isDirectory()) {
			//Verify if destinationFolder is already present; If not then create it
			if (!destinationFolder.exists()) 
				destinationFolder.mkdir();

			//Get all files from source directory
			String files[] = sourceFolder.list();

			//Iterate over all files and copy them to destinationFolder one by one
			for (String file : files) 
			{
				File srcFile = new File(sourceFolder, file);
				File destFile = new File(destinationFolder, file);

				//Recursive function call
				copyFolder(srcFile, destFile);
			}
		}
		else {
			//Copy the file content from one place to another 
			Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static void addJarToClassPath(Path jarFile) throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {

		//load
		URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<?> sysClass = URLClassLoader.class;
		Method sysMethod = sysClass.getDeclaredMethod("addURL", new Class[] {URL.class});
		sysMethod.setAccessible(true);
		sysMethod.invoke(sysLoader, new Object[]{ jarFile.toUri().toURL()});
	}

	public static void serialize(Object o, File file) {
		try {
			String fileName = file.getAbsolutePath();
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
			out.writeObject(o);
			out.close();
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserialize(File file) {
		try
		{
			String fileName = file.getAbsolutePath();
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
			T t = (T) in.readObject();
			in.close();
			return t;
		}
		catch(IOException | ClassNotFoundException | ClassCastException e) {
			throw new RuntimeException(e);
		} 
	}

}
