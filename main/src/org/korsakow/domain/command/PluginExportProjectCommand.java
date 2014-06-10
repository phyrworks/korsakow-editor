package org.korsakow.domain.command;


import org.dsrg.soenea.domain.command.CommandException;
import org.korsakow.services.export.Exporter;
import org.korsakow.services.plugin.export.ExportPlugin;


public class PluginExportProjectCommand extends AbstractExportProjectCommand {

	public static final String PLUGIN = "plugin";
	
	private ExportPlugin plugin;
	
	public PluginExportProjectCommand(Helper request, Helper response) {
		super(request, response);
	}

	@Override
	protected Exporter createExporter() {
		return plugin.getExporter();
	}
	
	@Override
	public void execute() throws CommandException {
		plugin = (ExportPlugin)request.get(PLUGIN);
		
		super.execute();
	}
}
