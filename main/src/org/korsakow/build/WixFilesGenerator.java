package org.korsakow.build;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.korsakow.ide.util.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Generates a Wix Fragment from a filesystem tree.
 * A component is created on a per-directory basis.
 */
public class WixFilesGenerator {
	
	private final Document doc;
	
	public WixFilesGenerator() throws ParserConfigurationException, SAXException {
		doc = DomUtil.createDocument();
	}
	public Document generateFilesFragment(String sourcePath, String featureRefId, String directoryRefId) throws IOException {
		
		Element wix = WixFactory.wix(doc);
		doc.appendChild(wix);
		
		Element fragment = WixFactory.fragment(doc);
		wix.appendChild(fragment);
		
		final Element featureRef = WixFactory.featureRef(doc, featureRefId);
		fragment.appendChild(featureRef);
		
		final Element rootDirectoryRef = WixFactory.directoryRef(doc, directoryRefId);
		fragment.appendChild(rootDirectoryRef);
		
		generateFilesFragment(rootDirectoryRef, rootDirectoryRef, featureRef, new File(sourcePath));
		
		return doc;
	}
	public void generateFilesFragment(final Element rootDirectoryRef, final Element rootComponent, final Element featureRef, final File sourceDir) throws IOException {

		WixUtil.visitRecursively(sourceDir, new WixUtil.FileVisitor() {
			Stack<Element> stack = new Stack<Element>();
			public void enter(File dir) throws IOException {
				if (dir.equals(sourceDir))
				return;
			
				String path = WixUtil.relative(sourceDir, dir);
	
				Element dirElem = WixFactory.directory(doc, WixUtil.sanitizeId(path), dir.getName());
				
				if (stack.isEmpty())
					rootDirectoryRef.appendChild(dirElem);
				else {
					stack.get(stack.size()-1).appendChild(dirElem);
				}
				
				final String id = WixUtil.sanitizeId(path);
				
				stack.push(dirElem);

			}
			public void exit(File dir) throws IOException {
				if (!stack.isEmpty()) {
					stack.pop();
				}
			}
			public void file(File file) throws IOException {
				String path = WixUtil.relative(sourceDir, file);
				String id = WixUtil.sanitizeId(path);
				
				Element fileElem = WixFactory.file(doc, file, id);

				Element child;
				Element component = WixFactory.component(doc, id);
				component.appendChild(fileElem);
				child = component;
				featureRef.appendChild(WixFactory.componentRef(doc, id));

				if (stack.isEmpty())
					rootComponent.appendChild(child);
				else
					stack.peek().appendChild(child);
			}
		});
	}
}
