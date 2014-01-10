package org.korsakow.ide.ui.interfacebuilder.widget;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.dsrg.soenea.domain.MapperException;
import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.mapper.input.ImageInputMapper;
import org.korsakow.ide.Application;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.resources.ResourceType;
import org.korsakow.ide.resources.WidgetType;
import org.korsakow.ide.resources.media.MediaFactory;
import org.korsakow.ide.resources.media.Playable;
import org.korsakow.ide.resources.property.LongProperty;
import org.korsakow.ide.resources.widget.DefaultTableWidgetPropertiesEditor;
import org.korsakow.ide.resources.widget.ScalingPolicy;
import org.korsakow.ide.resources.widget.WidgetComponent;
import org.korsakow.ide.resources.widget.WidgetModel;
import org.korsakow.ide.resources.widget.propertyhandler.ImagePropertyHandler;
import org.korsakow.ide.resources.widget.propertyhandler.ScalingPolicyPropertyHandler;
import org.korsakow.ide.ui.components.ResourceIcon;
import org.korsakow.ide.ui.interfacebuilder.widget.MediaArea.AspectRatioWrapper;
import org.korsakow.ide.util.UIResourceManager;

public class RestartButton extends WidgetModel
{
	private static class RestartButtonWidgetComponent extends WidgetComponent
	{
		private ResourceIcon icon;
		private Component mediaComponent;
		private AspectRatioWrapper wrapper;
		private Playable playable;
		private ImageLabel defaultIcon;
		public RestartButtonWidgetComponent(WidgetModel owner)
		{
			super(owner);
		}
		@Override
		protected void initUI()
		{
			super.initUI();
			setLayout(new BorderLayout());
			setSize(80, 80);
			
			icon = new ResourceIcon();
			
			defaultIcon = new ImageLabel((ImageIcon)UIResourceManager.getIcon(UIResourceManager.ICON_CONTROL_RESTART));
			
			setImage(null);
		}
		@Override
		public void setEnabled(boolean b)
		{
			super.setEnabled(b);
		}
		public void setImage(Long imageId)
		{
			IImage image = null;
			if (imageId != null)
				try {
					image = ImageInputMapper.map(imageId);
				} catch (MapperException e) {
					Application.getInstance().showUnhandledErrorDialog(LanguageBundle.getString("general.errors.uncaughtexception.title"), e);
				}
			
			if (image != null)
				icon.setResource(ResourceType.forId(image.getType()).getIcon(), image.getName());
			else
				icon.clear();
			
			if (playable != null) {
				playable.dispose();
				playable = null;
			}
			if (mediaComponent != null) {
				mediaComponent.getParent().remove(mediaComponent);
				mediaComponent = null;
			}

			if (image != null) {
				playable = MediaFactory.getMediaNoThrow(image);
				mediaComponent = playable.getComponent();
				wrapper = new AspectRatioWrapper(mediaComponent, playable, ScalingPolicy.MaintainAspectRatio);
				remove(defaultIcon);
				add(wrapper, BorderLayout.CENTER);
				setOpaque(true);
			} else {
				final Icon i = UIResourceManager.getIcon(UIResourceManager.ICON_CONTROL_RESTART);
				add(defaultIcon, BorderLayout.CENTER);
				setOpaque(false);
			}
			
			revalidate();
			repaint();
		}
		@Override
		public RestartButton getWidget() {
			return (RestartButton)super.getWidget();
		}
		public AspectRatioWrapper getWrapper() {
			return wrapper;
		}
	}
	private class RestartButtonWidgetEditor extends DefaultTableWidgetPropertiesEditor
	{
		public RestartButtonWidgetEditor(WidgetModel widget)
		{
			super(widget);
			addPropertyHandler("upImageId", new ImagePropertyHandler());
			addPropertyHandler("scalingPolicy", new ScalingPolicyPropertyHandler());
		}
	}

	private Long upImageId;
	
	public RestartButton()
	{
		super(WidgetType.RestartButton);
		
		addProperty(new LongProperty("upImageId") {
			@Override
			public Long getValue() { return getUpImage(); }
			@Override
			public void setValue(Long value) {
				setUpImage(value);
			}
		});
	}
	@Override
	protected WidgetComponent createComponent()
	{
		return new RestartButtonWidgetComponent(this);
	}
	@Override
	public RestartButtonWidgetComponent getComponent() {
		return (RestartButtonWidgetComponent)super.getComponent();
	}
	@Override
	protected RestartButtonWidgetEditor createWidgetEditor()
	{
		return new RestartButtonWidgetEditor(this);
	}
	private void setUpImage(Long upImageId)
	{
		getComponent().setImage(upImageId);
		Long oldImage = upImageId;
		this.upImageId = upImageId;
		firePropertyChange("upImage", oldImage, upImageId);
	}
	private Long getUpImage()
	{
		return upImageId;
	}
}
