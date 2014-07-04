/**
 * 
 */
package org.korsakow.services.plugin.export;

import org.korsakow.services.export.Exporter;
import org.korsakow.services.plugin.KorsakowPlugin;

public interface ExportPlugin extends KorsakowPlugin 
{
	Exporter getExporter();
	String getMenuItemLabel();
}
