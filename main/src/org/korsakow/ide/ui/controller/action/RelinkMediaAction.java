/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.korsakow.ide.ui.controller.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.korsakow.domain.CommandExecutor;
import org.korsakow.domain.command.FixInvalidMediaCommand;
import org.korsakow.domain.command.Request;
import org.korsakow.domain.command.Response;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.ide.Application;

/**
 *
 * @author phoenix
 */
public class RelinkMediaAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent event) {
	IProject project;
	try {
	    project = ProjectInputMapper.find();
	} catch (MapperException e) {
	    Application.getInstance().showUnhandledErrorDialog(e);

	    return;
	}

	Request request = new Request();
	request.set("id", project.getId());
	Response response = new Response();

	try {
	    response = CommandExecutor.executeCommand(FixInvalidMediaCommand.class, request);
	} catch (CommandException e) {
	    Application.getInstance().showUnhandledErrorDialog(e);
	}

	Collection<IMedia> missing = null;
	
	if (response.has("missingMedia")) {
	    missing = (Collection<IMedia>) response.get("missingMedia");
	}

	Collection<IMedia> invalid = null;
	if (response.has("invalidMedia")) {
	    invalid = (Collection<IMedia>) response.get("invalidMedia");
	}

	if (invalid != null || missing != null) {
	    String message = "";

	    if (invalid != null) {
		message = invalid.size() + " invalid media files remain.\n";
	    }

	    if (missing != null) {
		message = message + missing.size() + " missing files remain.";
	    }

	    Application.getInstance().showAlertDialog("Invalid and Missing Media", message);
	} else {
	    String message = "Success!";

	    Application.getInstance().showAlertDialog("Success!", message);
	}

    }

}
