/**
 * 
 */
package org.korsakow.services.export.task;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.ide.Application;
import org.korsakow.ide.DialogOptions;
import org.korsakow.ide.resources.media.MediaFactory;
import org.korsakow.ide.resources.media.PlayableVideo;
import org.korsakow.ide.task.AbstractTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.services.export.ExportException;
import org.korsakow.services.export.ExportOptions;

public class VideoExportTask extends AbstractTask
{
	private final IVideo video;
	private final File srcFile;
	private final File destFile;
	private final File rootDir;
	private Integer maxWidth = null;
	private Integer maxHeight = null;
	public VideoExportTask(ExportOptions options, IVideo video, File destFile, File rootDir) throws FileNotFoundException
	{
		super(options);
		this.video = video;
		this.destFile = destFile;
		this.rootDir = rootDir;
		srcFile = new File(video.getAbsoluteFilename());
	}
	public void setMaxSize(int width, int height)
	{
		maxWidth = width;
		maxHeight = height;
	}
	@Override
	public String getTitleString()
	{
		return srcFile.getName();
	}
	@Override
	public void runTask() throws TaskException, InterruptedException
	{
		// TODO: I think this is outdated 2013/01/20 // the length check is because in creating the unique export filename we actually reserve the physical file
		if (destFile.exists() && destFile.length() > 0) {
			Boolean overwriteOption;
			synchronized (exportOptions) {
				overwriteOption = exportOptions.overwriteExisting;
			}
			// if option already set to false, then abort
			if (overwriteOption == Boolean.FALSE)
				return;
			
			// if undecided, ask
			if (overwriteOption == null) {
				DialogOptions dialogOptions =  Application.getInstance().showFileOverwriteDialog("File exists", destFile.getName() + " already exists, YES to overwrite or NO to skip.");
				if (dialogOptions.applyToAll) {
					// apply to all means set the global option
					synchronized (exportOptions) {
						exportOptions.overwriteExisting = dialogOptions.dialogResult;
					}
				}
				if (!dialogOptions.dialogResult)
					return;
			}
		}
				
		if (!srcFile.exists())
			throw new TaskException(new FileNotFoundException(srcFile.getPath()));
		try {
			copyVideo(srcFile, destFile);
		} catch (IOException e) {
			throw new TaskException(e);
		}
		if (!destFile.exists() || destFile.length() == 0)
			throw new TaskException(new ExportException("Video File Missing:" + destFile.getPath(), rootDir));
	}
	private static void copyVideo(File srcFile, File destFile) throws IOException {
		FileUtil.copyFile(srcFile, destFile);
	}
	
	/**
	 * @return null if the dimensions can't be calculated or would result in a size larger than the media's actual size
	 */
	public static Dimension calculateVideoSize(File srcFile,
			int maxWidth,
			int maxHeight)
	{
		Dimension d = null;
		PlayableVideo playable = null;
		try {
			playable = (PlayableVideo)MediaFactory.getMedia(srcFile.getAbsolutePath());
			Component comp = playable.getComponent();
			Dimension pref = comp.getPreferredSize();
			// don't enlarge.
			if (maxWidth < pref.width && maxHeight < pref.height)
				d = playable.getAspectRespectingDimension(new Dimension(maxWidth, maxHeight));
		} catch (Exception e) {
			Logger.getLogger(VideoExportTask.class).error("", e);
		} finally {
            if (playable != null) {
                try { playable.dispose(); } catch (Exception e) {
                    Logger.getLogger(VideoExportTask.class).error("", e);
                }
            }
		    
		}
		return d;
	}
}




