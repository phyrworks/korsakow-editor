package org.korsakow.build;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class WixFactory {
	private static boolean win64Bit = true;
	
	private static String yesno(boolean b) {
		return b ? "yes" : "no";
	}
	
	public static Element file(Document doc, File file, String id) throws DOMException, IOException {
		Element fileElem = doc.createElement("File");
		fileElem.setAttribute("Id", id);
		//fileElem.setAttribute("Name", file.getName());
		fileElem.setAttribute("Source", file.getCanonicalPath());
		fileElem.setAttribute("KeyPath", "yes");
		return fileElem;
	}
	public static Element directory(Document doc, String id, String name) {
		Element dirElem = doc.createElement("Directory");
		dirElem.setAttribute("Id", id);
		dirElem.setAttribute("Name", name);
		return dirElem;
	}
	public static Element directoryRef(Document doc, String id) {
		Element dirElem = doc.createElement("DirectoryRef");
		dirElem.setAttribute("Id", id);
		return dirElem;
	}
	public static Element featureRef(Document doc, String id) {
		final Element featureRef = doc.createElement("Feature");
		featureRef.setAttribute("Id", id);
		return featureRef;
	}
	public static Element fragment(Document doc) {
		final Element fragment = doc.createElement("Fragment");
		return fragment;
	}
	public static Element wix(Document doc) {
		Element wix = doc.createElementNS("http://schemas.microsoft.com/wix/2006/wi", "Wix");
		return wix;
	}
	public static Element removeFile(Document doc, String id) {
		Element removeElem = doc.createElement("RemoveFile");
		removeElem.setAttribute("Id", id);
		removeElem.setAttribute("Name", "*");
		removeElem.setAttribute("On", "uninstall");
		return removeElem;
	}
	public static Element componentRef(final Document doc, final String id) {
		Element componentRef = doc.createElement("ComponentRef");
		componentRef.setAttribute("Id", id);
		return componentRef;
	}
	public static Element component(final Document doc, String id) {
		final Element component = doc.createElement("Component");
		component.setAttribute("Id", id);
		component.setAttribute("Win64", yesno(win64Bit));
		return component;
	}

}