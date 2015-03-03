package org.korsakow.domain.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.dsrg.soenea.environment.CreationException;
import org.dsrg.soenea.environment.KeyNotFoundException;
import org.dsrg.soenea.uow.UoW;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.ide.DataRegistry;
import org.korsakow.ide.util.DomUtil;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.services.conversion.ConversionException;
import org.korsakow.services.conversion.ConversionFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class LoadProjectCommand extends AbstractCommand{


	public LoadProjectCommand(Helper request, Helper response) {
		super(request, response);
		
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
	
	@Override
	public void execute()
			throws CommandException {
		try {
			String filename = request.getString("filename");
			IProject p = loadProject(new File(filename));
			response.set("project", p);
			
			Collection<IMedia> media = p.getMedia();
			//A Collection of media where the file is missing
			Collection<IMedia> missing = new HashSet<>();
			//A Collection of media where the file is an older invalid type
			Collection<IMedia> invalid = new HashSet<>();
			
			media.stream().forEach((medium) -> {
				try {
				String mediumFilename = medium.getAbsoluteFilename();
				
				if (checkforInvalidMedia(medium, mediumFilename))
				    invalid.add(medium);
				
				} catch (FileNotFoundException e) {
					missing.add(medium);
				}
			});
			
			if (!missing.isEmpty())
				response.set("missingMedia", missing);
			
			if (!invalid.isEmpty())
				response.set("invalidMedia", invalid);
			
			UoW.getCurrent().commit();
			UoW.newCurrent();
			
		} catch (MapperException | XPathExpressionException | SQLException | SAXException | ParserConfigurationException | IOException | KeyNotFoundException | CreationException | ConversionException e) {
			throw new CommandException(e);
		}
	}
	private IProject loadProject(File file) throws XPathExpressionException, SQLException, SAXException, ParserConfigurationException, IOException, MapperException, KeyNotFoundException, CreationException, ConversionException
	{
		Document document = DomUtil.parseXML(file);
		ConversionFactory cf = new ConversionFactory(document);
		cf.convert();
		if (!cf.getWarnings().isEmpty())
			response.set("warnings", cf.getWarnings());
		
		DataRegistry.initialize(document, file);
		IProject project = ProjectInputMapper.find();
		// this is for pre milestone 20, which don't have UUID
		if (project.getUUID() == null) {
			project.setUUID(UUID.randomUUID().toString());
			UoW.getCurrent().registerDirty(project);
		}
		return project;
	}
}
