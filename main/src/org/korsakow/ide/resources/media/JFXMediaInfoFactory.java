/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.korsakow.ide.resources.media;

import java.awt.Component;
import java.io.File;
/**
 *
 * @author phoenix
 */
public class JFXMediaInfoFactory {
	public int width;
	public int height;
	public long duration;
	
	public static MediaInfo getInfo(File src)
	{
		JFXVideo video = null;
		try {
			MediaInfo info = new MediaInfo();
			
			video = new JFXVideo(src.getPath());
			Component comp = video.getComponent();
			info.width = comp.getWidth();
			info.height = comp.getHeight();
			info.duration = video.getDuration();
			return info;
		} finally {
			try { if (video != null) video.dispose(); } catch (Throwable t) {}
		}
	}
    
}
