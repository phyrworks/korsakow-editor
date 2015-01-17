package org.korsakow.ide.resources.media;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.korsakow.ide.exception.MediaRuntimeException;
import org.korsakow.ide.util.JFXRunTask;

/**
 *
 * @author phoenix
 */
public final class JFXVideo extends AbstractPlayableVideo
{	
    private final Log log = LogFactory.getLog(getClass());

    private URI movieURI;
    private MediaPlayer moviePlayer;
    private MediaView movieView;
    private JFXPanel moviePanel;
    private AtomicLong currentTime;
    private AtomicLong currentVolume;
    private AtomicLong duration;
    private AtomicReference<MediaPlayer.Status> status;
    
    //The following values will not be valid until the movie is ready (pre-rolled)
    //We provide objects for locking the individual values, so that we can
    //get/set values without locking this
//    private Duration movieDuration = new Duration(0);
//    private final Object movieDurationLock = new Object();
//    private Duration currentTime = new Duration(0);
//    private final Object currentTimeLock = new Object();
//    private double currentVolume = 0.;
//    private final Object currentVolumeLock = new Object();
    private boolean movieIsReady = false;
    final private JFXRunTask.SimpleGetterTask<Boolean> readyTask;
//    private boolean movieIsPlaying = false;
//    private final Object movieIsPlayingLock = new Object();
   
    
    public JFXVideo(String url) throws MediaRuntimeException {
        try {
	    currentTime = new AtomicLong(0);
	    currentVolume = new AtomicLong(0);
	    duration = new AtomicLong(0);
        status = new AtomicReference<MediaPlayer.Status>(MediaPlayer.Status.UNKNOWN);
	    
	    readyTask = new JFXRunTask.SimpleGetterTask<>(false);
	    
	    LogFactory.getLog(getClass()).info(this + " :: " + url);
	    //convert the url to a proper uri for JavaFX media
	    File movieFile = new File(url).getAbsoluteFile();
	    
            movieURI = movieFile.toURI();
	    
	    
            moviePanel = new JFXPanel();
           
            
            if (!Platform.isFxApplicationThread()) {
		initializeScene();
            } else {
                throw new MediaRuntimeException("JFXVideo constructor must not be called on the JavaFX thread");
            }
        
        } catch (LogConfigurationException | MediaRuntimeException e){
            throw new MediaRuntimeException(e);
        }
    }
  
   
    /*
    All JavaFX scene commands must be done on the event dispatch
    thread (not the Swing thread).  We are going to do a bit of an odd
    thing hear to account for some ideosyncratic behaviour of the
    old Quicktime interface that we are replacing.  While the Quicktime
    interface was always ultimately asynchronous, it had certain workarounds
    in place to make it behave as if it were synchronous - namely the pre-roll
    routines could be treated as if synchronous.  This behaviour is
    not present in nearly any first class, modern media players
    (see for instance the Mac OSX/iOS AVFoundation routines).  Unfortunately
    there are many apparent dependencies within Korsakow that rely on
    the old synchonous behaviour (for instance, MediaInfoFactory expected
    the QTVideo constructor to return immediately with the movie
    already pre-rolled).  So we have a problem - either we reconstruct
    Korsakow to a more non-linear, event driven approach when relying
    on media loading - which could take quite a bit of work - or we
    attempt to make the pre-roll of the media synchronous.  In favor
    of symplifying the outer code that relies on these classes, the
    following is an attempt to cause the pre-roll to load syncronously.
    The side effect is that this could cause the entire application
    to hault if there are lengthy operations queued on the JavaFX thread,
    or if the video is slow to pre-load.
    */
    private synchronized void initializeScene() throws MediaRuntimeException {
	try {           
	    //Create a task that will run on the JavaFX Application thread, and
	    //block the current thread until the JafaFX Application thread returns.
	    JFXRunTask.get(new Callable<Void>() {
		@Override public Void call() {
		    createJFXScene();

		    return null;
		}
	    });

	    //Block until the movie has pre-rolled it's headers.  This will help
	    //us guarantee that all necessary values of the moviePlayer have
	    //been initialized before we continue.  This is all here to mimic
	    //the behaviour of the old Quicktime API, which is perhaps not the 
	    //best thing to be doing.
	    movieIsReady = readyTask.get();
	
	} catch (InterruptedException | ExecutionException e) {
	    throw new MediaRuntimeException(e);    
	}
    }
    
    private void createJFXScene() throws MediaRuntimeException  {
        try {
	    //JavaFX needs the path in the form of a valid URI
            Media movie = new Media(movieURI.toString());
            moviePlayer = new MediaPlayer(movie);
            movieView = new MediaView(moviePlayer);
            
            moviePlayer.setOnError(new Runnable() {
                public void run() {
                    log.error("", moviePlayer.getError());
                    readyTask.cancel(true);
                    dispose();
                }
            });

            //our onReady handler
            moviePlayer.setOnReady(new Runnable() {
                @Override
                public synchronized void run() {
//                    movieDuration = moviePlayer.getTotalDuration();  
		    
		    moviePanel.setPreferredSize(new Dimension(moviePlayer.getMedia().getWidth(), moviePlayer.getMedia().getHeight()));
		    
		    //set the ready flag by releasing the ready task
		    readyTask.setValue(true);
		    
		    //send the task off to be executed
		    JFXRunTask.getPool().execute(readyTask);
                  }
            });
            
            BorderPane mvPane = new BorderPane();
            mvPane.getChildren().add(movieView);
            moviePanel.setScene(new Scene(mvPane));
	    
	    //set some initial values
	    currentTime.set((long)moviePlayer.getCurrentTime().toMillis());
	    currentVolume.set(Double.doubleToLongBits(moviePlayer.getVolume()));
	    duration.set((long)moviePlayer.getTotalDuration().toMillis());
	    
	    //create a listener to set our currentTime
	    moviePlayer.currentTimeProperty().addListener(new InvalidationListener() {
		@Override
		public void invalidated(Observable o) {
		    currentTime.set((long)moviePlayer.getCurrentTime().toMillis());
		}
	    });

	    //create a listener to set our currentVolume
	    moviePlayer.volumeProperty().addListener(new InvalidationListener() {
		@Override
		public void invalidated(Observable o) {
		    currentVolume.set(Double.doubleToLongBits(moviePlayer.getVolume()));
		}
	    });
	    
	    //create a listener to set our duration
	    moviePlayer.totalDurationProperty().addListener(new InvalidationListener() {
		@Override
		public void invalidated(Observable o) {
		    duration.set((long)moviePlayer.getTotalDuration().toMillis());
		}
	    });

        } catch (Exception e) {
            throw new MediaRuntimeException(e);
        }
        
        moviePlayer.statusProperty().addListener(
                new InvalidationListener() {
                    @Override
                    public void invalidated(Observable o) {
                        status.set(moviePlayer.getStatus());
                    }
                });
    }
    
    @Override
    public void setVolume(float volume) {
	final float newVolume = volume;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                moviePlayer.setVolume(newVolume);
            }
        });
    }

    @Override
    public float getVolume() {
//	return JFXRunTask.get(new Callable<Float>() {
//
//	    @Override public Float call() throws Exception {
//		return (float)moviePlayer.getVolume();
//	    }  
//	});	
	
	return (float)Double.longBitsToDouble(currentVolume.get());
    }

    @Override
    public void setTime(long time) {
	final Duration seekTime = new Duration(time);
	
	Platform.runLater(new Runnable() {
	    @Override
	    public void run() {
		moviePlayer.seek(seekTime);
	    }
	});
        
    }
    
    @Override
    public long getTime() {      
//	return JFXRunTask.get(new Callable<Long>() {
//
//	    @Override public Long call() throws Exception {
//		return (long)moviePlayer.getCurrentTime().toMillis();
//	    }  
//	});	    

	return currentTime.get();
    }

    @Override
    public long getDuration() {
//	return JFXRunTask.get(new Callable<Long>() {
//
//	    @Override public Long call() throws Exception {
//		return (long)moviePlayer.getTotalDuration().toMillis();
//	    }  
//	});	    
	return duration.get();
    }

    @Override
    public void start() {
	Platform.runLater(new Runnable() {
	    @Override
	    public void run() {
		moviePlayer.play();
	    }
	});	
    }

    @Override
    public void stop() {
	Platform.runLater(new Runnable() {
	    @Override
	    public void run() {
		moviePlayer.pause();
	    }
	});	  
    }

    @Override
    public boolean isPlaying() {
        return status.get() == Status.PLAYING;
    }
    
    @Override
    public Component getComponent() {
        return moviePanel;
    }

    @Override
    public void dispose() {
	
	if (movieView != null) {
	     Platform.runLater(new Runnable() {
		 @Override
		 public void run() {
		    try {
			    //This will ensure that we have cleaned up any media resources
			    if (moviePlayer != null) {
				moviePlayer.stop();
				moviePlayer.dispose();
			    }

			    movieView.setMediaPlayer(null);

			    movieView = null;

		    } catch(Exception e) {
			Logger.getLogger(JFXVideo.class).debug(e);
			throw new MediaRuntimeException(e);
		    }
		  }
	   });
	 }
    }            
            
}
