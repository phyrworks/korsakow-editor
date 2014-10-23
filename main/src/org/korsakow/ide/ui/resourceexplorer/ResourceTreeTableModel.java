package org.korsakow.ide.ui.resourceexplorer;

import org.korsakow.ide.ui.components.tree.FolderNode;
import org.korsakow.ide.ui.components.tree.KNode;
import org.korsakow.ide.ui.components.tree.KTreeTableModel;

public interface ResourceTreeTableModel extends KTreeTableModel
{
	@Override
	FolderNode getRoot();
	KNode remove(Long id);
	KNode findResource(Long id);
	FolderNode findFolder(FolderNode parent, String name);
	void fireChanged();
	void fireChanged(KNode changed);
}
