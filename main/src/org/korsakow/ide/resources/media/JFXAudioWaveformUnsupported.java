package org.korsakow.ide.resources.media;

import java.awt.Component;

import javax.swing.JPanel;

import org.korsakow.ide.exception.MediaRuntimeException;

/**
 *
 * @author phoenix
 */
public class JFXAudioWaveformUnsupported extends JFXSound 
{
 
	public JFXAudioWaveformUnsupported(String url) throws MediaRuntimeException {
		super(url);
	}
	
	@Override
	public Component getComponent() {
		return new JPanel();
	}

}
