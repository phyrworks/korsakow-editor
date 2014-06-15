package org.korsakow.ide.ui.controller.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JDialog;

import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.korsakow.domain.CommandExecutor;
import org.korsakow.domain.command.AbstractExportProjectCommand;
import org.korsakow.domain.command.Helper;
import org.korsakow.domain.command.PluginExportProjectCommand;
import org.korsakow.domain.command.Request;
import org.korsakow.domain.command.Response;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.domain.task.IWorker;
import org.korsakow.services.plugin.export.ExportPlugin;

public class PluginExportAction extends AbstractExportWebAction implements ActionListener {

	private final ExportPlugin plugin;
	public PluginExportAction(ExportPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.info(String.format("Exporting via Plugin: " + plugin.getName()));
		super.actionPerformed(event);
	}
	@Override protected IWorker createExportWorker(File mainFile,
			JDialog progressDialog,
			boolean forceSkipOverwrite, boolean encodeVideo, File parentFile)
			throws MapperException, CommandException {
		IProject project = ProjectInputMapper.find();
		Helper request = new Request();
		Helper response = new Response();
		request.set(AbstractExportProjectCommand.PROJECT_ID, project.getId());
		request.set(AbstractExportProjectCommand.EXPORT_DIR, parentFile.getPath());
		request.set(AbstractExportProjectCommand.INDEX_FILENAME, mainFile.getName());
		if (forceSkipOverwrite)
			request.set(AbstractExportProjectCommand.OVERWRITE_EXISTING, forceSkipOverwrite);
		request.set(AbstractExportProjectCommand.VIDEO_ENCODING_ENABLED, encodeVideo);
		request.set(PluginExportProjectCommand.PLUGIN, plugin);
		CommandExecutor.executeCommand(PluginExportProjectCommand.class, request, response);
		IWorker exportWorker = (IWorker)response.get(AbstractExportProjectCommand.WORKER);
		return exportWorker;
	}
}
