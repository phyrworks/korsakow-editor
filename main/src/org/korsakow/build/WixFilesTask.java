package org.korsakow.build;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;

/**
 * Ant task that generates a WIX fragment listing files to install.
 */
public class WixFilesTask extends Task {
	private String directoryRef;
	private String featureRef;
	private String sourcePath;
	private String outputFile;
	private boolean is64Bit = true;
	
	/**
	 * The feature (ref) which the files will be a part of.
	 */
	public void setFeatureRef(String featureRef) {
		this.featureRef = featureRef;
	}
	/**
	 * The directory (Ref) which the files will be a sub-tree of.
	 */
	public void setDirectoryRef(String directoryRef) {
		this.directoryRef = directoryRef;
	}
	/**
	 * The directory which will be recurisvely scanned for files.
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}
	/**
	 * The .wxs file to generate.
	 */
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}
	
	public void setIs64Bit(boolean is64Bit) {
		this.is64Bit = is64Bit;
	}
	
	@Override
	public void execute() throws BuildException {
		try {
			log(String.format("Creating Wix file list from '%s' for feature '%s' in '%s'", sourcePath, featureRef, outputFile));
			WixFilesGenerator wix = new WixFilesGenerator(is64Bit);
			Document doc = wix.generateFilesFragment(sourcePath, featureRef, directoryRef);

			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer transformer = tranFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}
}