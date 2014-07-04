package org.korsakow.services.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.korsakow.services.plugin.export.ExportPlugin;

public class PluginRegistry
{
	private static PluginRegistry instance = new PluginRegistry();
	public static PluginRegistry get() {
		return instance;
	}

	private final Collection<KorsakowPlugin> plugins = new ArrayList<KorsakowPlugin>();
	
	public Collection<KorsakowPlugin> getPlugins() {
		return Collections.unmodifiableCollection(plugins);
	}
	
	public void register(KorsakowPlugin plugin) throws KorsakowPluginException {
		KorsakowPlugin existing = find(plugins, plugin.getClass());
		if (existing != null) {
			existing.shutdown();
		}
		plugins.add(plugin);
		plugin.initialize();
	}
	
	public void unregister(KorsakowPlugin plugin) {
		plugin.shutdown();
		plugins.remove(plugin);
	}
	
	public Collection<ExportPlugin> getExportPlugins() {
		Collection<ExportPlugin> exportPlugins = new ArrayList<ExportPlugin>();
		for (KorsakowPlugin plugin : plugins) {
			if (plugin instanceof ExportPlugin)
				exportPlugins.add((ExportPlugin)plugin);
		}
		return exportPlugins; 
	}
	
	private <T extends KorsakowPlugin> T find(Collection<T> plugins, Class<? extends KorsakowPlugin> klass) {
		for (T plugin : plugins) {
			if (plugin.getClass().getName().equals(klass.getName()))
				return plugin;
		}
		return null;
	}
}