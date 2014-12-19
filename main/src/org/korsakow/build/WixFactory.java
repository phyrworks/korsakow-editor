package org.korsakow.build;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class WixFactory {
	private final String YES = "yes";
	private final String NO = "no";
			
	private boolean is64Bit = true;
	public void set64Bit(boolean is64) {
		is64Bit = true;
	}
	
	private String yesno(boolean b) {
		return b ? YES : NO;
	}
	
	public Element file(Document doc, File file, String id) throws DOMException, IOException {
		Element fileElem = doc.createElement("File");
		fileElem.setAttribute("Id", id);
		//fileElem.setAttribute("Name", file.getName());
		fileElem.setAttribute("Source", file.getCanonicalPath());
		fileElem.setAttribute("KeyPath", "yes");
		return fileElem;
	}
	public Element directory(Document doc, String id, String name) {
		Element dirElem = doc.createElement("Directory");
		dirElem.setAttribute("Id", id);
		dirElem.setAttribute("Name", name);
		return dirElem;
	}
	public Element directoryRef(Document doc, String id) {
		Element dirElem = doc.createElement("DirectoryRef");
		dirElem.setAttribute("Id", id);
		return dirElem;
	}
	public Element featureRef(Document doc, String id) {
		final Element featureRef = doc.createElement("Feature");
		featureRef.setAttribute("Id", id);
		return featureRef;
	}
	public Element fragment(Document doc) {
		final Element fragment = doc.createElement("Fragment");
		return fragment;
	}
	public Element wix(Document doc) {
		Element wix = doc.createElementNS("http://schemas.microsoft.com/wix/2006/wi", "Wix");
		return wix;
	}
	public Element removeFile(Document doc, String id) {
		Element removeElem = doc.createElement("RemoveFile");
		removeElem.setAttribute("Id", id);
		removeElem.setAttribute("Name", "*");
		removeElem.setAttribute("On", "uninstall");
		return removeElem;
	}
	public Element componentRef(final Document doc, final String id) {
		Element componentRef = doc.createElement("ComponentRef");
		componentRef.setAttribute("Id", id);
		return componentRef;
	}
	public Element component(final Document doc, String id) {
		final Element component = doc.createElement("Component");
		component.setAttribute("Id", id);
		component.setAttribute("Win64", yesno(is64Bit));
		return component;
	}

}