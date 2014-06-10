package org.korsakow.ide.ui.controller.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.korsakow.ide.Application;
import org.korsakow.ide.resources.ResourceType;
import org.korsakow.ide.ui.ProjectExplorer;
import org.korsakow.ide.ui.components.tree.KNode;
import org.korsakow.ide.ui.components.tree.ResourceNode;
import org.korsakow.ide.ui.resourceexplorer.ResourceTreeTable;
import org.korsakow.services.plugin.PluginRegistry;
import org.korsakow.services.plugin.export.ExportPlugin;

public class ExportMenuAction implements ActionListener
{
	private final ResourceTreeTable resourceTreeTable;
	private final List<JMenuItem> defaultItems = new ArrayList<JMenuItem>(); 
	public ExportMenuAction(ResourceTreeTable resourceTreeTable)
	{
		this.resourceTreeTable = resourceTreeTable;
		
		ProjectExplorer projectExplorer = Application.getInstance().getProjectExplorer();
		JMenu exportMenu = (JMenu)projectExplorer.getMenu(ProjectExplorer.Action.MenuFileExport);
		for (Component child : exportMenu.getMenuComponents()) {
			defaultItems.add((JMenuItem)child); 
		}
	}
	public void actionPerformed(ActionEvent event)
	{
		boolean enabled = false;
		KNode selectedNode = resourceTreeTable.getSelectedNode();
		if (selectedNode != null) {
			
			if (selectedNode instanceof ResourceNode) {
				ResourceNode resourceNode = (ResourceNode)selectedNode;
		
				if (resourceNode.getResourceType() == ResourceType.INTERFACE) {
					enabled = true;
				}
			}
		}
		ProjectExplorer projectExplorer = Application.getInstance().getProjectExplorer();
		projectExplorer.getMenu(ProjectExplorer.Action.MenuFileExportInterface).setEnabled(enabled);
		
		
		JMenu exportMenu = (JMenu)projectExplorer.getMenu(ProjectExplorer.Action.MenuFileExport);
		
		exportMenu.removeAll();
		for (JMenuItem item : defaultItems)
			exportMenu.add(item);
		
		for (ExportPlugin plugin : PluginRegistry.get().getExportPlugins()) {
			JMenuItem item = new JMenuItem();
			item.setText(plugin.getMenuItemLabel());
			item.addActionListener(new PluginExportAction(plugin));
			exportMenu.add(item);
		}
	}
}
