package org.korsakow.ide;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.SQLException;

import javafx.embed.swing.JFXPanel;

import javax.swing.JOptionPane;
import javax.swing.JWindow;
import javax.swing.Timer;

import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.util.PluginManagerUtil;

import org.apache.log4j.Logger;
import org.dsrg.soenea.service.threadLocal.DbRegistry;
import org.korsakow.ide.controller.ApplicationAdapter;
import org.korsakow.ide.lang.LanguageBundle;
import org.korsakow.ide.ui.ProjectExplorer;
import org.korsakow.ide.ui.SplashPage;
import org.korsakow.ide.util.Platform;
import org.korsakow.ide.util.UIUtil;
import org.korsakow.services.encoders.image.ImageEncoderFactory;
import org.korsakow.services.encoders.image.JavaImageIOImageEncoder;
import org.korsakow.services.encoders.sound.SoundEncoderFactory;
import org.korsakow.services.encoders.sound.lame.plaf.LameEncoderOSX;
import org.korsakow.services.encoders.sound.lame.plaf.LameEncoderWin32;
import org.korsakow.services.encoders.video.VideoEncoderFactory;
import org.korsakow.services.encoders.video.ffmpeg.plaf.FFMpegEncoderOSX;
import org.korsakow.services.encoders.video.ffmpeg.plaf.FFMpegEncoderWin32;
import org.korsakow.services.plugin.PluginHelper;
import org.korsakow.services.plugin.PluginRegistry;
import org.korsakow.services.plugin.export.ExportPlugin;
import org.korsakow.services.updater.Updater;

public class Main {
	/**
	 * Introduced because ApplicationShutdownListener is added as a weak reference
	 */
	private static Main main;
	public static void main(String[] args) throws Exception {
		main = new Main(args);
	}

	
	private class ApplicationShutdownListener extends ApplicationAdapter implements Runnable
	{
		@Override
		public void onApplicationShutdown() {
			// do later so that other shutdown listeners can do their stuff
			UIUtil.runUITaskLater(this);
		}
		public void run() {
			try {
				shutdown();
			} catch (Exception e) {
				getLogger().error("", e);
			}
		}
	}
	
	private final ApplicationShutdownListener applicationShutdownListener = new ApplicationShutdownListener();
	private static Logger logger;
	private static final Logger getLogger() {
		if (logger == null ) {
			logger = Logger.getLogger(Main.class);
		}
		return logger;
	}

	public Main(String[] args) throws Exception {
		if (Platform.getOS() == Platform.OS.UNKNOWN)
		{
			JOptionPane.showMessageDialog(null, 
					LanguageBundle.getString("general.errors.unsupportedplatform.message", 
							Platform.getOS().getCanonicalName(), 
							Platform.getArch()), 
					LanguageBundle.getString("general.errors.unsupportedplatform.title"), 
					JOptionPane.ERROR_MESSAGE);
		}
		
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");

		// These preferences are best set on init of the application
		if ( Platform.getOS() == Platform.OS.MAC ) {
//			System.setProperty("apple.laf.useScreenMenuBar", "true"); // removed per #840
			System.setProperty("com.apple.mrj.application.apple.menu.about.name",
					"Korsakow");
		}
		
		final Exception[] remoteException = new SQLException[1];
		Thread.currentThread().setUncaughtExceptionHandler(
				new UncaughtExceptionHandler() {
					public void uncaughtException(Thread thread,
							Throwable exception) {
						Logger.getLogger(Application.class)
								.error("", exception);
					}
				});

		setup();
		
		getLogger().info("CommandLine Arguments");
		for (String arg : args) {
			getLogger().info("\t" + arg + "\n");
		}

		
		/* without some amount of jfx initialization, strange unpredictable
		 * failures happen when trying to load/display videos */
		javafx.application.Platform.setImplicitExit(false);
		/*
		Since we need JavaFX for the web window, and since we don't want
		the web window to have to be embedded in a Swing window (rendering 
		of the Javafx WebView is slooooow when embedded in Swing), we
		need to start up the JavaFX environment.  There are two ways
		to do this: subclass the JavaFX Application class (which brings
		in several complications that we don't want right now), or 
		create a JFXPanel, which implicitly creates the environment.
		So we create (and then immediately discard) a JFXPanel here.
		*/
		
		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow() {
		    @Override
		    public void run() {
			final JFXPanel initPanel = new JFXPanel(); // initializes JavaFX environment;\
		    }
		});
		
		
		javafx.application.Platform.setImplicitExit(false);
		/*
		Since we need JavaFX for the web window, and since we don't want
		the web window to have to be embedded in a Swing window (rendering 
		of the Javafx WebView is slooooow when embedded in Swing), we
		need to start up the JavaFX environment.  There are two ways
		to do this: subclass the JavaFX Application class (which brings
		in several complications that we don't want right now), or 
		create a JFXPanel, which implicitly creates the environment.
		So we create (and then immediately discard) a JFXPanel here.
		*/
		
		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow() {
		    @Override
		    public void run() {
			final JFXPanel initPanel = new JFXPanel(); // initializes JavaFX environment;\
		    }
		});

		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow() {
			public void run() {
				UIUtil.setUpLAF();
			}
		});

		final JWindow splashDialog = new JWindow();

		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow() {
			public void run() {
				
				splashDialog.setAlwaysOnTop(true);
				SplashPage page = new SplashPage();
				page.setUUIDVisible(false);
				splashDialog.add(page);
				splashDialog.pack();

				UIUtil.centerOnScreen(splashDialog);
			}
		});
		
		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow(){
			public void run() throws Throwable {
				// doing this separately avoids an issue where the splash shows up unpainted for a second
//				splashDialog.setVisible(true);
				splashDialog.toFront();
			}
		});

		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow() {
			public void run() throws Throwable {
				File pluginDir = PluginHelper.ensurePluginsDir();
				getLogger().info("Loading plugins from ..." + pluginDir.getPath());

				PluginManager pluginManager = PluginManagerFactory.createPluginManager();
				if (pluginDir.listFiles() != null) {
					for (File child : pluginDir.listFiles()) {
						if (child.isFile()) {
							getLogger().info(String.format("Found possible plugin at: %s", child.getPath()));
							pluginManager.addPluginsFrom(child.toURI());
						}
					}
				}
				
				PluginManagerUtil pluginUtil = new PluginManagerUtil(pluginManager);
				
				for (ExportPlugin plugin : pluginUtil.getPlugins(ExportPlugin.class)) {
					getLogger().info(String.format("Installing Export Plugin: %s", plugin.getName()));
					PluginRegistry.get().register(plugin);
				}
				
			}
		});
		
		UIUtil.runUITaskNowThrow(new UIUtil.RunnableThrow() {
			public void run() throws Throwable {
				long beforeTime = System.currentTimeMillis();
				
				Application.initializeInstance();
				final Application app = Application.getInstance();
				app.addApplicationListener(applicationShutdownListener);
				
				final ProjectExplorer explorer = app.showProjectExplorer();
				UIUtil.centerOnScreen(explorer);
				
				long afterTime = System.currentTimeMillis();
				long delta = afterTime - beforeTime;
				long minTime = 2000;
				
				long timerTime = Math.max(1, minTime-delta);
				Timer timer = new Timer((int)timerTime, new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						splashDialog.dispose();
						explorer.setVisible(true);
						app.getProjectExplorerController().loadDefaultProject();
						
						Updater.checkAsynch();
					}
				});
				timer.setRepeats(false);
				timer.start();
			}
		});
	}

	private void shutdown() throws Exception {
		getLogger().info("shutdown begin");
		try {
			shutdownUoW();
		} catch (Exception e) {
			getLogger().error("", e);
		}
		try {
			DataRegistry.getConnection().commit();
		} catch (Exception e) {
			getLogger().error("", e);
		}

		getLogger().info(
				"shutdown complete (this should be the last item logged)");
		System.exit(0);
	}

	private void shutdownUoW() throws Exception {
		final Exception[] remoteException = new SQLException[1];
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					DbRegistry.getDbConnection().rollback();
				} catch (SQLException e) {
					Logger.getLogger(Application.class).error("", e);
					remoteException[0] = e;
					return;
				}

			}
		};
		UIUtil.runUITaskNow(runnable);
		if (remoteException[0] != null)
			throw remoteException[0];
	}

	private void setup() throws Exception {
		// setup the UUID as early as possible as it is used in error reporting and logging.
		Application.getUUID(); // side effect causes the UUID to be persisted.
		setupLogging();
		setupPlatform();
	}

	public static void setupLogging() {
		System.setProperty("org.korsakow.log.filename", Application.getLogfilename());
		getLogger().info(Build.getAboutString());
		getLogger().info(String.format("Java: JVM %s, JRE %s, ", System.getProperty("java.version"), System.getProperty("java.class.version")));
		getLogger().info(String.format("Platform: %s %s\n\tDetected as: Operating System: %s, Architechture: %s", Platform.getArchString(), Platform.getOSString(), Platform.getOS().getCanonicalName(), Platform.getArch()));
		getLogger().info(String.format("UUID: %s", Application.getUUID()));
		getLogger().info(String.format("Korsakow Home set to: %s", Application.getKorsakowHome()));
	}

	private static void setupPlatform() {
		setupPlatformEncoders();
	}
	public static void setupPlatformEncoders() {
		switch (Platform.getOS()) {
		case MAC:
			SoundEncoderFactory.getDefaultFactory().addEncoder(
					new LameEncoderOSX.LameEncoderOSXDescription());
			VideoEncoderFactory.addEncoder(
					new FFMpegEncoderOSX.FFMpegEncoderOSXDescription());
			break;
		case WIN:
			// FontEncoderFactory.getDefaultFactory().addEncoder(new
			// SwfMillEncoderOSX.SwfMillEncoderWin32XDescription());
			SoundEncoderFactory.getDefaultFactory().addEncoder(
					new LameEncoderWin32.LameEncoderWin32Description());
			VideoEncoderFactory.addEncoder(
					new FFMpegEncoderWin32.FFMpegEncoderWin32Description());
			break;
		case NIX:
		default:
			System.out.println("Platform specific features not yet supported");
			break;
		}
		ImageEncoderFactory.addEncoder(new JavaImageIOImageEncoder.JavaImageIOEncoderDescription());
	}
}
