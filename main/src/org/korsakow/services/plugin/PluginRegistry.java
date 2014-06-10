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
		exportPlugins.add(plugin);
	}
	
	public Collection<ExportPlugin> getExportPlugins() {
		return Collections.unmodifiableCollection(exportPlugins);
	}
}