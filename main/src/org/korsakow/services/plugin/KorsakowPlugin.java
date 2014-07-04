/**
 * 
 */
package org.korsakow.services.plugin;

import net.xeoh.plugins.base.Plugin;


public interface KorsakowPlugin extends Plugin
{
	void initialize() throws KorsakowPluginException;
	void shutdown();
	
	String getName();
	String getVersion();
}
