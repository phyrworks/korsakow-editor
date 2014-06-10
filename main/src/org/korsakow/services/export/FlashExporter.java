package org.korsakow.services.export;


import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.task.ITask;
import org.korsakow.services.encoders.font.FontFormat;
import org.korsakow.services.encoders.sound.SoundFormat;
import org.korsakow.services.encoders.video.VideoCodec;
import org.korsakow.services.export.task.CopyFlashPlayerExportTask;

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
	public String getStaticResourceRoot() { return PLAYER_ROOT; }
	@Override
	public Collection<String> getStaticResources() { return Arrays.asList(PLAYER_RESOURES); }
	@Override
	public SoundFormat getSoundFormat() { return SOUND_EXPORT_FORMAT; }
	@Override
	public VideoCodec getVideoFormat() { return VIDEO_EXPORT_FORMAT; }
}
