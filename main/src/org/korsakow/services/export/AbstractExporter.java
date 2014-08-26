package org.korsakow.services.export;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.korsakow.domain.Image;
import org.korsakow.domain.Media;
import org.korsakow.domain.Settings;
import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.interf.IInterface;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISettings;
import org.korsakow.domain.interf.ISnu;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IText;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.interf.IWidget;
import org.korsakow.domain.task.ITask;
import org.korsakow.ide.Application;
import org.korsakow.ide.DataRegistry;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.resources.WidgetType;
import org.korsakow.ide.task.DelegateTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.ide.util.ResourceManager;
import org.korsakow.ide.util.Util;
import org.korsakow.services.encoders.image.ImageFormat;
import org.korsakow.services.encoders.sound.SoundFormat;
import org.korsakow.services.encoders.video.VideoCodec;
import org.korsakow.services.export.task.CreateFilenameMapTask;
import org.korsakow.services.export.task.ImageExportTask;
import org.korsakow.services.export.task.SoundExportTask;
import org.korsakow.services.export.task.SubtitleExportTask;
import org.korsakow.services.export.task.TextExportTask;
import org.korsakow.services.export.task.ThumbnailExportTask;
import org.korsakow.services.export.task.VideoExportTask;

public abstract class AbstractExporter implements Exporter {

	public static final String VIDEO_DIR = "video";
	public static final String TEXT_DIR = "text";
	public static final String IMAGE_DIR = "image";
	public static final String SOUND_DIR = "sound";
	public static final String FONT_DIR = "font";
	public static final String SUBTITLE_DIR = "subtitle";
	public static final String THUMBNAIL_DIR = "thumbnail";

	public static final String DATA_DIR = "data";
	public static final String RESOURCE_INDEX = "index.html";
	
	public AbstractExporter()
	{
		indexFilename = RESOURCE_INDEX;
	}
	
	protected final ExportOptions exportOptions = new ExportOptions();
	protected Map<String, String> filenamemap;
	protected File rootDir;
	protected ISettings settings;
	protected IProject projectToExport;
	protected String indexFilename;
	protected Collection<ISnu> snusToExport = new HashSet<ISnu>();
	protected Collection<ISound> soundsToExport = new HashSet<ISound>();
	protected Collection<IImage> imagesToExport = new HashSet<IImage>();
	protected Collection<IVideo> videosToExport = new HashSet<IVideo>();
	protected Collection<IInterface> interfacesToExport = new HashSet<IInterface>();
	protected Collection<IText> textsToExport = new HashSet<IText>();

	public abstract String getStaticResourceRoot();
	public abstract Collection<String> getStaticResources();
	public abstract SoundFormat getSoundFormat();
	public abstract VideoCodec getVideoFormat();
	
	/**
	 * Calculates the largest dimension a video would play at. Currently this means the
	 * largest size of any video playing widget.
	 * 
	 * @param interfacesToExport
	 * @return
	 */
	protected static Dimension calculateMaxVideoSize(IProject projectToExport, Collection<IInterface> interfacesToExport) {
		Dimension d = new Dimension(0, 0);
		for (IInterface interf : interfacesToExport)
		{
			Collection<IWidget> widgets = interf.getWidgets();
			for (IWidget widget : widgets)
			{
				if (widget.getWidgetId().equals(WidgetType.MainMedia.getId()) ||
					widget.getWidgetId().equals(WidgetType.MediaArea.getId()) ||
					widget.getWidgetId().equals(WidgetType.SnuAutoLink.getId()) ||
					widget.getWidgetId().equals(WidgetType.SnuFixedLink.getId()))
				{
					if (widget.getWidth() > d.width)
						d.width = widget.getWidth();
					if (widget.getHeight() > d.height)
						d.height = widget.getHeight();
				}
			}
		}
		if (d.width == 0)
			d.width = projectToExport.getMovieWidth();
		if (d.height == 0)
			d.height = projectToExport.getMovieHeight();
		return d;
	}

	/**
	 * CAVEAT: must synchronize on the returned instance
	 * @return
	 */
	public ExportOptions getExportOptions() {
		return exportOptions;
	}

	@Override
	public void setFilenameMap(Map<String, String> map) {
		filenamemap = map;
	}

	protected String getFilename(String key) throws ExportException {
		if (!filenamemap.containsKey(key))
			throw new ExportException("unexpected error: filename '" + key + "' not in map", rootDir);
		return filenamemap.get(key);
	}

	@Override
	public void setIndexFilename(String indexFilename) {
		this.indexFilename = indexFilename;
	}

	@Override
	public void setProject(IProject proj) {
		projectToExport = proj;
	}

	@Override
	public void setSettings(ISettings settings) {
		this.settings = settings;
	}

	@Override
	public void setSnus(Collection<ISnu> snus) {
		snusToExport = new HashSet<ISnu>(snus);
	}

	@Override
	public void setSounds(Collection<ISound> sounds) {
		soundsToExport = new HashSet<ISound>(sounds);
	}

	@Override
	public void setImages(Collection<IImage> images) {
		imagesToExport = new HashSet<IImage>(images);
	}

	@Override
	public void setVideos(Collection<IVideo> videos) {
		videosToExport = new HashSet<IVideo>(videos);
	}

	@Override
	public void setInterfaces(Collection<IInterface> interfaces) {
		interfacesToExport = new HashSet<IInterface>(interfaces);
	}

	@Override
	public void setTexts(Collection<IText> texts) {
		textsToExport = new HashSet<IText>(texts);
	}
	
	@Override
	public void setOverwriteExistingFiles(boolean overwrite) {
		synchronized (exportOptions) {
			exportOptions.overwriteExisting = overwrite;
		}
	}
	
	@Override
	public void setVideoEncodingEnabled(boolean enabled) {
		synchronized (exportOptions) {
			exportOptions.encodeVideo = enabled;
		}
	}
	
	public IVideoEncodingProfile getVideoEncodingProfile() throws IOException {
		try {
			return new PropertiesVideoEncodingProfile(ResourceManager.getResourceStream("encodingprofiles/" + settings.getString("videoEncodingProfile") + ".properties"));
		} catch (IOException e) {
			Application.getInstance().showAlertDialog(String.format("Encoding profile not found: %s", settings.getString("videoEncodingProfile")), "Please check your project's export settings.");
			return new PropertiesVideoEncodingProfile(ResourceManager.getResourceStream("encodingprofiles/" + "flv_low" + ".properties"));
		}
	}

	@Override
	public List<ITask> createExportTasks(File rootDir) throws IOException,
			ExportException, InterruptedException {
				//List<IVideo> videos = Command.listVideo();
				IProject project = projectToExport;
				
				FileUtil.mkdirs(rootDir);
				
				File dataDir = new File(rootDir, DATA_DIR);
				FileUtil.mkdirs(dataDir);
				
				String dataPath = "project.xml";
				
				ITask createFilenameMapTask = new CreateFilenameMapTask(this, IMAGE_DIR, imagesToExport, VIDEO_DIR, videosToExport, SOUND_DIR, soundsToExport, TEXT_DIR, textsToExport, FONT_DIR);
				try {
					createFilenameMapTask.run();
				} catch (TaskException e) {
					ExportException ee;
					if (e.getCause() instanceof ExportException)
						throw ee = (ExportException)e.getCause();
					else
						ee = new ExportException(e.getCause(), rootDir);
					throw ee;
				}		
				
				List<ITask> exportTasks = new ArrayList<ITask>();
				exportTasks.addAll(createTextExportTasks(exportOptions, dataDir, textsToExport));
				if (settings.getBoolean(Settings.ExportImages))
					exportTasks.addAll(createImageExportTasks(exportOptions, dataDir, imagesToExport));
				if (settings.getBoolean(Settings.ExportSounds))
					exportTasks.addAll(createSoundExportTasks(exportOptions, dataDir, soundsToExport));
			
				Dimension maxVideoSize = calculateMaxVideoSize(project, interfacesToExport);
				Logger.getLogger(FlashExporter.class).info(String.format("Video Encoding Profile: %s", getVideoEncodingProfile().getName()));
				if (settings.getBoolean(Settings.ExportVideos))
					exportTasks.addAll(createVideoExportTasks(exportOptions, dataDir, getVideoEncodingProfile(), maxVideoSize, videosToExport));
				if (settings.getBoolean(Settings.ExportSubtitles))
					exportTasks.addAll(createSubtitleExportTasks(exportOptions, dataDir, videosToExport));
				
				// for hackish reasons we check the ExportImages settings inside the createThumbnail task
				exportTasks.addAll(createThumbnailExportTasks(exportOptions, rootDir, maxVideoSize, snusToExport));
				
				if (settings.getBoolean(Settings.ExportWebFiles))
					exportTasks.add(createCopyTask(rootDir, project, dataPath));
			
				exportTasks.addAll(createDataExportTasks(dataPath, project, snusToExport, textsToExport, imagesToExport, soundsToExport, videosToExport, interfacesToExport, dataDir, filenamemap));
				
				return exportTasks;
			}
	
	public Map<String, String> getFilenameMap() {
		return filenamemap;
	}

	protected abstract ITask createCopyTask(File rootDir, IProject project, String dataPath);

	protected static Collection<Font> calculateFontsToExport(boolean exportFonts, Collection<IText> textsToExport,
			Collection<IInterface> interfacesToExport) throws Exception {
				Collection<Font> fontsToExport = new HashSet<Font>();
				if (!exportFonts)
					return fontsToExport;
				
				for (IText text : textsToExport)
					fontsToExport.addAll(text.getFonts());
				for (IInterface interf : interfacesToExport) {
					for (IWidget widget : interf.getWidgets()) {
						if (widget.getDynamicProperty("fontFamily") != null) {
							Font font = new Font((String)widget.getDynamicProperty("fontFamily"), Font.PLAIN, widget.getDynamicProperty("fontSize")!=null?(Integer)widget.getDynamicProperty("fontSize"):10);
							fontsToExport.add(font);
						}
					}
				}
				return fontsToExport;
			}

	protected abstract List<ITask> createDataExportTasks(String dataPath, IProject project,
			Collection<ISnu> snusToExport, Collection<IText> textsToExport, Collection<IImage> imagesToExport, Collection<ISound> soundsToExport, Collection<IVideo> videosToExport,
			Collection<IInterface> interfacesToExport, File rootDir, Map<String, String> filenamemap)
			throws IOException;

	protected List<ITask> createVideoExportTasks(ExportOptions options, File rootDir,
			IVideoEncodingProfile encodingProfile, Dimension maxVideoSize, Collection<IVideo> videosToExport) throws IOException, ExportException {
				Set<String> alreadyTasked = new HashSet<String>();
				List<ITask> tasks = new ArrayList<ITask>();
				for (IVideo video : videosToExport)
				{
					String dest = getFilename(video.getAbsoluteFilename());
					if (!alreadyTasked.add(dest))
						continue;
					File destFile = new File(rootDir.getAbsolutePath() + File.separatorChar + dest);
					destFile.getParentFile().mkdirs();
					VideoExportTask task = new VideoExportTask(options, encodingProfile, video, destFile, rootDir);
					if (maxVideoSize != null)
						task.setMaxSize(maxVideoSize.width, maxVideoSize.height);
					tasks.add(task);
				}
				return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.encodingvideo"), tasks));
			}

	protected List<ITask> createSubtitleExportTasks(ExportOptions options, File rootDir,
			Collection<IVideo> videosToExport) throws IOException, ExportException {
				Set<String> alreadyTasked = new HashSet<String>();
				List<ITask> tasks = new ArrayList<ITask>();
				for (IVideo video : videosToExport)
				{
					if (video.getSubtitles() == null)
						continue;
					String subtitles = Media.getAbsoluteFilename(video.getSubtitles());
					if (!alreadyTasked.add(subtitles))
						continue;
					File destFile = new File(rootDir.getAbsolutePath() + File.separatorChar + getFilename(subtitles));
					destFile.getParentFile().mkdirs();
					SubtitleExportTask task = new SubtitleExportTask(options, destFile, new File(subtitles));
					tasks.add(task);
				}
				return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.encodingvideo"), tasks));
			}

	protected List<ITask> createThumbnailExportTasks(ExportOptions options, File rootDir,
			Dimension maxVideoSize, Collection<ISnu> snusToExport) throws IOException, ExportException {
			    List<ITask> tasks = new ArrayList<ITask>();
			    for (ISnu snu : snusToExport)
			    {
			            final IMedia mainMedia = snu.getMainMedia();
			            String mainMediaFile = getFilename(mainMedia.getAbsoluteFilename());
			            String base = FileUtil.getFilenameWithoutExtension(new File(mainMediaFile).getName());
			            String ext = ImageFormat.JPG.getFileExtension();
			            String dest = base + "." + ext;
			            File destFile = new File(rootDir, FlashExporter.DATA_DIR + File.separatorChar + FlashExporter.THUMBNAIL_DIR + File.separatorChar + dest);
			            destFile.getParentFile().mkdirs();
			            ThumbnailExportTask task = new ThumbnailExportTask(options, mainMedia, destFile, rootDir);
			            if (maxVideoSize != null)
			                    task.setMaxSize(maxVideoSize.width, maxVideoSize.height);
			            
			            // we do all the other work and check this setting here because of how hackish thumbs are
			            // and we still want them to appear in the xml!
			    		if (settings.getBoolean(Settings.ExportImages))
			    			tasks.add(task);
			            
			            // hackish: temporarily setting the thumbnail this way
			            if (filenamemap.containsKey(destFile.getAbsoluteFile()))
			                    throw new ExportException(
			                                    "Internal Error: filename already in use "
			                                                    + destFile.getAbsolutePath(), rootDir);
			            filenamemap.put(destFile.getAbsolutePath(), new File(FlashExporter.THUMBNAIL_DIR + File.separatorChar + dest).getPath());
			            IImage thumbnail = new Image(DataRegistry.getMaxId(), 0);
			            thumbnail.setFilename(destFile.getPath());
			            thumbnail.setName(destFile.getName());
			            snu.setThumbnail(thumbnail);
			    }
			    return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.encodingthumbnail"), tasks));
			}

	protected List<ITask> createTextExportTasks(ExportOptions options, File rootDir, Collection<IText> textsToExport)
			throws IOException, ExportException {
				List<ITask> tasks = new ArrayList<ITask>();
				for (IText text : textsToExport)
				{
					String dest = getFilename(text.getAbsoluteFilename());
					File destFile = new File(rootDir.getAbsolutePath() + File.separatorChar + dest);
					destFile.getParentFile().mkdirs();
					tasks.add(new TextExportTask(options, text, destFile));
				}
				return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.encodingtext"), tasks));
			}

	protected List<ITask> createImageExportTasks(ExportOptions options, File rootDir, Collection<IImage> imagesToExport)
			throws IOException, ExportException {
				List<ITask> tasks = new ArrayList<ITask>();
				for (IImage image : imagesToExport)
				{
					String dest = getFilename(image.getAbsoluteFilename());
					File destFile = new File(rootDir.getAbsolutePath() + File.separatorChar + dest);
					destFile.getParentFile().mkdirs();
					tasks.add(new ImageExportTask(options, image, destFile));
				}
				return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.encodingimage"), tasks));
			}

	protected List<ITask> createSoundExportTasks(ExportOptions options, File rootDir, Collection<ISound> soundsToExport)
			throws IOException, ExportException {
				List<ITask> tasks = new ArrayList<ITask>();
				for (ISound sound : soundsToExport)
				{
					String dest = getFilename(sound.getAbsoluteFilename());
					File destFile = new File(rootDir.getAbsolutePath() + File.separatorChar + dest);
					destFile.getParentFile().mkdirs();
					File subtitleFile = null;
					if (sound.getSubtitles() != null)
						subtitleFile = new File(rootDir.getAbsolutePath() + File.separatorChar + getFilename(Media.getAbsoluteFilename(sound.getSubtitles())));
					tasks.add(new SoundExportTask(options, getSoundFormat(), sound, destFile, subtitleFile));
				}
				return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.encodingsound"), tasks));
			}

}