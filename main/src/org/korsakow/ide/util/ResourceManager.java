package org.korsakow.ide.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;

public class ResourceManager
{
	/**
	 * This exists to abstract away the source of all resources primarily for testing purposes.
	 * It is also a sign of bad design since the ResourceManager itself should have been an interface =P
	 * TODO: refactor, refactor, refactor!
	 * @author d
	 */
	public static interface IResourceSource
	{
		File getResourceFile(String name);
		InputStream getResourceStream(String name);
	}
	private static class MyResourceSource implements IResourceSource
	{
		private static final String RESOURCE_BASE_PATH = "/resources/";
		private final Class<?> clazz;
		public MyResourceSource(Class<?> clazz)
		{
			this.clazz = clazz;
		}
		public File getResourceFile(String name) throws MissingResourceException {
			File parentDir;
			try {
				parentDir = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				// in dist builds parentDir will be the Jar file; running from the IDE it will just be the path
				if (!parentDir.isDirectory())
					parentDir = parentDir.getParentFile();
				
			} catch (URISyntaxException e) {
				throw new MissingResourceException(e.getMessage(), clazz.getCanonicalName(), name);
			}
			// TODO: why do we prefix with ./ ?
			File file = new File(parentDir, File.separator  + RESOURCE_BASE_PATH + name);
			if (!file.exists())
				throw new MissingResourceException(file.getAbsolutePath(), clazz.getCanonicalName(), name);
			return file;
		}
		public InputStream getResourceStream(String name) throws MissingResourceException {
			try {
				return new FileInputStream(getResourceFile(name));
			} catch (FileNotFoundException e) {
				throw new MissingResourceException(e.getMessage(), clazz.getCanonicalName(), name);
			}
		}
		
	}
	private static List<IResourceSource> resourceSources = Collections.synchronizedList(new ArrayList<IResourceSource>());
	static {
		resourceSources.add(0, new MyResourceSource(ResourceManager.class));
	}
	
	public static void addResourceSource(IResourceSource resourceSource) {
		if (resourceSource == null)
			throw new NullPointerException();
		resourceSources.add(0, resourceSource);
	}
	public static void setResourceSource(IResourceSource resourceSource)
	{
		if (resourceSource == null)
			throw new NullPointerException();
		resourceSources.clear();
		resourceSources.add(0, resourceSource);
	}
	public static IResourceSource getResourceSource() {
		return resourceSources.get(0);
	}
	public static File getResourceFile(String name)
	{
		MissingResourceException firstException = null;
		for (IResourceSource resourceSource : resourceSources) {
			try {
				File file = resourceSource.getResourceFile(name);
				if (file != null)
					return file;
			} catch (MissingResourceException e) {
				if (firstException == null)
					firstException = e;
				continue;
			}
		}
		if (firstException != null)
			throw firstException;
		throw new MissingResourceException(name, ResourceManager.class.getCanonicalName(), name);
	}
	public static InputStream getResourceStream(String name)
	{
		MissingResourceException firstException = null;
		for (IResourceSource resourceSource : resourceSources) {
			try {
				InputStream stream = resourceSource.getResourceStream(name);
				if (stream != null)
					return stream;
			} catch (MissingResourceException e) {
				if (firstException == null)
					firstException = e;
				continue;
			}
		}
		if (firstException != null)
			throw firstException;
		throw new MissingResourceException(name, ResourceManager.class.getCanonicalName(), name);
	}
}
