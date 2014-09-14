package org.korsakow.domain.command;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.dsrg.soenea.uow.UoW;
import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISettings;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IText;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.mapper.input.MediaInputMapper;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.domain.mapper.input.SettingsInputMapper;
import org.korsakow.domain.task.ITask;
import org.korsakow.domain.task.IWorker;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.resources.ResourceType;
import org.korsakow.ide.task.AbstractTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.task.UIWorker;
import org.korsakow.services.export.ExportException;
import org.korsakow.services.export.Exporter;

public abstract class AbstractExportProjectCommand extends AbstractCommand {

	public static final String EXPORTER = "exporter";
	public static final String WORKER = "worker";
	public static final String PROJECT_ID = "project_id";
	public static final String EXPORT_DIR = "export_dir";
	public static final String INDEX_FILENAME = "index_filename";
	public static final String OVERWRITE_EXISTING = "overwrite_existing";
	public static final String VIDEO_ENCODING_ENABLED = "video_encoding_enabled";

	public static class SetupExporterTask extends AbstractTask
	{
		private final IWorker uiWorker;
		private final String indexFilename;
		private final File exportDir;
		private final Exporter exporter;
		private final IProject project;
		public SetupExporterTask(IWorker uiWorker, Exporter exporter, File exportDir, String indexFilename, IProject project)
		{
			this.uiWorker = uiWorker;
			this.exporter = exporter;
			this.exportDir = exportDir;
			this.indexFilename = indexFilename;
			this.project = project;
		}
	
		@Override
		public void runTask() throws TaskException, InterruptedException {
			UoW.newCurrent();
			
			ISettings settings;
	
			Collection<IMedia> media;
			try {
				settings = SettingsInputMapper.find();
				media = MediaInputMapper.findReferencedMedia();
			} catch (XPathExpressionException e) {
				throw new TaskException(e);
			} catch (MapperException e) {
				throw new TaskException(e);
			} catch (SQLException e) {
				throw new TaskException(e);
			}
			Collection<ISound> sounds = new HashSet<ISound>();
			Collection<IVideo> videos = new HashSet<IVideo>();
			Collection<IImage> images = new HashSet<IImage>();
			Collection<IText> texts = new HashSet<IText>();
			int i = 0;
			int size = media.size();
			long before = System.currentTimeMillis();
			for (IMedia medium : media)
			{
				fireProgressChanged(0, ++i*100/size);
				fireDisplayStringChanged("", medium.getName());
				
				ResourceType resourceType = ResourceType.forId(medium.getType());
				switch (resourceType)
				{
				case SOUND:
					sounds.add((ISound)medium);
					break;
				case VIDEO:
					videos.add((IVideo)medium);
					break;
				case IMAGE:
					images.add((IImage)medium);
					break;
				case TEXT:
					texts.add((IText)medium);
					break;
				default:
					Logger.getLogger(ExportFlashProjectCommand.class).error("media list contains unknown media: " + medium.getClass().getCanonicalName(), new Exception("just-for-stacktrace"));
					break;
				}
			}
			long after = System.currentTimeMillis();
			System.out.println("Determination: " + (after-before)/1000 + "\t" + media.size());
			
			exporter.setIndexFilename(indexFilename);
			exporter.setProject(project);
			exporter.setSettings(settings);
			exporter.setSnus(project.getSnus());
			exporter.setSounds(sounds);
			exporter.setImages(images);
			exporter.setVideos(videos);
			exporter.setInterfaces(project.getInterfaces());
			exporter.setTexts(texts);
	
			fireDisplayStringChanged("", LanguageBundle.getString("export.task.initializingtasks"));
			List<ITask> exportTasks;
			try {
				exportTasks = exporter.createExportTasks(exportDir);
			} catch (IOException e) {
				throw new TaskException(e);
			} catch (ExportException e) {
				throw new TaskException(e);
			}
			uiWorker.addTasks(exportTasks);
		}

		@Override
		public String getTitleString()
		{
			return LanguageBundle.getString("export.task.determiningmediatoexport");
		}
	}

	public AbstractExportProjectCommand(Helper request, Helper response) {
		super(request, response);
	}

	public void execute() throws CommandException {
	
		try {
		    
			long projectId = request.getLong(PROJECT_ID);
			IProject project = ProjectInputMapper.map(projectId);
			
			File exportDir = new File(request.getString(EXPORT_DIR));
			if (exportDir.isFile() && exportDir.getParentFile() != null)
				exportDir = exportDir.getParentFile();
			
			String indexFilename = request.getString(INDEX_FILENAME);
			
			Exporter exporter = createExporter();
			if (request.has(OVERWRITE_EXISTING))
				exporter.setOverwriteExistingFiles(request.getBoolean(OVERWRITE_EXISTING));
			exporter.setVideoEncodingEnabled(request.getBoolean(VIDEO_ENCODING_ENABLED));
			
			IWorker exportWorker = new UIWorker();
			
			// we want to do the setup in a task since its lengthy
			// however we can't generate the other tasks from the exporter until that's done
			// so the setup tasks does that and then adds them to the worker
			exportWorker.addTask(new SetupExporterTask(exportWorker, exporter, exportDir, indexFilename, project));
	
			
			response.set(WORKER, exportWorker);
			response.set(EXPORTER, exporter);
			
		} catch (Exception e) {
			throw new CommandException(e);
		}
	
	}

	protected abstract Exporter createExporter();

}