/**
 * 
 */
package org.korsakow.services.plugin;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.korsakow.ide.util.ResourceManager;
import org.korsakow.ide.util.ResourceManager.IResourceSource;


/**
 * Interface presented to plugins to interact with the main system.
 */
public interface KorsakowSystem
{
	public void addResourceSource(IResourceSource resourceSource);
}

@PluginImplementation
class KorsakowSystemImpl implements KorsakowSystem {
	public void addResourceSource(IResourceSource resourceSource) {
		ResourceManager.addResourceSource(resourceSource);
	}
}