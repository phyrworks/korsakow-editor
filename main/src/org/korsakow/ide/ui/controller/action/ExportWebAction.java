package org.korsakow.ide.ui.controller.action;

import java.io.File;

import javax.swing.JDialog;

import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.korsakow.domain.CommandExecutor;
import org.korsakow.domain.command.AbstractExportProjectCommand;
import org.korsakow.domain.command.ExportFlashProjectCommand;
import org.korsakow.domain.command.Helper;
import org.korsakow.domain.command.Request;
import org.korsakow.domain.command.Response;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.domain.task.IWorker;


public class ExportWebAction extends AbstractExportWebAction {
	
	@Override protected IWorker createExportWorker(File mainFile,
			JDialog progressDialog,
			boolean forceSkipOverwrite, File parentFile)
			throws MapperException, CommandException {
		IProject project = ProjectInputMapper.find();
		Helper request = new Request();
		Helper response = new Response();
		request.set(AbstractExportProjectCommand.PROJECT_ID, project.getId());
		request.set(AbstractExportProjectCommand.EXPORT_DIR, parentFile.getPath());
		request.set(AbstractExportProjectCommand.INDEX_FILENAME, mainFile.getName());
		if (forceSkipOverwrite)
			request.set(AbstractExportProjectCommand.OVERWRITE_EXISTING, forceSkipOverwrite);
		CommandExecutor.executeCommand(ExportFlashProjectCommand.class, request, response);
		IWorker exportWorker = (IWorker)response.get(AbstractExportProjectCommand.WORKER);
		return exportWorker;
	}

}
