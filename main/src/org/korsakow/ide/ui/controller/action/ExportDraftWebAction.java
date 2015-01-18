package org.korsakow.ide.ui.controller.action;

import java.io.File;
import java.net.MalformedURLException;

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
import org.korsakow.ide.Application;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.ui.controller.action.helper.ProgressDialogStatusListener;
import org.korsakow.ide.util.ShellExec;
import org.korsakow.ide.util.ShellExec.ShellException;

public class ExportDraftWebAction extends AbstractExportWebAction {

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
		exportWorker.addPropertyChangeListener(IWorker.PROPERTY_STATE, new DraftExportDoneWorkerListener(progressDialog, mainFile));
		return exportWorker;
	}
	
	public static class DraftExportDoneWorkerListener extends ProgressDialogStatusListener
	{
		private final File exportFile;
		public DraftExportDoneWorkerListener(JDialog progressDialog, File exportFile)
		{
			super(progressDialog);
			this.exportFile = exportFile;
		}
		
		@Override
		protected void onDone()
		{
			try {
				ShellExec.openUrl(exportFile.toURL());
			} catch (ShellException e) {
				Application.getInstance().showUnhandledErrorDialog(LanguageBundle.getString("general.errors.uncaughtexception.title"), e);
			} catch (MalformedURLException e) {
				Application.getInstance().showUnhandledErrorDialog(LanguageBundle.getString("general.errors.uncaughtexception.title"), e);
			}
		}
	}
}
