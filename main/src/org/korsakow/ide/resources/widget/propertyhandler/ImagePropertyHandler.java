/**
 * 
 */
package org.korsakow.ide.resources.widget.propertyhandler;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JList;

import org.dsrg.soenea.domain.MapperException;
import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.mapper.input.ImageInputMapper;
import org.korsakow.ide.resources.widget.DefaultPropertyHandler;
import org.korsakow.ide.resources.widget.WidgetModel;
import org.korsakow.ide.ui.components.model.KComboboxModel;

public class ImagePropertyHandler extends DefaultPropertyHandler
{
	@Override
	public void initializeEditor(Collection<? extends WidgetModel> widgets, final JComboBox editor, String propertyName) {
		super.initializeEditor(widgets, editor, propertyName);
		editor.setEditable(false);
		editor.setRenderer(this);
 		Object value = getCommonValue(widgets, propertyName);
		try {
			List<IImage> images = ImageInputMapper.findAll();
			List<Long> ids = new ArrayList<Long>();
			ids.add(null);
			for (IImage image : images)
				ids.add(image.getId());
			editor.setModel(new KComboboxModel(ids));
		} catch (MapperException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public Component getPropertyRenderer(String propertyName, Object propertyValue)
	{
//		System.out.println(propertyName + "\t" + propertyValue);
		return getListCellRendererComponent(null, propertyValue, -1, false, false);
	}
	@Override
	public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
	{
		Long entry = (Long)value;
		if (entry != null) {
			IImage image;
			try {
				image = ImageInputMapper.map(entry);
			} catch (MapperException e) {
				throw new RuntimeException(e);
			}
			setText(image.getName());
		} else {
			setText("--");
		}
		setPreferredSize(new Dimension(70, 20));
		return this;
	}
}
