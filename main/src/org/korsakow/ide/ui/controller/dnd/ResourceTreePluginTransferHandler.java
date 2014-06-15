/**
 * 
 */
package org.korsakow.ide.ui.controller.dnd;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.korsakow.ide.Application;
import org.korsakow.ide.ui.dnd.AggregateFileTransferHandler.FileTransferHandler;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.ide.util.Util;
import org.korsakow.services.plugin.KorsakowPlugin;
import org.korsakow.services.plugin.PluginHelper;

class ResourceTreePluginTransferHandler implements FileTransferHandler
{
	public boolean importData(List<File> files)
	{
		for ( File file : files )
			if ( !"jar".equalsIgnoreCase( FileUtil.getFileExtension( file.getName() ) ) )
				return false;
		
		try {
			Collection<String> installed = new HashSet<String>();
			for ( File file : files ) {
				for (KorsakowPlugin plugin: PluginHelper.installPlugins(file)) {
					installed.add(plugin.getName());
				}
			}
			
			Application app = Application.getInstance();
			if (installed.isEmpty()) {
				app.showAlertDialog("No plugins installed", "No plugins were found");
			} else {
				String message = String.format("The following plugins were successfully installed. Please restart Korsakow.\n\t%s", Util.join(installed, "\t\n"));
				app.showAlertDialog("Plugins installed", message);
			}
			return true;
		} catch (Exception e) {
			Application app = Application.getInstance();
			app.showUnhandledErrorDialog(e);
			return false;
		}
	}
}