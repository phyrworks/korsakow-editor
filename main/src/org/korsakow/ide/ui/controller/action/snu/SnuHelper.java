package org.korsakow.ide.ui.controller.action.snu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.dsrg.soenea.domain.MapperException;
import org.korsakow.domain.RuleFactory;
import org.korsakow.domain.Settings;
import org.korsakow.domain.Snu;
import org.korsakow.domain.command.Request;
import org.korsakow.domain.command.UpdateSnuCommand;
import org.korsakow.domain.interf.IEvent;
import org.korsakow.domain.interf.IImage;
import org.korsakow.domain.interf.IInterface;
import org.korsakow.domain.interf.IKeyword;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IPredicate;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.IRule;
import org.korsakow.domain.interf.ISettings;
import org.korsakow.domain.interf.ISnu;
import org.korsakow.domain.interf.ISnu.BackgroundSoundMode;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.ITrigger;
import org.korsakow.domain.mapper.input.ImageInputMapper;
import org.korsakow.domain.mapper.input.InterfaceInputMapper;
import org.korsakow.domain.mapper.input.MediaInputMapper;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.domain.mapper.input.SettingsInputMapper;
import org.korsakow.domain.mapper.input.SoundInputMapper;
import org.korsakow.ide.code.RuleParserException;
import org.korsakow.ide.code.k5.K5Code;
import org.korsakow.ide.code.k5.K5CodeGenerator;
import org.korsakow.ide.code.k5.K5Lexeme;
import org.korsakow.ide.code.k5.K5RuleParser;
import org.korsakow.ide.code.k5.K5Symbol;
import org.korsakow.ide.resources.PredicateType;
import org.korsakow.ide.resources.TriggerType;
import org.korsakow.ide.rules.RuleType;
import org.korsakow.ide.ui.components.code.CodeTableModel;
import org.korsakow.ide.ui.controller.helper.ViewHelper;
import org.korsakow.ide.ui.model.EventModel;
import org.korsakow.ide.ui.model.PredicateModel;
import org.korsakow.ide.ui.model.RuleModel;
import org.korsakow.ide.ui.model.TriggerModel;
import org.korsakow.ide.ui.resources.SnuResourceView;

interface SnuValueExtractor {
	String getName();
	Collection<IKeyword> getKeywords();
	boolean getStarter();
	boolean getEnder();
	Long getMaxLinks();
	boolean getLooping();
	Long getMainMediaId();
	Long getMainMediaCustomDuration();
	BackgroundSoundMode getBackgroundSoundMode();
	Long getBackgroundSoundId();
	boolean getBackgroundSoundLooping();
	Long getInterfaceId();
	List<IRule> getRules();
	Long getLives();
	Long getPreviewImageId();
	Long getPreviewMediaId();
	String getPreviewText();
	String getInsertText();
	float getRating();
}

class SnuDomainObjectExtractor implements SnuValueExtractor {
	private final ISnu snu;
	public SnuDomainObjectExtractor(ISnu snu) {
		this.snu = snu;
	}
	@Override
	public String getName() { return snu.getName(); }
	@Override
	public Collection<IKeyword> getKeywords() { return snu.getKeywords(); }
	@Override
	public boolean getStarter() { return snu.getStarter(); }
	@Override
	public boolean getEnder() { return snu.getEnder(); }
	@Override
	public Long getMaxLinks() { return snu.getMaxLinks(); }
	@Override
	public boolean getLooping() { return snu.getLooping(); }
	@Override
	public Long getMainMediaId() { return snu.getMainMedia()!=null ? snu.getMainMedia().getId() : null; }
	@Override
	public Long getMainMediaCustomDuration() { return null; }
	@Override
	public BackgroundSoundMode getBackgroundSoundMode() { return snu.getBackgroundSoundMode(); }
	@Override
	public Long getBackgroundSoundId() { return snu.getBackgroundSound()!=null? snu.getBackgroundSound().getId() : null; }
	@Override
	public boolean getBackgroundSoundLooping() { return snu.getBackgroundSoundLooping(); }
	@Override
	public Long getInterfaceId() { return snu.getInterface().getId(); }
	@Override
	public List<IRule> getRules() { return snu.getRules(); }
	@Override
	public Long getLives() { return snu.getLives(); }
	@Override
	public Long getPreviewImageId() { return snu.getPreviewImage()!=null ? snu.getPreviewImage().getId() : null; }
	@Override
	public Long getPreviewMediaId() { return snu.getPreviewMedia()!=null ? snu.getPreviewMedia().getId() : null; }
	@Override
	public String getPreviewText() { return snu.getPreviewText(); }
	@Override
	public String getInsertText() { return snu.getInsertText(); }
	@Override
	public float getRating() { return snu.getRating(); }
}

class SnuResourceViewExtractor implements SnuValueExtractor {
	
	private final SnuResourceView view;
	
	public SnuResourceViewExtractor(SnuResourceView view) {
		this.view = view;	
	}

	public String getName() { return view.getNameFieldText(); }
	public Collection<IKeyword> getKeywords() { return view.getKeywords(); }
	public boolean getStarter() { return view.getStarter(); }
	public boolean getEnder() { return view.getEnder(); }
	public Long getMaxLinks() { return view.getMaxLinks(); }
	public boolean getLooping() { return view.getLooping(); }
	public Long getMainMediaId() { return view.getMainMediaId(); }
	public Long getMainMediaCustomDuration() { return view.getMainMediaCustomDuration(); }
	public BackgroundSoundMode getBackgroundSoundMode() { return view.getBackgroundSoundMode(); }
	public Long getBackgroundSoundId() { return view.getBackgroundSoundId(); }
	public boolean getBackgroundSoundLooping() { return view.getBackgroundSoundLooping(); }
	public Long getInterfaceId() { return view.getInterfaceId(); }
	public List<IRule> getRules() { return getSearchRules(view); }
	public Long getLives() { return view.getLives(); }
	public Long getPreviewImageId() { return view.getPreviewImageId(); }
	public Long getPreviewMediaId() { return view.getPreviewMediaId(); }
	public String getPreviewText() { return view.getPreviewText(); }
	public String getInsertText() { return view.getInsertText(); }
	public float getRating() { return view.getRating(); }

	public static List<IRule> getSearchRules(SnuResourceView resourceView)
	{
		K5RuleParser parser = new K5RuleParser();
		List<IRule> rules = new ArrayList<IRule>();
		CodeTableModel codeModel = resourceView.getCodeTable().getModel();
		boolean isFirstTime = true;
		for (int i = 0; i < codeModel.getRowCount(); ++i)
		{
			Long time = codeModel.getTimeAt(i);
			if (time == null) {
				// not sure what we should do... for now we just ignore them
				continue;
			}
			Long maxLinks = codeModel.getMaxLinksAt(i);
			K5Code code = codeModel.getCodeAt(i);
			List<IRule> subRules = Collections.EMPTY_LIST;
			try {
				List<K5Lexeme> lexemes = parser.tokenize(code.getRawCode());
				subRules = parser.createRules(lexemes);
				// handle special cases
				boolean clearorkeepIsExplicitlyStated = false;
				K5Lexeme firstLexeme = lexemes.isEmpty()?null:lexemes.get(0);
				if (firstLexeme != null)
				{
					switch (firstLexeme.getOpType())
					{
					case CLEAR_PREVIOUS_LINKS:
					case KEEP_PREVIOUS_LINKS:
						clearorkeepIsExplicitlyStated = true;
						break;
					}
				}
				if (!clearorkeepIsExplicitlyStated)
				{
					if (isFirstTime) {
						subRules.add(0, RuleFactory.createClearScoresRule());
					}
					// else do nothing since there is no (and no need for) a KeepScoresRule
				}
			} catch (RuleParserException e) {
				// just omit these
				// its okay since the code editor identifies erroneous entries
				continue;
			}
			IRule searchRule = RuleFactory.createSearchRule(subRules);
			searchRule.setTriggerTime(time);
			searchRule.setDynamicProperty("maxLinks", maxLinks);
			rules.add(searchRule);
			isFirstTime = false;
		}
		return rules;
	}
}

public class SnuHelper {
	public static Request createRequest(ISnu snu) {
		return createRequest(new SnuDomainObjectExtractor(snu), snu.getId());
	}
	public static Request createRequest(SnuResourceView view, Long id) {
		return createRequest(new SnuResourceViewExtractor(view), id);
	}
	private static Request createRequest(SnuValueExtractor view, Long id)
	{
		Request request = new Request();
		
		request.set(UpdateSnuCommand.ID, id);
		request.set(UpdateSnuCommand.NAME, view.getName().trim());
		request.set(UpdateSnuCommand.KEYWORDS, view.getKeywords());
		
		request.set(UpdateSnuCommand.MAIN_MEDIA_ID, view.getMainMediaId());
		if ( view.getMainMediaCustomDuration() != null )
			request.set(UpdateSnuCommand.MAIN_MEDIA_CUSTOM_DURATION, view.getMainMediaCustomDuration() );
		request.set(UpdateSnuCommand.RATING, view.getRating());
		switch (view.getBackgroundSoundMode())
		{
		case CLEAR:
		case KEEP:
			request.set(UpdateSnuCommand.BACKGROUND_SOUND_ID, null);
			break;
		case SET:
			request.set(UpdateSnuCommand.BACKGROUND_SOUND_ID, view.getBackgroundSoundId());
			break;
		}
		request.set(UpdateSnuCommand.BACKGROUND_SOUND_MODE, view.getBackgroundSoundMode());
		request.set(UpdateSnuCommand.BACKGROUND_SOUND_VOLUME, 1.0f);
		request.set(UpdateSnuCommand.BACKGROUND_SOUND_LOOPING, view.getBackgroundSoundLooping());
		request.set(UpdateSnuCommand.LIVES, view.getLives());
		request.set(UpdateSnuCommand.INTERFACE_ID, view.getInterfaceId());
		request.set(UpdateSnuCommand.MAX_LINKS, view.getMaxLinks());
		request.set(UpdateSnuCommand.LOOPING, view.getLooping());
		request.set(UpdateSnuCommand.STARTER, view.getStarter());
		request.set(UpdateSnuCommand.ENDER, view.getEnder());
		request.set(UpdateSnuCommand.PREVIEW_IMAGE_ID, view.getPreviewImageId());
		request.set(UpdateSnuCommand.PREVIEW_MEDIA_ID, view.getPreviewMediaId());
		request.set(UpdateSnuCommand.PREVIEW_TEXT, view.getPreviewText());
		request.set(UpdateSnuCommand.INSERT_TEXT, view.getInsertText());
		
		ViewHelper.addRulesToRequest(request, view.getRules());
		
		return request;
	}
	
	public static void initView(SnuResourceView view, String name, IMedia mainMedia) throws MapperException
	{
		ISettings settings = SettingsInputMapper.find();
		final String similarName = settings.getBoolean(Settings.PutSimilarResourcesAtTop)?mainMedia.getName():null;
		final IProject project = ProjectInputMapper.find();
		
		view.setNameFieldText(name);
		view.setMainMedia(mainMedia);
		view.setRating(1.0f);
		view.setPreviewImageChoices(ViewHelper.sort(ImageInputMapper.findAll(), IImage.class, similarName));
		view.setPreviewImage(null);
		view.setPreviewMediaChoices(ViewHelper.sort(MediaInputMapper.findAll(), IMedia.class, similarName));
		view.setPreviewMedia(mainMedia);
		List<IInterface> interfaces = InterfaceInputMapper.findAll();
		view.setInterfaceChoices(ViewHelper.sort(interfaces, IInterface.class, similarName));
		if (project.getDefaultInterface() != null)
			view.setInterface(project.getDefaultInterface());
		else if (!interfaces.isEmpty())
			view.setInterface(interfaces.get(0));
		view.setBackgroundSoundChoices(ViewHelper.sort(SoundInputMapper.findAll(), ISound.class, similarName));
		view.setBackgroundSound(Snu.DEFAULT_BACKGROUNDSOUNDMODE);
		
		view.repaint();
		view.revalidate();
	}
	public static void initView(SnuResourceView view, ISnu snu) throws MapperException
	{
		ISettings settings = SettingsInputMapper.find();
		final String similarName = settings.getBoolean(Settings.PutSimilarResourcesAtTop)?snu.getMainMedia().getName():null;
		
		view.setResourceId(snu.getId());
		view.setNameFieldText(snu.getName());
		view.setKeywords(snu.getKeywords());
		
		view.setMainMedia(snu.getMainMedia());
		
		view.setRating(snu.getRating());
		view.setBackgroundSoundChoices(ViewHelper.sort(SoundInputMapper.findAll(), ISound.class, similarName));
		if (snu.getBackgroundSoundMode() == BackgroundSoundMode.SET)
			view.setBackgroundSound(snu.getBackgroundSound());
		else
			view.setBackgroundSound(snu.getBackgroundSoundMode());
		view.setBackgroundSoundLoop(snu.getBackgroundSoundLooping());

		view.setInterfaceChoices(ViewHelper.sort(InterfaceInputMapper.findAll(), IInterface.class, similarName));
		view.setInterface(snu.getInterface());
		view.setLives(snu.getLives());
		view.setMaxLinks(snu.getMaxLinks());
		view.setLooping(snu.getLooping());
		view.setStarter(snu.getStarter());
		view.setEnder(snu.getEnder());
		view.setPreviewImageChoices(ViewHelper.sort(ImageInputMapper.findAll(), IImage.class, similarName));
		view.setPreviewImage(snu.getPreviewImage());
		view.setPreviewMediaChoices(ViewHelper.sort(MediaInputMapper.findAll(), IMedia.class, similarName));
		view.setPreviewMedia(snu.getPreviewMedia());
		view.setPreviewText(snu.getPreviewText());
		view.setInsertText(snu.getInsertText());


		List<IRule> allRules = new ArrayList<IRule>(snu.getRules());
		List<IRule> searchRules = getSearchRules(allRules);
		allRules.removeAll(searchRules);
		setSearchRules(view, searchRules);
		view.setCachedRules(new ArrayList<IRule>(allRules));
		
		view.repaint();
		view.revalidate();
	}
	private static TriggerModel getTriggerModel(ITrigger trigger)
	{
		TriggerType triggerType = TriggerType.forId(trigger.getTriggerType());
		TriggerModel model = new TriggerModel(triggerType);
		for (String id : trigger.getDynamicPropertyIds())
			model.addProperty(id, trigger.getDynamicProperty(id));
		List<TriggerModel> children = new ArrayList<TriggerModel>();
		return model;
	}
	private static PredicateModel getPredicateModel(IPredicate predicate)
	{
		PredicateType predicateType = PredicateType.forId(predicate.getPredicateType());
		PredicateModel model = new PredicateModel(predicateType);
		for (String id : predicate.getDynamicPropertyIds())
			model.addProperty(id, predicate.getDynamicProperty(id));
		List<PredicateModel> children = new ArrayList<PredicateModel>();
		for (IPredicate child : predicate.getPredicates())
			children.add(getPredicateModel(child));
		model.setPredicates(children);
		return model;
	}
	private static RuleModel getRuleModel(IRule rule)
	{
		RuleType ruleType = RuleType.forId(rule.getRuleType());
		RuleModel model = new RuleModel(ruleType);
		for (String id : rule.getDynamicPropertyIds())
			model.setProperty(id, rule.getDynamicProperty(id));
		List<RuleModel> children = new ArrayList<RuleModel>();
		for (IRule child : rule.getRules())
			children.add(getRuleModel(child));
		model.setRules(children);
		return model;
	}
	private static Collection<EventModel> createEventModels(Collection<IEvent> events)
	{
		Collection<EventModel> eventModels = new ArrayList<EventModel>();
		for (IEvent event : events) {
			
			TriggerModel triggerModel = getTriggerModel(event.getTrigger());
			PredicateModel predicateModel = getPredicateModel(event.getPredicate());
			RuleModel ruleModel = getRuleModel(event.getRule());
			
			EventModel model = new EventModel(triggerModel, predicateModel, ruleModel);
			
			eventModels.add(model);
		}
		return eventModels;
	}
	private static List<IRule> getSearchRules(List<IRule> snuRules)
	{
		List<IRule> searchRules = new ArrayList<IRule>();
		for (IRule rule : snuRules)
			if (RuleType.Search.getId().equals(rule.getRuleType()))
				searchRules.add(rule);
		return searchRules;
	}
	private static void setSearchRules(SnuResourceView resourceView, List<IRule> searchRules)
	{
		K5CodeGenerator gen = new K5CodeGenerator();
		CodeTableModel codeModel = new CodeTableModel();

		boolean isFirstTime = true;
		for (IRule searchRule : searchRules) {
			
			List<IRule> rules = searchRule.getRules();
			
			// this is the default behavior and is not shown in the ui
//			if (isFirstTime && !rules.isEmpty() && rules.get(0).getRuleType() == RuleType.ClearScores)
//				rules.remove(0);
			// this is the default behavior and is not shown in the ui
//			if (!isFirstTime && !rules.isEmpty() && rules.get(0).getRuleType() != RuleType.ClearScores)
//				rules.remove(0);
			K5Code code = gen.createK5CodeOmitUnsupported(rules, isFirstTime);
			if (isFirstTime && (rules.isEmpty() || !RuleType.ClearScores.getId().equals(rules.get(0).getRuleType()))) {
				code.setRawCode(K5Symbol.KEEP_PREVIOUS_LINKS + K5Symbol.DEFAULT_STATEMENT_SEPARATOR_STRING + code.getRawCode());
			}
			int row = codeModel.addRow(searchRule.getTriggerTime(), code);
			Long maxLinks;
			try {
				maxLinks = Long.parseLong(""+searchRule.getDynamicProperty("maxLinks"));
			} catch (Exception e) {
				maxLinks = null;
			}
			codeModel.setMaxLinks(maxLinks, row);
			isFirstTime = false;
		}
		resourceView.getCodeTable().setModel(codeModel);
	}
 }
