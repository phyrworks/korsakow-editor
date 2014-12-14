package org.korsakow.ide.resources.media;

import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.korsakow.ide.exception.MediaRuntimeException;
import org.korsakow.ide.ui.resources.SingleWaveformPanel;

/**
 *
 * @author phoenix
 */
public class JFXSound {
   private Component waveform;
   private JFXVideo innerPlayer;
   
   public JFXSound(String url) throws MediaRuntimeException {
	try {
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream (new FileInputStream (url)));
		AudioInfo audioInfo = new AudioInfo(audioInputStream);
		waveform = new SingleWaveformPanel(audioInfo, 0);
		innerPlayer = new JFXVideo(url);
	} catch (Exception e) {
		throw new MediaRuntimeException(e);
	}  
   }

    public Component getComponent() {
		return waveform;
    }

    public Dimension getAspectRespectingDimension(Dimension outter) {
		return outter;
    }

    public void dispose() {
		innerPlayer.dispose();
		innerPlayer = null; // mostly to help catch usage errors, technically GC should work as well assuming 'this' is GC'd as expected
    }

    public long getDuration() {
		return innerPlayer.getDuration();
    }

    public boolean isPlaying() {
		return innerPlayer.isPlaying();
    }

    public void setTime(long time) {
		innerPlayer.setTime(time);
    }
    
    public long getTime() {
		return innerPlayer.getTime();
    }

    public void start() {
		innerPlayer.start();
    }

    public void stop() {
		innerPlayer.stop();
    }
    
    public void setVolume(float volume){
		innerPlayer.setVolume(volume);
    }
    
    public float getVolume() {
		return innerPlayer.getVolume();
    }
}
