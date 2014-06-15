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

	private final Collection<ExportPlugin> exportPlugins = new ArrayList<ExportPlugin>();
	
	public void register(ExportPlugin plugin) {
		ExportPlugin existing = find(exportPlugins, plugin.getName());
		if (existing != null) {
			plugin.shutdown();
		}
		exportPlugins.add(plugin);
		plugin.initialize();
	}
	
	public Collection<ExportPlugin> getExportPlugins() {
		return Collections.unmodifiableCollection(exportPlugins);
	}
	
	private <T extends KorsakowPlugin> T find(Collection<T> plugins, String name) {
		for (T plugin : plugins) {
			if (plugin.getName().equals(name))
				return plugin;
		}
		return null;
	}
}