/**
 * 
 */
package org.korsakow.services.export.task;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.antlr.stringtemplate.StringTemplate;
import org.korsakow.domain.interf.IProject;
import org.korsakow.ide.Build;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.task.AbstractTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.ide.util.ResourceManager;
import org.korsakow.services.export.AbstractExporter;
import org.korsakow.services.export.FlashExporter;
import org.korsakow.services.util.ColorFactory;

public class CopyFlashPlayerExportTask extends AbstractTask
{
	private final File rootDir;
	private final String indexFilename;
	private final String dataPath;
	private final IProject project;
	private final String resourceRoot;
	private final Collection<String> staticResources;
	public CopyFlashPlayerExportTask(File rootDir, String indexFilename, String dataPath, IProject project, String resourceRoot, Collection<String> staticResources)
	{
		this.rootDir = rootDir;
		this.indexFilename = indexFilename;
		this.project = project;
		this.dataPath = dataPath;
		this.resourceRoot = resourceRoot;
		this.staticResources = staticResources;
	}
	@Override
	public String getTitleString()
	{
		return LanguageBundle.getString("export.task.copyplayer");
	}
	@Override
	public void runTask() throws TaskException
	{
		try {
			for (String resource : staticResources) {
				FileUtil.copyFile(ResourceManager.getResourceFile(resourceRoot + resource), new File(rootDir, resource));
			}
			
			FileUtil.writeFileFromString(new File(rootDir, FlashExporter.PLAYER_RESOURCE_CSS), createCSS());
			FileUtil.writeFileFromString(new File(rootDir, indexFilename), createIndex());
		} catch (IOException e) {
			throw new TaskException(e);
		}
	}
	private String createIndex() throws IOException
	{
		InputStream inputStream = ResourceManager.getResourceStream(FlashExporter.PLAYER_ROOT + AbstractExporter.RESOURCE_INDEX);
		String template = FileUtil.readString(inputStream);
		StringTemplate st = new StringTemplate(template);
		st.setAttribute("title", project.getName());
		Color backgroundColor = project.getBackgroundColor()!=null?project.getBackgroundColor():Color.black;
		st.setAttribute("backgroundColor", ColorFactory.formatCSS(backgroundColor));
		st.setAttribute("dataPath", dataPath);
		st.setAttribute("width", project.getMovieWidth());
		st.setAttribute("height", project.getMovieHeight());
		st.setAttribute("requireVersion", Build.getRelease());
		return st.toString();
	}
	private String createCSS() throws IOException
	{
		InputStream inputStream = ResourceManager.getResourceStream(FlashExporter.PLAYER_ROOT + FlashExporter.PLAYER_RESOURCE_CSS);
		String template = FileUtil.readString(inputStream);
		StringTemplate st = new StringTemplate(template);
		Color backgroundColor = project.getBackgroundColor()!=null?project.getBackgroundColor():Color.black;
		st.setAttribute("backgroundColor", ColorFactory.formatCSS(backgroundColor));
		return st.toString();
	}
}
