package org.korsakow.ide.ui.interfacebuilder.widget;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;

import org.korsakow.ide.resources.WidgetType;
import org.korsakow.ide.resources.widget.WidgetComponent;
import org.korsakow.ide.resources.widget.WidgetModel;
import org.korsakow.ide.util.UIResourceManager;

public class RestartButton extends WidgetModel
{
	private static class RestartButtonWidgetComponent extends WidgetComponent
	{
		public RestartButtonWidgetComponent(WidgetModel owner)
		{
			super(owner);
		}
		@Override
		protected void initUI()
		{
			super.initUI();
			setOpaque(false);
			setBackground(null);
			setLayout(new BorderLayout());
			final ImageIcon icon = (ImageIcon)UIResourceManager.getIcon(UIResourceManager.ICON_CONTROL_RESTART);
			add(new ImageLabel(icon));
			setSize(icon.getIconWidth(), icon.getIconHeight());
		}
	}

	public RestartButton()
	{
		super(WidgetType.RestartButton);
	}
	@Override
	protected WidgetComponent createComponent()
	{
		return new RestartButtonWidgetComponent(this);
	}
}
