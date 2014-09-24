package org.korsakow.services.export;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.interf.IInterface;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISnu;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IText;
import org.korsakow.domain.interf.IVideo;

public class ExportData {
	public String dataPath;
	public IProject project;
	public Collection<ISnu> snusToExport;
	public Collection<IText> textsToExport;
	public Collection<IImage> imagesToExport;
	public Collection<ISound> soundsToExport;
	public Collection<IVideo> videosToExport;
	public Collection<IInterface> interfacesToExport;
	public File rootDir;
	public Map<String, String> filenamemap;
	
	public ExportData(String dataPath, IProject project,
		Collection<ISnu> snusToExport, Collection<IText> textsToExport, Collection<IImage> imagesToExport, Collection<ISound> soundsToExport, Collection<IVideo> videosToExport,
		Collection<IInterface> interfacesToExport, File rootDir, Map<String, String> filenamemap)
	{
	this.dataPath = dataPath;
	this.project = project;
	this.snusToExport = snusToExport;
	this.textsToExport = textsToExport;
	this.imagesToExport = imagesToExport;
	this.soundsToExport = soundsToExport;
	this.videosToExport = videosToExport;
	this.interfacesToExport = interfacesToExport;
	this.rootDir = rootDir;
	this.filenamemap = filenamemap;
	}
}

