package org.korsakow.services.export;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.task.ITask;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.task.DelegateTask;
import org.korsakow.ide.util.Util;
import org.korsakow.services.encoders.font.FontFormat;
import org.korsakow.services.encoders.sound.SoundFormat;
import org.korsakow.services.encoders.video.VideoCodec;
import org.korsakow.services.export.task.CopyFlashPlayerExportTask;
import org.korsakow.services.export.task.XMLExportTask;

public class FlashExporter extends AbstractExporter
{
	public static final String PLAYER_ROOT = "players/flash/";
	public static final String PLAYER_RESOURCE_CSS = "data/css/style.css";
	private static final String[] PLAYER_RESOURES = {
		"data/KorsakowPlayer.swf",
		"data/Embed.swf",
		"data/DebugWindow.swf",
		"data/swf/expressInstall.swf",
		"data/js/swfobject.js",
		"data/js/swfaddress.js",
		"data/js/jquery-2.1.0.min.js",
		"data/js/embed.js",
		"data/js/korsakow.js",
		"data/css/cross.png",
		"data/css/embed.css",
	};
	private static final SoundFormat SOUND_EXPORT_FORMAT = SoundFormat.MP3;
	private static final VideoCodec VIDEO_EXPORT_FORMAT = VideoCodec.FLV;
	private static final FontFormat FONT_EXPORT_FORMAT = FontFormat.SWF;
	
	public FlashExporter()
	{
	}

	@Override
	protected ITask createCopyTask(File rootDir, IProject project, String dataPath) {
		return new CopyFlashPlayerExportTask(rootDir, indexFilename, dataPath, project, getStaticResourceRoot(), getStaticResources());
	}
	
	@Override
	protected List<ITask> createDataExportTasks(ExportData data)
			throws IOException {
				List<ITask> tasks = new ArrayList<ITask>();
				XMLExportTask task = new XMLExportTask(data.dataPath, data.project, data.snusToExport, data.textsToExport, data.imagesToExport, data.soundsToExport, data.videosToExport, data.interfacesToExport, data.rootDir, data.filenamemap);
				tasks.add(task);
				return Util.list(ITask.class, new DelegateTask(LanguageBundle.getString("export.task.processingproject"), tasks));
			}
	
	@Override
	public String getStaticResourceRoot() { return PLAYER_ROOT; }
	@Override
	public Collection<String> getStaticResources() { return Arrays.asList(PLAYER_RESOURES); }
	@Override
	public SoundFormat getSoundFormat() { return SOUND_EXPORT_FORMAT; }
	@Override
	public VideoCodec getVideoFormat() { return VIDEO_EXPORT_FORMAT; }
}
