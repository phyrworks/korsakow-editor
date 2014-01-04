package org.korsakow.build;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class WixUtil {
	private static Set<String> generated = new HashSet<String>();
	/**
	 * Wix uses GUID's all over. I think for files/components we want to keep the same
	 * GUID across installs, so we typically use the file's path as a seed to
	 * consistently get the same hash
	 */
	public static String createGuid(String seed) {
		String guid = UUID.nameUUIDFromBytes(seed.getBytes()).toString();
		// Just a failsafe in case my way of generating UUID is flawed.
		if (!generated.add(guid))
			throw new IllegalStateException("Duplicate guid hash from: '" + seed + "'");
		return guid;
	}

	/**
	 * For lack of a better idea, we use a file's path as it's id, so
	 * we need to strip out invalid chars.
	 */
	public static String sanitizeId(String path) {
		if (!path.matches("^[a-zA-Z].*"))
			path = "_" + path;
		return path.replaceAll("[^a-zA-Z0-9_.]", "_");
	}

	public static String relative(File base, File file) throws IOException {
		String baseCanon = base.getCanonicalPath();
		String fileCanon = file.getCanonicalPath();
		if (fileCanon.startsWith(baseCanon)) {
			String sub = fileCanon.substring(baseCanon.length());
			if (sub.startsWith("/") || sub.startsWith("\\"))
				sub = sub.substring(1);
			return sub;
		}
		return file.getPath();
	}
	
	public static interface FileVisitor
	{
		void enter( File file ) throws IOException;
		void exit( File file ) throws IOException;
		void file( File file ) throws IOException;
	}
	public static void visitRecursively( File parent, FileVisitor visitor ) throws IOException {
		visitRecursively(parent, visitor, new HashSet<String>());
	}
	private static void visitRecursively( File parent, FileVisitor visitor, Set<String> visited ) throws IOException {
		String canon = parent.getCanonicalPath();
		if (visited.contains(canon))
			return;
		visited.add(canon);

		visitor.enter( parent );
		File[] children = parent.listFiles();
		if ( children != null ) {
			for ( File child : children )
				if (child.isFile())
					visitor.file(child);
			for ( File child : children )
				if (child.isDirectory())
					visitRecursively( child, visitor, visited );
		}
		visitor.exit( parent );
	}
}