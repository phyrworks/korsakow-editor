package org.korsakow.domain.command;

import org.korsakow.services.export.Exporter;
import org.korsakow.services.export.FlashExporter;


public class ExportFlashProjectCommand extends AbstractExportProjectCommand {

	public ExportFlashProjectCommand(Helper request, Helper response) {
		super(request, response);
	}

	@Override
	protected Exporter createExporter() {
		return new FlashExporter();
	}
}
