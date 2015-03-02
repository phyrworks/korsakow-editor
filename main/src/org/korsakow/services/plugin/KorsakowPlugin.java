/**
 * 
 */
package org.korsakow.services.plugin;

import net.xeoh.plugins.base.Plugin;

public interface KorsakowPlugin extends Plugin
{
	void initialize(KorsakowSystem system) throws KorsakowPluginException;
	void shutdown();
	
	String getName();
	String getVersion();

	boolean isPublic();
}
