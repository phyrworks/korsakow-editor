package org.korsakow.ide.util;


public class Platform
{
	public static enum OS
	{
		MAC("mac"),
		WIN("windows"),
		NIX("linux"),
		UNKNOWN("unknown")
		;
		
		private final String canonicalName;
		private OS(String canonicalName)
		{
			this.canonicalName = canonicalName;
		}
		public String getCanonicalName()
		{
			return canonicalName;
		}
		public String getVersion()
		{
			return System.getProperty("os.version");
		}
	}
	public static OS getOS()
	{
		if (isMacOS())
			return OS.MAC;
		if (isWindowsOS())
			return OS.WIN;
		if (isLinuxOS())
			return OS.NIX;
		return OS.UNKNOWN;
	}
	public static String getArchString() {
		return System.getProperty("os.arch");
	}
	public static String getOSString() {
		return System.getProperty("os.name") + " " + System.getProperty("os.version");
	}
	public static String getArch()
	{
		return System.getProperty("os.arch");
	}
	public static boolean isLinuxOS()
	{
		final String osName =  System.getProperty("os.name", "unknown").toLowerCase();
		return osName.startsWith("linux");
	}
	public static boolean isWindowsOS()
	{
		final String osName =  System.getProperty("os.name", "unknown").toLowerCase();
		return osName.startsWith("windows");
	}
	public static boolean isMacOS()
	{
		final String osName =  System.getProperty("os.name", "unknown").toLowerCase();
		return osName.startsWith("mac") || osName.contains("darwin");
	}
}
