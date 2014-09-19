package org.korsakow.services.conversion;


import java.util.List;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.korsakow.ide.resources.WidgetType;
import org.korsakow.ide.util.DomUtil;
import org.korsakow.services.tdg.PropertyTDG;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ConvertUpTo26_06 extends ConversionModule
{
	public ConvertUpTo26_06(Document document)
	{
		super(document);
	}

	@Override
	public void convert() throws ConversionException
	{
		try {
			document.getDocumentElement().setAttribute("versionMajor", "5.0.6.6");
			document.getDocumentElement().setAttribute("versionMinor", "26.06");

			addMissingFields();
		} catch (XPathException e) {
			throw new ConversionException(e);
		} catch (NumberFormatException e) {
			throw new ConversionException(e);
		}
	}
	private void addMissingFields() throws XPathExpressionException
	{
		addIfMissing( "loadingColor", "#ffffff", "//Widget[widgetType=?]",
				WidgetType.Scrubber.getId() );
		addIfMissing( "interactive", "false", "//Widget[widgetType=?]",
				WidgetType.Scrubber.getId() );
		addIfMissing( "loading", "true", "//Widget[widgetType=?]",
				WidgetType.Scrubber.getId() );
	}
	
	private void addIfMissing( String name, String value, String xpath, Object... args ) throws XPathExpressionException {
		addIfMissing( name, value, helper.xpathAsList( xpath, args ) );
	}
	private void addIfMissing( String name, String value, List<Node> nodes ) {
		for (Node node :  nodes ) {
			if (DomUtil.findChildByTagName( (Element)node, name ) == null) {
				Element e = DomUtil.setString( document, (Element)node, name, value );
				PropertyTDG.setDynamicAttribute( e );
			}
		}
	}
}
