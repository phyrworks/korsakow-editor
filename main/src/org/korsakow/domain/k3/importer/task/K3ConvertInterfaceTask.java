/**
 * 
 */
package org.korsakow.domain.k3.importer.task;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import org.korsakow.domain.InterfaceFactory;
import org.korsakow.domain.WidgetFactory;
import org.korsakow.domain.interf.IInterface;
import org.korsakow.domain.interf.IWidget;
import org.korsakow.domain.k3.K3Interface;
import org.korsakow.domain.k3.K3Widget;
import org.korsakow.domain.k3.importer.K3ImportException;
import org.korsakow.domain.k3.importer.K3ImportReport;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.resources.WidgetType;
import org.korsakow.ide.resources.widget.FontStyle;
import org.korsakow.ide.resources.widget.FontWeight;
import org.korsakow.ide.resources.widget.HorizontalTextAlignment;
import org.korsakow.ide.resources.widget.PreviewTextEffect;
import org.korsakow.ide.resources.widget.PreviewTextMode;
import org.korsakow.ide.resources.widget.ScalingPolicy;
import org.korsakow.ide.resources.widget.TextDecoration;
import org.korsakow.ide.resources.widget.VerticalTextAlignment;
import org.korsakow.ide.task.AbstractTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.util.StrongReference;
import org.korsakow.services.util.ColorFactory;

public class K3ConvertInterfaceTask extends AbstractTask
{
	private final K3ImportReport report;
	private final StrongReference<K3Interface> k3Interface;
	private final StrongReference<IInterface> k5InterfaceRef;
	
	public K3ConvertInterfaceTask(StrongReference<K3Interface> k3Interface, K3ImportReport report, StrongReference<IInterface> k5Interface)
	{
		this.k3Interface = k3Interface;
		this.report = report;
		k5InterfaceRef = k5Interface;
	}
	@Override
	public String getTitleString()
	{
		return LanguageBundle.getString("import.task.convertinterface");
	}
	@Override
	public void runTask() throws TaskException
	{
		if (k3Interface.get() != null)
			try {
				importInterface(k3Interface.get());
			} catch (K3ImportException e) {
				throw new TaskException(e);
			}
	}
	private void addWidgetTextProperties(IWidget widget) {
		widget.setDynamicProperty("fontColor", ColorFactory.formatCSS(Color.white));
		widget.setDynamicProperty("fontFamily", "Courier");
		widget.setDynamicProperty("fontSize", 10);
		widget.setDynamicProperty("fontWeight", FontWeight.Normal);
		widget.setDynamicProperty("fontStyle", FontStyle.Normal);
		widget.setDynamicProperty("textDecoration", TextDecoration.None);
	}
	private void importInterface(K3Interface k3Interface) throws K3ImportException
	{
		IInterface k5Interface = InterfaceFactory.createNew();
		k5Interface.setName("Interface");
		k5Interface.setGridWidth(20);
		k5Interface.setGridHeight(20);
		
		Collection<IWidget> k5Widgets = new ArrayList<IWidget>();
		int autoLinkCounter = 0;
		for (K3Widget k3Widget : k3Interface.widgets)
		{
			IWidget k5Widget = null;
			if (K3Widget.MAIN.equals(k3Widget.type)) {
				k5Widget = WidgetFactory.createNew(WidgetType.MainMedia.getId());
				k5Widget.setDynamicProperty("scalingPolicy", ScalingPolicy.ScaleDownMaintainAspectRatio);
			} else
			if (K3Widget.LOADING.equals(k3Widget.type)) {
				report.addUnsupported("Video loading bar Widget", "Widget");
				k5Widget = WidgetFactory.createNew(WidgetType.Scrubber.getId());
				k5Widget.setDynamicProperty("foregroundColor", ColorFactory.formatCSS(Color.white));
				k5Widget.setDynamicProperty("backgroundColor", ColorFactory.formatCSS(Color.black));
				k5Widget.setDynamicProperty("barWidth", 5);
				k5Widget.setDynamicProperty("barHeight", 5);
				k5Widget.setDynamicProperty("interactive", false);
				k5Widget.setDynamicProperty("loading", true);
			} else
			if (K3Widget.PREVIEW.equals(k3Widget.type)) {
				k5Widget = WidgetFactory.createNew(WidgetType.SnuAutoLink.getId());
				k5Widget.setDynamicProperty("index", autoLinkCounter++);
				addWidgetTextProperties(k5Widget);
				k5Widget.setDynamicProperty("horizontalTextAlignment", HorizontalTextAlignment.Left);
				k5Widget.setDynamicProperty("verticalTextAlignment", VerticalTextAlignment.Top);
				k5Widget.setDynamicProperty("previewTextMode", PreviewTextMode.ALWAYS);
				k5Widget.setDynamicProperty("previewTextEffect", PreviewTextEffect.NONE);
				k5Widget.setDynamicProperty("scalingPolicy", ScalingPolicy.MaintainAspectRatio);
			} else
			if (K3Widget.SUBTITLE.equals(k3Widget.type)) {
				k5Widget = WidgetFactory.createNew(WidgetType.Subtitles.getId());
				addWidgetTextProperties(k5Widget);
			} else
			if (K3Widget.INSERTTEXT.equals(k3Widget.type)) {
				k5Widget = WidgetFactory.createNew(WidgetType.InsertText.getId());
				addWidgetTextProperties(k5Widget);
			} else
				report.addUnsupported(k3Widget.type, "Widget");
			
			if (k5Widget != null) {
				k5Widgets.add(k5Widget);
				k5Widget.setX(k3Widget.left);
				k5Widget.setY(k3Widget.top);
				k5Widget.setWidth(k3Widget.right - k3Widget.left);
				k5Widget.setHeight(k3Widget.bottom - k3Widget.top);
			}
		}
		
		k5Interface.setWidgets(k5Widgets);
		
		k5InterfaceRef.set(k5Interface);
	}
}