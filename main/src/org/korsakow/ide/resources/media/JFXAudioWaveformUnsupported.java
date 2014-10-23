/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
	
	public Component getComponent() {
		return new JPanel();
	}

}
