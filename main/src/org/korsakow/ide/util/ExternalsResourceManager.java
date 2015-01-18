package org.korsakow.ide.util;

import java.io.File;
import java.io.IOException;

/**
 * Externals are anything that must be present in the filesystem in order to work. For example platform executables.
 * 
 * @author d
 *
 */
public class ExternalsResourceManager
{
	public static final String FFMPEG = "ffmpeg/";
	public static final String FFMPEG_PRESETS = "ffmpeg/ffpresets";
	public static final String FFMPEG_OSX = "ffmpeg/osx/ffmpeg";
	public static final String FFMPEG_WIN = "ffmpeg/win32/ffmpeg.exe";
	
	public static File getPlatformScript(String name) throws IOException
	{
		String path = "script/";
		switch (Platform.getOS())
		{
		case MAC:
			path = "scripts/osx/";
			break;
		case WIN:
			path = "scripts/win/";
			break;
		case NIX:
			path = "scripts/nix/";
			break;
		}
		path += name;
		File file = getExternalFile(path);
		return file;
	}
	
	public static File getExternalFile(String name)
	{
		return ResourceManager.getResourceFile(name);
	}
}
