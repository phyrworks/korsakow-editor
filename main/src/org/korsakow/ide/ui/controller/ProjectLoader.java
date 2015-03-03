package org.korsakow.ide.ui.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.domain.command.CommandException;
import org.dsrg.soenea.environment.CreationException;
import org.dsrg.soenea.environment.KeyNotFoundException;
import org.dsrg.soenea.uow.UoW;
import org.korsakow.domain.CommandExecutor;
import org.korsakow.domain.Media;
import org.korsakow.domain.command.FindMissingFilesCommand;
import org.korsakow.domain.command.LoadProjectCommand;
import org.korsakow.domain.command.NewProjectCommand;
import org.korsakow.domain.command.Request;
import org.korsakow.domain.command.Response;
import org.korsakow.domain.interf.IMedia;
import org.korsakow.domain.interf.IProject;
import org.korsakow.domain.interf.ISound;
import org.korsakow.domain.interf.IVideo;
import org.korsakow.domain.mapper.input.ProjectInputMapper;
import org.korsakow.domain.mapper.input.ResourceInputMapper;
import org.korsakow.ide.Application;
import org.korsakow.ide.DataRegistry;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.task.AbstractTask;
import org.korsakow.ide.task.TaskException;
import org.korsakow.ide.task.UIWorker;
import org.korsakow.ide.ui.components.tree.FolderNode;
import org.korsakow.ide.ui.components.tree.KNode;
import org.korsakow.ide.ui.components.tree.ResourceNode;
import org.korsakow.ide.ui.controller.action.AbstractAction;
import org.korsakow.ide.ui.controller.action.helper.ProgressDialogStatusListener;
import org.korsakow.ide.ui.dialogs.MissingMediaDialog;
import org.korsakow.ide.ui.resourceexplorer.ResourceTreeTableModel;
import org.korsakow.ide.util.MultiMap;
import org.korsakow.ide.util.StrongReference;
import org.korsakow.ide.util.UIUtil;
import org.korsakow.ide.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ProjectLoader {

    public static void newProject() {
	Logger.getLogger(ProjectLoader.class).info("NewProject");
	try {
	    newProjectImpl();
	} catch (ParserConfigurationException | SAXException | XPathExpressionException | SQLException | IOException | InterruptedException e) {
	    Application.getInstance().showUnhandledErrorDialog("Error", "An unrecoverable error has occurred.", e);
	    // there's really very little we can do if we cant even reset the system.
	    System.exit(1);
	}
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

    public static void loadProject(final File file) throws SAXException, ParserConfigurationException, IOException, SQLException, XPathExpressionException, Throwable {
	loadProject(file, true);
    }

    public static void loadProject(final File file, final boolean setSaveFile) throws SAXException, ParserConfigurationException, IOException, SQLException, XPathExpressionException, Throwable {
	Logger.getLogger(ProjectLoader.class).info("loadProject:" + file != null ? file.getAbsolutePath() : "");
	if (!file.exists()) {
	    throw new FileNotFoundException(file.getAbsolutePath());
	}
	final Application app = Application.getInstance();
	try {
	    app.stopCommonTasks(); // this is necessary because we have these ui components updating all the time and we offload most of the work to another thread
	    app.clearRegistry();
	    app.getProjectExplorer().getResourceBrowser().getResourceTreeTable().getTreeTableModel().beginBatchUpdate();

	    final JDialog progressDialog = new JDialog(Application.getInstance().getProjectExplorer());
	    progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

	    final JProgressBar progressBar = new JProgressBar(0, 100);
	    progressBar.setIndeterminate(true);

	    progressDialog.setLayout(new BoxLayout(progressDialog.getContentPane(), BoxLayout.Y_AXIS));
	    progressDialog.add(progressBar);
	    progressDialog.setTitle("Loading...");
	    progressDialog.pack();
	    progressDialog.setSize(640, progressDialog.getSize().height);
	    UIUtil.centerOnFrame(progressDialog, app.getProjectExplorer());
	    progressDialog.setModal(true);

	    final StrongReference<IProject> projectRef = new StrongReference<>();
	    final StrongReference<Collection<IMedia>> missingMediaRef = new StrongReference<>();
	    final StrongReference<Collection<IMedia>> invalidMediaRef = new StrongReference<>();

	    UIWorker worker = new UIWorker(new AbstractTask() {
		@Override
		public void runTask() throws TaskException, InterruptedException {
		    Request request = new Request();
		    request.set("filename", file.getPath());
		    Response response;
		    try {
			response = CommandExecutor.executeCommand(LoadProjectCommand.class, request);
		    } catch (CommandException e) {
			throw new TaskException(e);
		    }

		    if (response.has("warnings")) {
			Application.getInstance().showAlertDialog("There were warnings", Util.join((Collection<String>) response.get("warnings"), "\n"));
		    }

		    IProject project = (IProject) response.get("project");
		    projectRef.set(project);
		    if (response.has("missingMedia")) {
			missingMediaRef.set((Collection<IMedia>) response.get("missingMedia"));
		    }

		    if (response.has("invalidMedia")) {
			invalidMediaRef.set((Collection<IMedia>) response.get("invalidMedia"));
		    }

		    final StrongReference<Exception> throwableRef = new StrongReference<>();
		    UIUtil.runUITaskNow(() -> {
			UoW.newCurrent();
			Application.getInstance().beginBusyOperation();
			try {
			    Element root = DataRegistry.getHelper().xpathAsElement("/korsakow/resources/Folder[@name=?]", "/");
			    if (root != null) {
				importResourceTree(app.getProjectExplorer().getResourceBrowser().getResourceTreeTable().getTreeTableModel(), root);
			    }
			} catch (XPathExpressionException e) {
			    throwableRef.set(e);
			} finally {
			    Application.getInstance().endBusyOperation();
			}
		    });
		    if (!throwableRef.isNull()) {
			throw new TaskException(throwableRef.get());
		    }
		}
	    });

	    worker.addPropertyChangeListener(new ProgressDialogStatusListener(progressDialog) {
		@Override
		public void onDone() {
		    UoW.newCurrent();

		    app.notifyKeywordsChanged(); // cause context panes to update
		    try {
			app.notifyProjectLoaded(ProjectInputMapper.find());
		    } catch (MapperException e) {
			Application.getInstance().showUnhandledErrorDialog(e);
		    }
		    app.getProjectExplorer();
		    if (setSaveFile) {
			app.setSaveFile(file, DataRegistry.getHeadVersion());
		    }

		    if (!invalidMediaRef.isNull() && !invalidMediaRef.get().isEmpty()) {
			Collection<IMedia> invalidMedia = invalidMediaRef.get();
			Collection<IMedia> replacedMedia = new HashSet<>();
			//attempt to auto-replace any invalid media with possible proper media that
			//has been placed in the same location (ie, if "car.mov" is invalid, and there
			//exists a file called "car.mp4", the filename will be replaced with the one
			//with the mp4 extension.  This can be done in parallel, so lets do that.
			invalidMedia.stream().filter((medium) -> (attemptToReplaceInvalidMedia(medium))).forEach((medium) -> {
			    replacedMedia.add(medium);
			});

			boolean modified = !replacedMedia.isEmpty();

			invalidMedia.removeAll(replacedMedia);

			if (modified) {
			    try {
				UoW.getCurrent().commit();
				UoW.newCurrent();
			    } catch (SQLException | KeyNotFoundException | CreationException | MapperException e) {
				Application.getInstance().showUnhandledErrorDialog(e);
			    }
			}
		    }

		    //Attempt to do the same auto-replace as the invalid media for missing media 
		    //- it may be the user put in replacement files, but removed the old files.
		    if (!missingMediaRef.isNull() && !missingMediaRef.get().isEmpty()) {
			Collection<IMedia> missingMedia = missingMediaRef.get();
			Collection<IMedia> replacedMedia = new HashSet<>();

			missingMedia.stream().filter((medium) -> (attemptToReplaceInvalidMedia(medium))).forEach((medium) -> {
			    replacedMedia.add(medium);
			});

			boolean modified = !replacedMedia.isEmpty();

			missingMedia.removeAll(replacedMedia);

			if (modified) {
			    try {
				UoW.getCurrent().commit();
				UoW.newCurrent();
			    } catch (SQLException | KeyNotFoundException | CreationException | MapperException e) {
				Application.getInstance().showUnhandledErrorDialog(e);
			    }
			}
		    }

		    //If there is still left over invalid media, show a dialog box to let
		    //user know how to replace it.
		    if (!invalidMediaRef.isNull() && !invalidMediaRef.get().isEmpty()) {
			Application.getInstance().showAlertDialog(LanguageBundle.getString("invalidmediadialog.title"), LanguageBundle.getString("invalidmediadialog.message"));
		    }
		    
		    //Anything left in the missingMedia is actually just missing, so see
		    //if we can get the user to find the missing media.
		    if (!missingMediaRef.isNull() && !missingMediaRef.get().isEmpty()) {
			Collection<IMedia> missingMedia = missingMediaRef.get();

			MissingMediaDialog missingMediaDialog = app.showMissingMediaDialog();

			missingMediaDialog.setMessage(LanguageBundle.getString("missingmediadialog.somemediacouldnotbelocated.label"));

			for (IMedia medium : missingMedia) {
			    missingMediaDialog.addMissingItem(medium);
			}

			missingMediaDialog.setFindMissingAction(new FindMissingAction(projectRef.get().getId(), missingMediaDialog));
			missingMediaDialog.expandAll();
			missingMediaDialog.setVisible(true);
		    }
		}

		@Override
		protected void handleException(Throwable e) {
		    newProject();
		}
	    });

	    worker.execute();

	    progressDialog.setVisible(true);

	    if (worker.getException() != null) {
		Throwable e = worker.getException();
		if (e instanceof CommandException && e.getCause() != null) {
		    e = e.getCause();
		}
		throw e;
	    }

	} finally {
	    app.startCommonTasks();
	    app.getProjectExplorer().getResourceBrowser().getResourceTreeTable().getTreeTableModel().endBatchUpdate();
	}
    }

    /**
     * This method is not really expected to ever throw. If it does its due to
     * internal failure.
     *
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    private static void newProjectImpl() throws ParserConfigurationException, SAXException, XPathExpressionException, SQLException, IOException, InterruptedException {
	Application app = Application.getInstance();

	app.stopCommonTasks();
	try {

	    UoW.newCurrent();
	    app.clearRegistry();
	    Response response = CommandExecutor.executeCommand(NewProjectCommand.class, new Request());
	    IProject project = (IProject) response.get("project");

	    app.notifyKeywordsChanged(); // cause context panes to update

	    // force snupanel to update now that resources are loaded (since Applications isXXReferenced methods currently incorrectly dont use a command)
	    app.getProjectExplorer().getResourceBrowser().getResourceTreeTable().getTreeTableModel().fireChanged();
	    app.getProjectExplorer();
	    app.setSaveFile(null, DataRegistry.getHeadVersion());

	    Element root = DataRegistry.getHelper().xpathAsElement("/korsakow/resources/Folder[@name=?]", "/");
	    if (root != null) {
		importResourceTree(app.getProjectExplorer().getResourceBrowser().getResourceTreeTable().getTreeTableModel(), root);
	    }
	} catch (CommandException e) {
	    Application.getInstance().showUnhandledErrorDialog("Unexpected error", e);
	} finally {
	    app.startCommonTasks();
	}
    }

    private static void importResourceTreeNode(ResourceTreeTableModel model, Element domParent, KNode treeParent) {
	NodeList childList = domParent.getChildNodes();
	int childLength = childList.getLength();
	for (int i = 0; i < childLength; ++i) {
	    if (childList.item(i) instanceof Element == false) {
		continue;
	    }
	    Element domChild = (Element) childList.item(i);
	    switch (domChild.getTagName()) {
		case "Folder":
		    FolderNode folderNode = new FolderNode(domChild.getAttribute("name"));
		    model.insertNodeInto(folderNode, treeParent, treeParent.getChildCount());
		    // recurse
		    importResourceTreeNode(model, domChild, folderNode);
		    break;
		case "Resource":
		    Long id = null;
		    try {
			id = Long.parseLong(domChild.getAttribute("id"));
		    } catch (NumberFormatException e) {
			Logger.getLogger(Application.class).error("", e);
			continue; // dunno what else to do
		    }
		    KNode resourceNode = model.findResource(id);
		    if (resourceNode != null) {
			Logger.getLogger(Application.class).error("resource already exist: " + id + "," + domChild.getAttribute("class"), new Exception());
			continue;
		    }
		    try {
			resourceNode = ResourceNode.create(ResourceInputMapper.map(id));
		    } catch (MapperException e) {
			Application.getInstance().showUnhandledErrorDialog(e);
			continue;
		    }
		    model.insertNodeInto(resourceNode, treeParent, treeParent.getChildCount()); // reparent the node at the new location
		    break;
	    }
	}
    }

    protected static void importResourceTree(ResourceTreeTableModel model, Element root) {
	importResourceTreeNode(model, root, model.getRoot());
    }

    private static final class ProgressListener extends ProgressDialogStatusListener {

	public ProgressListener(JDialog progressDialog) {
	    super(progressDialog);
	}

	@Override
	protected void onDone() {
	}
    }

    private static class FindMissingAction extends AbstractAction {

	private final long projectId;
	private final MissingMediaDialog missingMediaDialog;

	public FindMissingAction(long projectId, MissingMediaDialog missingMediaialog) {
	    this.projectId = projectId;
	    missingMediaDialog = missingMediaialog;
	}

	@Override
	public void performAction() {
	    Application app = Application.getInstance();
	    File basePath = app.showDirOpenDialog(missingMediaDialog, null);
	    if (basePath == null) {
		return;
	    }
	    Request request = new Request();
	    request.set("id", projectId);
	    request.set("basePath", basePath.getAbsolutePath());
	    request.set("updateUniqueMatches", true);
	    Response response = new Response();
	    try {
		CommandExecutor.executeCommand(FindMissingFilesCommand.class, request, response);
	    } catch (CommandException e) {
		e.printStackTrace();
	    }
	    MultiMap<String, File, Set<File>> possibleMatches = (MultiMap<String, File, Set<File>>) response.get("possibleMatches");
	    Collection<IMedia> updatedMedia = (Collection<IMedia>) response.get("updatedMedia");

	    Collection<IMedia> missingMedia = missingMediaDialog.getMissingMedia();

	    for (IMedia medium : missingMedia) {
		String filename = new File(medium.getFilename()).getName();
		if (possibleMatches.containsKey(filename)) {
		    missingMediaDialog.setPossibleMatches(medium, possibleMatches.get(filename));
		}
	    }
	    for (IMedia medium : updatedMedia) {
		missingMediaDialog.removeMissingItem(medium);
	    }
	    missingMediaDialog.expandAll();

	    app.showAlertDialog(LanguageBundle.getString("findmissingfiles.xoutofy.title"), LanguageBundle.getString("findmissingfiles.xoutofy.message", updatedMedia.size(), missingMedia.size()));

	    if (updatedMedia.size() == missingMedia.size()) {
		missingMediaDialog.dispose();
	    }
	}
    }
}
