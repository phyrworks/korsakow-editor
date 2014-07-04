package org.korsakow.ide.ui.controller.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.korsakow.ide.Application;
import org.korsakow.ide.ui.ProjectExplorer;
import org.korsakow.services.plugin.KorsakowPlugin;
import org.korsakow.services.plugin.PluginHelper;
import org.korsakow.services.plugin.PluginRegistry;

public class PluginsMenuAction implements ActionListener
{
	public PluginsMenuAction() {
	}
	public void actionPerformed(ActionEvent event) {
		ProjectExplorer projectExplorer = Application.getInstance().getProjectExplorer();
		JMenu pluginsMenu = (JMenu)projectExplorer.getMenu(ProjectExplorer.Action.MenuFilePlugins);
		
		pluginsMenu.removeAll();

		Collection<KorsakowPlugin> plugins = PluginRegistry.get().getPlugins();
		for (KorsakowPlugin plugin : plugins) {
			JMenu menu = new JMenu();
			menu.setText(plugin.getName());
			pluginsMenu.add(menu);
			createSubMenus(menu, plugin);
		}
	}
	
	private void createSubMenus(JMenu menu, KorsakowPlugin plugin) {
		JMenuItem versionItem = new JMenuItem(String.format("Version %s", plugin.getVersion()));
		menu.add(versionItem);
		
		menu.add(new JSeparator());
		
		JMenuItem uninstallItem = new JMenuItem("Uninstall");
		uninstallItem.addActionListener(new UninstallPluginAction(plugin));
		menu.add(uninstallItem);
	}
}

class UninstallPluginAction implements ActionListener {
	private final KorsakowPlugin plugin;
	public UninstallPluginAction(KorsakowPlugin plugin) {
		this.plugin = plugin;
	}
	@Override
	public void actionPerformed(ActionEvent event) {
		try {
			PluginHelper.uninstallPlugin(plugin);
			PluginRegistry.get().unregister(plugin);
			Application.getInstance().showAlertDialog("Plugin Uninstalled", String.format("The plugin was successfully uninstalled, please restart Korsakow."));
		} catch (Exception e) {
			Application.getInstance().showUnhandledErrorDialog(e);
		}
	}
}
