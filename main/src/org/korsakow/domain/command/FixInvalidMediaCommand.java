/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.korsakow.domain.command;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.dsrg.soenea.environment.CreationException;
import org.dsrg.soenea.environment.KeyNotFoundException;
import org.dsrg.soenea.uow.UoW;
import org.korsakow.domain.Media;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.ide.Application;
import org.korsakow.ide.util.FileUtil;

/**
 *
 * @author phoenix
 */
public class FixInvalidMediaCommand extends AbstractCommand {

    public FixInvalidMediaCommand(Helper request, Helper response) {
	super(request, response);
    }

    @Override
    public void execute() throws CommandException {
	long projectId = request.getLong("id");

	IProject project;
	try {
	    project = ProjectInputMapper.map(projectId);
	} catch (MapperException e) {
	    throw new CommandException(e);
	}

	Collection<IMedia> media = project.getMedia();

	//A Collection of media where the file is missing
	Collection<IMedia> missing = new HashSet<>();
	//A Collection of media where the file is an older invalid type
	Collection<IMedia> invalid = new HashSet<>();

	media.stream().forEach((medium) -> {
	    try {
		String mediumFilename = medium.getAbsoluteFilename();

		if (this.checkforInvalidMedia(medium, mediumFilename)) {
		    invalid.add(medium);
		}

	    } catch (FileNotFoundException e) {
		missing.add(medium);
	    }
	});

	if (invalid.isEmpty() && missing.isEmpty()) {
	    return;
	}

	UoW.newCurrent();

	boolean modified = false;

	if (!invalid.isEmpty()) {
	    Collection<IMedia> replacedMedia = new HashSet<>();
	    //attempt to auto-replace any invalid media with possible proper media that
	    //has been placed in the same location (ie, if "car.mov" is invalid, and there
	    //exists a file called "car.mp4", the filename will be replaced with the one
	    //with the mp4 extension.  This can be done in parallel, so lets do that.
	    invalid.stream().filter((medium) -> (attemptToReplaceInvalidMedia(medium))).forEach((medium) -> {
		replacedMedia.add(medium);
	    });

	    modified = !replacedMedia.isEmpty();

	    invalid.removeAll(replacedMedia);
	}

	if (!missing.isEmpty()) {
	    Collection<IMedia> replacedMedia = new HashSet<>();
	    missing.stream().filter((medium) -> (attemptToReplaceInvalidMedia(medium))).forEach((medium) -> {
		replacedMedia.add(medium);
	    });

	    modified |= !replacedMedia.isEmpty();

	    missing.removeAll(replacedMedia);
	}

	if (modified) {
	    try {
		UoW.getCurrent().commit();
		UoW.newCurrent();
	    } catch (SQLException | KeyNotFoundException | CreationException | MapperException e) {
		Application.getInstance().showUnhandledErrorDialog(e);
	    }

	}

	if (!missing.isEmpty()) {
	    response.set("missingMedia", missing);
	}

	if (!invalid.isEmpty()) {
	    response.set("invalidMedia", invalid);
	}

    }

    /* 
     @param IMedia medium
     @return true if the filename is replaced with valid media, false if it is not.
     */
    private static boolean attemptToReplaceInvalidMedia(IMedia medium) {
	if (medium instanceof IVideo) {
	    //check to see if there is another in the folder with a proper extension (mp4/m4v).  This exploits the characteristic of ||, that it stops processing at the first value that returns true.
	    if (checkFilenameExistsAndReplace(medium, medium.getFilename(), ".mp4")
		    || checkFilenameExistsAndReplace(medium, medium.getFilename(), ".m4v")) {
		return true;
	    }
	} else if (medium instanceof ISound) {
	    ///check to see if there is another in the folder with a proper extension (mp4/m4v).  This exploits the characteristic of ||, that it stops processing at the first value that returns true.
	    if (checkFilenameExistsAndReplace(medium, medium.getFilename(), ".wav")
		    || checkFilenameExistsAndReplace(medium, medium.getFilename(), ".mp3")
		    || checkFilenameExistsAndReplace(medium, medium.getFilename(), ".m4a")
		    || checkFilenameExistsAndReplace(medium, medium.getFilename(), ".aif")
		    || checkFilenameExistsAndReplace(medium, medium.getFilename(), ".aiff")) {
		return true;
	    }
	}

	return false;
    }

    /* returns : true if replaced false otherwise */
    private static boolean checkFilenameExistsAndReplace(IMedia medium, String filename, String newExtension) {
	int dotIndex = filename.lastIndexOf('.');

	if (dotIndex == -1) {
	    dotIndex = filename.length();
	}

	String newFilename = filename.substring(0, dotIndex).concat(newExtension);

	try {
	    Media.getAbsoluteFilename(newFilename);
	} catch (FileNotFoundException e) {
	    return false;
	}

	//replace the media filename with this filename
	medium.setFilename(newFilename);

	//mark as dirty - the user will want to save this change
	UoW.getCurrent().registerDirty(medium);

	return true;
    }
    /*
     @param IMedia media
     @param IMedia mediumFilename 
	
     @returns true if media is invalid, false if it is valid
     */

    private boolean checkforInvalidMedia(IMedia medium, String mediumFilename) {
	if (medium instanceof IVideo) {
	    //for videos, check the extension to see if it is 
	    //one of our very limited mp4/m4v types (h264)

	    return !FileUtil.VIDEO_FILE_EXTENSION_PATTERN.matcher(mediumFilename).matches();
	} else if (medium instanceof ISound) {
	    //for audio, check the extension to see if it is 
	    //one of our very limited set of types (wav/mp3/m4a/aif/aiff)

	    return !FileUtil.SOUND_FILE_EXTENSION_PATTERN.matcher(mediumFilename).matches();
	}

	return false;
    }

}
