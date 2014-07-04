package org.korsakow.services.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collection;
import java.util.HashSet;

import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.util.PluginManagerUtil;

import org.apache.log4j.Logger;
import org.korsakow.ide.Application;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.services.plugin.export.ExportPlugin;

public class PluginHelper
{
	private static final Logger logger = Logger.getLogger(PluginHelper.class);
	
	private static File getJar(KorsakowPlugin plugin) throws IOException, URISyntaxException {
		CodeSource src = plugin.getClass().getProtectionDomain().getCodeSource();
		if (src == null)
			throw new IOException("Could not determine jar file");
		URL url = src.getLocation();
		return new File(url.toURI());
	}
	
	public static File ensurePluginsDir() throws IOException {
		File dir = new File(Application.getKorsakowHome(), "plugins");
		FileUtil.mkdirs(dir);
		return dir;
	}
	
	public static boolean containsPlugins(String pluginFile) {
		PluginManager pluginManager = PluginManagerFactory.createPluginManager();
		pluginManager.addPluginsFrom(new File(pluginFile).toURI());
		
		PluginManagerUtil pluginUtil = new PluginManagerUtil(pluginManager);
		
		return !pluginUtil.getPlugins(KorsakowPlugin.class).isEmpty();
	}
	
	public static Collection<KorsakowPlugin> installPlugins(File pluginFile) throws IOException {
		PluginManager pluginManager = PluginManagerFactory.createPluginManager();
		pluginManager.addPluginsFrom(pluginFile.toURI());
		
		PluginManagerUtil pluginUtil = new PluginManagerUtil(pluginManager);
		
		Collection<KorsakowPlugin> plugins = new HashSet<KorsakowPlugin>();
		Collection<ExportPlugin> exportPlugins = new HashSet<ExportPlugin>(pluginUtil.getPlugins(ExportPlugin.class));
		plugins.addAll(exportPlugins);
		
		if (exportPlugins.isEmpty()) {
			
		} else {
			File pluginsDir = ensurePluginsDir();
			final File copiedPlugin = new File(pluginsDir, pluginFile.getName());
			for (ExportPlugin plugin : exportPlugins) {
				logger.info(String.format("Installing Export Plugin: %s to %s", plugin.getName(), copiedPlugin.getAbsolutePath()));
				FileUtil.copyFile(pluginFile, copiedPlugin);
			}
		}
		return plugins;
	}
	
	public static void uninstallPlugin(KorsakowPlugin plugin) throws IOException, URISyntaxException {
		File pluginsDir = ensurePluginsDir();
		 
		File jar = getJar(plugin);
		
		// Extra paranoia around deleting files: make sure its in the plugins dir.
		if (!jar.getAbsolutePath().startsWith(pluginsDir.getAbsolutePath()))
			throw new IOException("Plugin is not in the plugins directory");
		FileUtil.delete(jar);
	}
}