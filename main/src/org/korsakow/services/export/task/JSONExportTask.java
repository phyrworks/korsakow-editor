/**
 * 
 */
package org.korsakow.services.export.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.dsrg.soenea.domain.MapperException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.korsakow.domain.EventFactory;
import org.korsakow.domain.KeywordFactory;
import org.korsakow.domain.Media;
import org.korsakow.domain.PredicateFactory;
import org.korsakow.domain.RuleFactory;
import org.korsakow.domain.TriggerFactory;
import org.korsakow.domain.interf.IDynamicProperties;
import org.korsakow.domain.interf.IEvent;
import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.interf.IInterface;
import org.korsakow.domain.interf.IKeyword;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IPredicate;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.IResource;
import org.korsakow.domain.interf.IRule;
import org.korsakow.domain.interf.ISnu;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IText;
import org.korsakow.domain.interf.ITrigger;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.interf.IWidget;
import org.korsakow.ide.Build;
import org.korsakow.ide.DataRegistry;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.resources.PredicateType;
import org.korsakow.ide.resources.TriggerType;
import org.korsakow.ide.rules.RuleType;
import org.korsakow.ide.task.AbstractTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.ide.util.Util;
import org.korsakow.services.export.ExportData;
import org.korsakow.services.export.ExportException;
import org.korsakow.services.plugin.predicate.IArgumentInfo;
import org.korsakow.services.plugin.predicate.IPredicateTypeInfo;
import org.korsakow.services.plugin.predicate.PredicateTypeInfoFactory;
import org.korsakow.services.plugin.rule.IRuleTypeInfo;
import org.korsakow.services.plugin.rule.RuleTypeInfoFactory;
import org.korsakow.services.util.ColorFactory;
import org.w3c.dom.DOMException;

/**
 * TODO: this class should belong to the HTML export project.
 * 		 but to ease development it remains here for now.
 */
public class JSONExportTask extends AbstractTask
{
	@Deprecated
	private static class MyDynamicProperties implements IDynamicProperties
	{
		private String id;
		private Object value;
		public MyDynamicProperties() {
		}
		public MyDynamicProperties(String id, Object value) {
			this.id = id;
			this.value = value;
		}
		public Object getDynamicProperty(String id) {
			return value;
		}
		public Collection<String> getDynamicPropertyIds() {
			if (id == null) return Collections.emptyList();
			return Arrays.asList(id);
		}
		public void setDynamicProperty(String id, Object value) {
			throw new IllegalStateException();
		}
	}
	private final Collection<IVideo> videosToExport;// = new HashSet<IVideo>();
	private final Collection<ISound> soundsToExport;// = new HashSet<ISound>();
	private final Collection<IImage> imagesToExport;// = new HashSet<IImage>();
	private final Collection<IText> textsToExport;// = new HashSet<Text>();
	private final Collection<IInterface> interfacesToExport;// = new HashSet<IInterface>();;
	private final Collection<ISnu> snusToExport;// = new HashSet<ISnu>();
	private final IProject project;
	private final File rootDir;
	private final String dataPath;
	private final Map<String, String> filenamemap;
		
	public JSONExportTask(ExportData data)
	{
		data.dataPath = FileUtil.setFileExtension(data.dataPath, "js");
		List<IImage> adjustedImages = new ArrayList<IImage>(data.imagesToExport);
		for (ISnu snu : data.snusToExport) {
			if (snu.getThumbnail() != null) {
				adjustedImages.add(snu.getThumbnail());
			}
		}
		data.imagesToExport = adjustedImages;
		
		dataPath = data.dataPath;
		videosToExport = data.videosToExport;
		soundsToExport = data.soundsToExport;
		imagesToExport = data.imagesToExport;
		textsToExport = data.textsToExport;
		snusToExport = data.snusToExport;
		interfacesToExport = data.interfacesToExport;
		project = data.project;
		rootDir = data.rootDir;
		filenamemap = data.filenamemap;
		
	}
	
	@Override
	public String getTitleString()
	{
		return LanguageBundle.getString("export.task.xml");
	}
	@Override
	public void runTask() throws TaskException
	{
		JSONObject doc;
		try {
			doc = projectToDOM(project, snusToExport, textsToExport, imagesToExport, soundsToExport, videosToExport, interfacesToExport);
			FileUtil.writeFileFromString(new File(rootDir, dataPath), doc.toString(4));
		} catch (ExportException e) {
			throw new TaskException(e);
		} catch (IOException e) {
			throw new TaskException(e);
		} catch (MapperException e) {
			throw new TaskException(e);
		}
	}
	private static JSONArray keywordsToDom(Collection<IKeyword> keywords, String extraKeyword)
	{
		TreeSet<IKeyword> set = new TreeSet<IKeyword>(keywords); // treeset sorts them "naturally"
		if (extraKeyword != null)
			set.add(KeywordFactory.createNew(extraKeyword));
		JSONArray array = new JSONArray();
		for (IKeyword keyword : set) {
			JSONObject obj = new JSONObject();
			obj.put("value", keyword.getValue());
			obj.put("weight", keyword.getWeight());
			obj.put("className", "Keyword");
			array.put(obj);
		}
		return array;
	}
	private static void dynamicPropertiesToDom(JSONObject obj, IDynamicProperties absprops)
	{
		for (String id : absprops.getDynamicPropertyIds())
			obj.put(id, absprops.getDynamicProperty(id));
	}
	private static void dynamicPropertiesToDom(JSONObject obj, IRule rule) throws MapperException
	{
		IRuleTypeInfo typeInfo = RuleTypeInfoFactory.getFactory().getTypeInfo(rule.getRuleType());
		for (String id : rule.getDynamicPropertyIds()) {
			IArgumentInfo argInfo = typeInfo.getArgument(id);
			String value = argInfo.serialize(rule.getDynamicProperty(id));
			obj.put(id, value);
		}
	}
	private static void dynamicPropertiesToDom(JSONObject obj, IPredicate pred) throws MapperException
	{
		IPredicateTypeInfo typeInfo = PredicateTypeInfoFactory.getFactory().getTypeInfo(pred.getPredicateType());
		for (String id : pred.getDynamicPropertyIds()) {
			IArgumentInfo argInfo = typeInfo.getArgument(id);
			String value = argInfo.serialize(pred.getDynamicProperty(id));
			obj.put(id, value);
		}
	}
	private static JSONObject triggerToDom(long id, String type, IDynamicProperties dynamicProperties)
	{
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("type", type);
		obj.put("className", "Trigger");
		dynamicPropertiesToDom(obj, dynamicProperties);
		return obj;
	}
	private static JSONObject predicateToDom(long id, String type, IPredicate dynamicProperties, List<IPredicate> predicates) throws MapperException
	{
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("type", type);
		obj.put("className", "Predicate");
		if (dynamicProperties != null) // TODO: depricated check, needed until Snu.rules is eliminated
			dynamicPropertiesToDom(obj, dynamicProperties);

		JSONArray predicatesArray = new JSONArray();
		obj.put("predicates", predicatesArray);
		for (IPredicate sub : predicates) {
			predicatesArray.put(predicateToDom(sub.getId(), sub.getPredicateType(), sub, sub.getPredicates()));
		}
		return obj;
	}
	private static JSONObject ruleToDom(long id, String type, Collection<IKeyword> keywords, IRule dynamicProperties, List<IRule> rules) throws MapperException
	{
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("type", type);
		obj.put("className", "Rule");
		dynamicPropertiesToDom(obj, dynamicProperties);
		
		obj.put("keywords", keywordsToDom(keywords, null));

		JSONArray rulesArray = new JSONArray();
		obj.put("rules", rulesArray);
		if (RuleType.Search.getId().equals(type)) {
			boolean clearLinks = false;
			for (IRule sub : rules) {
				if (RuleType.ClearScores.getId().equals(sub.getRuleType())) {
					clearLinks = true;
					break;
				}
			
			}
			obj.put("keepLinks", !clearLinks);
 		}
		for (IRule sub : rules) {
			if (RuleType.ClearScores.getId().equals(sub.getRuleType())) {
				continue;
			}
			rulesArray.put(ruleToDom(sub.getId(), sub.getRuleType(), sub.getKeywords(), sub, sub.getRules()));
		}
		return obj;
	}
	private static JSONObject eventToDom(long id, ITrigger trigger, IPredicate predicate, IRule rule) throws DOMException, MapperException
	{
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("className", "Event");
		obj.put("Trigger", triggerToDom(trigger.getId(), trigger.getTriggerType(), trigger));
		obj.put("Predicate", predicateToDom(predicate.getId(), predicate.getPredicateType(), predicate, predicate.getPredicates()));
		obj.put("Rule", ruleToDom(rule.getId(), rule.getRuleType(), rule.getKeywords(), rule, rule.getRules()));
		return obj;
	}
	private static JSONObject widgetToDom(IWidget widget) throws ExportException
	{
		JSONObject obj = new JSONObject();
		obj.put("id", widget.getId());
		if (widget.getId() == null)
			throw new ExportException(new NullPointerException(), null);
		obj.put("type", widget.getWidgetId());
		obj.put("className", "Widget");
		JSONObject persist = new JSONObject();
		persist.put("condition", widget.getPersistCondition().getId());
		persist.put("action", widget.getPersistAction().getId());
		obj.put("persist", persist);

		obj.put("x", widget.getX());
		obj.put("y", widget.getY());
		obj.put("width", widget.getWidth());
		obj.put("height", widget.getHeight());
		dynamicPropertiesToDom(obj, widget);
		obj.put("keywords", keywordsToDom(widget.getKeywords(), widget.getName()));
		return obj;
	}
	private static void resourceToDom(IResource resource, JSONObject obj)
	{
		obj.put("id", resource.getId());
		obj.put("name", resource.getName());
		obj.put("keywords", keywordsToDom(resource.getKeywords(), resource.getName()));
	}
	private JSONObject videoToDom(IVideo video) throws IOException
	{
		JSONObject obj = new JSONObject();
		resourceToDom(video, obj);
		obj.put("className", "Video");

		String filename = getExportFilename(video.getAbsoluteFilename());

		obj.put("filename", formatExportUrl(filename));
		if (video.getSubtitles() != null) {
			String subtitlefilename = getExportFilename(Media.getAbsoluteFilename(video.getSubtitles()));
			obj.put("subtitles", formatExportUrl(subtitlefilename));
		}
		return obj;
	}
	private JSONObject textToDom(IText text) throws IOException
	{
		JSONObject obj = new JSONObject();
		resourceToDom(text, obj);
		obj.put("className", "Text");
		switch (text.getSource())
		{
		case INLINE:
			obj.put("text", text.getText());
			break;
		case FILE:
			String filename = getExportFilename(text.getAbsoluteFilename());
			obj.put("filename", formatExportUrl(filename));
			break;
		}
		return obj;
	}
	private JSONObject imageToDom(IImage image) throws IOException
	{
		JSONObject obj = new JSONObject();
		resourceToDom(image, obj);
		obj.put("className", "Image");
		String filename = getExportFilename(image.getAbsoluteFilename());
		obj.put("filename", formatExportUrl(filename));
		obj.put("duration", image.getDuration());
		return obj;
	}
	private JSONObject soundToDom(ISound sound) throws IOException
	{
		JSONObject obj = new JSONObject();
		resourceToDom(sound, obj);
		obj.put("className", "Sound");
		
		String filename = getExportFilename(sound.getAbsoluteFilename());
		
		obj.put("filename", formatExportUrl(filename));
		if (sound.getSubtitles() != null) {
			String subtitlefilename = getExportFilename(Media.getAbsoluteFilename(sound.getSubtitles()));
			obj.put("subtitles", formatExportUrl(subtitlefilename));
		}
		return obj;
	}
	private static JSONObject interfaceToDom(IInterface interf) throws ExportException
	{
		JSONObject obj = new JSONObject();
		resourceToDom(interf, obj);
		obj.put("className", "Interface");
        if (interf.getClickSound()!=null) {
        	obj.put("clickSoundId", interf.getClickSound().getId());
        	obj.put("clickSoundVolume", interf.getClickSoundVolume());
        }
        if (interf.getBackgroundImage()!=null) {
        	obj.put("backgroundImageId", interf.getBackgroundImage().getId());
        }
        if (interf.getBackgroundColor()!=null) {
        	obj.put( "backgroundColor", ColorFactory.toString(interf.getBackgroundColor()));
        }
        JSONArray widgetsArray = new JSONArray();
        obj.put("widgets", widgetsArray);
		Collection<IWidget> widgets = interf.getWidgets();
		for (IWidget widget : widgets)
			widgetsArray.put(widgetToDom(widget));
		return obj;
	}
	private static JSONObject snuToDom(ISnu snu) throws ExportException, DOMException, MapperException
	{
		JSONObject obj = new JSONObject();
		resourceToDom(snu, obj);
		obj.put("className", "Snu");
//		if (snu.getIVideo()!=null)
		if (snu.getThumbnail() != null)
			obj.put("thumbnailId", snu.getThumbnail().getId());
		obj.put("mainMediaId", snu.getMainMedia().getId());
		obj.put("rating", snu.getRating());
		obj.put("lives", snu.getLives());
		obj.put("maxLinks", snu.getMaxLinks());
		obj.put("looping", snu.getLooping());
		obj.put("starter", snu.getStarter());
		obj.put("ender", snu.getEnder());
        if (snu.getBackgroundSound()!=null) {
        	obj.put("backgroundSoundId", snu.getBackgroundSound().getId());
        	obj.put("backgroundSoundVolume", snu.getBackgroundSoundVolume());
        }
    	obj.put("backgroundSoundMode", snu.getBackgroundSoundMode().getId());
    	obj.put("backgroundSoundLooping", snu.getBackgroundSoundLooping());
    	JSONArray eventsArray = new JSONArray();
    	obj.put("events", eventsArray);
		for (IEvent event : snu.getEvents()) {
			eventsArray.put(eventToDom(event.getId(), event.getTrigger(), event.getPredicate(), event.getRule()));
		}
		for (final IRule rule : snu.getRules()) {
			JSONObject eventObj = new JSONObject();
			eventObj.put("id", DataRegistry.getMaxId());
			eventObj.put("className", "Event");
			eventObj.put("Trigger", triggerToDom(DataRegistry.getMaxId(), TriggerType.SnuTime.getId(), new MyDynamicProperties("time", rule.getTriggerTime())));
			eventObj.put("Predicate", predicateToDom(DataRegistry.getMaxId(), PredicateType.True.getId(), null, Util.list(IPredicate.class)));
			eventObj.put("Rule", ruleToDom(rule.getId(), rule.getRuleType(), rule.getKeywords(), rule, rule.getRules()));
			eventsArray.put(eventObj);
		}
		if (snu.getInterface()==null) {
			//TODO: Stu: this should throw some other form of exception which is caught and wrapped above
			throw new ExportException("SNU " + snu.getName() + "; " + LanguageBundle.getString("export.errors.snuhasnointerface"), null);
		}
		obj.put("interfaceId", snu.getInterface().getId());
		if (snu.getPreviewMedia()!=null)
			obj.put("previewMediaId", snu.getPreviewMedia().getId());
		if (snu.getPreviewImage()!=null)
			obj.put("previewImageId", snu.getPreviewImage().getId());
		obj.put("previewText", snu.getPreviewText());
		obj.put("insertText", snu.getInsertText());
		return obj;
	}
	private JSONObject projectToDOM(IProject project,
			Collection<ISnu> snusToExport,
			Collection<IText> textsToExport,
			Collection<IImage> imagesToExport,
			Collection<ISound> soundsToExport,
			Collection<IVideo> videosToExport,
			Collection<IInterface> interfacesToExport) throws ExportException, IOException, DOMException, MapperException
	{
		JSONObject root = new JSONObject();

        root.put("versionMajor", Build.getVersion());
        root.put("versionMinor", ""+Build.getRelease());
        
        JSONObject projObj = new JSONObject();
        root.put("Project", projObj);
        resourceToDom(project, projObj);
		projObj.put("className", "Project");
        
        projObj.put("uuid", project.getUUID());
//        if (project.getMovieWidth() != null)
        	projObj.put("movieWidth", project.getMovieWidth());
//        if (project.getMovieHeight() != null)
        	projObj.put("movieHeight", project.getMovieHeight());
        if (project.getBackgroundSound()!=null) {
        	projObj.put("backgroundSoundId", project.getBackgroundSound().getId());
        	projObj.put("backgroundSoundVolume", project.getBackgroundSoundVolume());
        	projObj.put("backgroundSoundLooping", project.getBackgroundSoundLooping());
        }
        if (project.getClickSound()!=null) {
        	projObj.put("clickSoundId", project.getClickSound().getId());
        	projObj.put("clickSoundVolume", project.getClickSoundVolume());
        }
        if (project.getSplashScreenMedia()!=null) {
        	projObj.put("splashScreenMediaId", project.getSplashScreenMedia().getId());
        }
        projObj.put("randomLinkMode", project.getRandomLinkMode());
        projObj.put("keepLinksOnEmptySearch", project.getKeepLinksOnEmptySearch());
        projObj.put("maxLinks", project.getMaxLinks());
        
		List<IEvent> events = new ArrayList<IEvent>();
		if (project.getBackgroundImage() != null) {
        	projObj.put("backgroundImageId", project.getBackgroundImage().getId());
        	
			ITrigger trigger = TriggerFactory.createClean(TriggerType.Initialized.getId());
			IPredicate pred = PredicateFactory.createClean(PredicateType.True.getId());
			IRule rule = RuleFactory.createClean(RuleType.SetBackgroundImage.getId());
			rule.setDynamicProperty("imageId", project.getBackgroundImage().getId());
			
			IEvent event = EventFactory.createClean(trigger, pred, rule);
			events.add(event);
		}
        if (project.getBackgroundColor()!=null) {
        	projObj.put("backgroundColor", ColorFactory.toString(project.getBackgroundColor()));
        }
        JSONArray eventsArray = new JSONArray();
		projObj.put("events", eventsArray);
		for (IEvent event : events) {
			eventsArray.put(eventToDom(event.getId(), event.getTrigger(), event.getPredicate(), event.getRule()));
		}
		
		JSONArray snusArray = new JSONArray();
		root.put("snus", snusArray);
        for (ISnu snu : snusToExport) {
        	try {
        		validate(snu);
        		snusArray.put(snuToDom(snu));
	    	} catch (ExportException e) {
	    		//Hack because the export Exception is thrown in a static method that
	    		//has no reason to know of the root dir.
	    		//TODO: Stu: this should throw some other form of Exception which is caught and converted.
	    		e.setProjectFile(rootDir);
	    		throw e;
	    	}
        }
        JSONArray interfsArray = new JSONArray();
        root.put("interfaces", interfsArray);
        for (IInterface interf : interfacesToExport) {
        	try {
        		interfsArray.put(interfaceToDom(interf));
        	} catch (ExportException e) {
        		//Hack because the export Exception is thrown in a static method that
        		//has no reason to know of the root dir.
        		//TODO: Stu: this should throw some other form of Exception which is caught and converted.
        		e.setProjectFile(rootDir);
        		throw e;
        	}
        }
        JSONArray textsArray = new JSONArray();
        root.put("texts", textsArray);
        for (IText text : textsToExport) {
        	try {
        		validate(text);
        	} catch (ExportException e) {
        		//Hack because the export Exception is thrown in a static method that
        		//has no reason to know of the root dir.
        		//TODO: Stu: this should throw some other form of Exception which is caught and converted.
        		e.setProjectFile(rootDir);
        		throw e;
        	}
        	textsArray.put(textToDom(text));
        }
        JSONArray videosArray = new JSONArray();
        root.put("videos", videosArray);
        for (IVideo video : videosToExport) {
        	validate(video);
        	videosArray.put(videoToDom(video));
        }
        JSONArray soundsArray = new JSONArray();
        root.put("sounds", soundsArray);
        for (ISound sound : soundsToExport) {
        	validate(sound);
        	soundsArray.put(soundToDom(sound));
        }
        JSONArray imagesArray = new JSONArray();
        root.put("images", imagesArray);
        for (IImage image : imagesToExport) {
        	validate(image);
        	imagesArray.put(imageToDom(image));
        }
        
        return root;
	}
	private static void validate(IMedia media) throws ExportException
	{
		switch (media.getSource())
		{
		case FILE:
			if (media.getFilename() == null)
				throw new ExportException("media has null filename: " + media.getName(), null);
			if (media.getFilename().length()==0)
				throw new ExportException("media has empty filename:" + media.getName(), null);
			File file = null;
			try {
				file = new File(media.getAbsoluteFilename());
			} catch (FileNotFoundException e) {
				throw new ExportException(e, null);
			}
			if (!file.exists() || !file.canRead())
				throw new ExportException("file not found or cannot read: " + file.getAbsolutePath(), null);
			break;
		case INLINE:
			break;
		default:
			throw new ExportException("invalid source: " + media.getSource(), null);
		}
	}
	private static void validate(ISnu snu) throws ExportException
	{
		IMedia media = snu.getMainMedia();
		if (media == null)
			throw new ExportException("SNU has no video", null);
		validate(media);
	}

	/**
	 * URL encodes the elements of the path as Per URLEncoder, however we ensure this is done
	 * for all entities, e.g. space encoded as %20, not +
	 * Path separators are not encoded.
	 * 
	 * This is sometimes necessary for being able to view files on the hard drive in the browser
	 * @throws UnsupportedEncodingException 
	 */
	public static String formatExportUrl(String filename) throws UnsupportedEncodingException
	{
		String[] pathparts = filename.split("[/\\\\]");
		StringBuilder sb = new StringBuilder();
		for (String part : pathparts)
		{
			String encoded = URLEncoder.encode(part, "UTF-8");
			encoded = encoded.replace("+", "%20");
			sb.append(encoded)
				.append('/');
		}
		sb.deleteCharAt(sb.length()-1); // final '/'
		
		filename = sb.toString();
		return filename;
	}
	private String getExportFilename(String mediafilename) throws IOException
	{
		if (filenamemap.containsKey(mediafilename))
			return filenamemap.get(mediafilename);
		throw new IOException("Filename not in map: " + mediafilename);
	}
}
