package org.korsakow.ide.controller;

import java.awt.event.WindowEvent;

import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.IResource;

public class ApplicationAdapter implements ApplicationListener
{
	@Override
	public void onProjectLoaded(IProject project) {
	}
	@Override
	public void onResourceAdded(IResource resource) {
	}
	@Override
	public void onResourceDeleted(IResource resource) {
	}
	@Override
	public void onResourceModified(IResource resource) {
	}
	@Override
	public void onResourcesCleared() {
		
	}
	@Override
	public void onKeywordsChanged() {
	}
	@Override
	public void onWindowActivated(WindowEvent event) {
	}
	@Override
	public void onWindowClosed(WindowEvent event) {
	}
	@Override
	public boolean onApplicationWillShutdown() {
		return true;
	}
	@Override
	public void onApplicationShutdown() {
		
	}
}
