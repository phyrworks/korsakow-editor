package org.korsakow.services.export;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.interf.IInterface;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISettings;
import org.korsakow.domain.interf.ISnu;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IText;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.task.ITask;
import org.korsakow.services.encoders.sound.SoundFormat;
import org.korsakow.services.encoders.video.VideoCodec;

public interface Exporter {

	public abstract void setOverwriteExistingFiles(boolean overwrite);
	public abstract void setVideoEncodingEnabled(boolean enabled);
	
	public abstract void setFilenameMap(Map<String, String> map);

	public abstract void setIndexFilename(String indexFilename);

	public abstract void setProject(IProject proj);

	public abstract void setSettings(ISettings settings);

	public abstract void setSnus(Collection<ISnu> snus);

	public abstract void setSounds(Collection<ISound> sounds);

	public abstract void setImages(Collection<IImage> images);

	public abstract void setVideos(Collection<IVideo> videos);

	public abstract void setInterfaces(Collection<IInterface> interfaces);

	public abstract void setTexts(Collection<IText> texts);

	public abstract List<ITask> createExportTasks(File rootDir)
			throws IOException, ExportException, InterruptedException;

	public abstract SoundFormat getSoundFormat();
	public abstract VideoCodec getVideoFormat();
}