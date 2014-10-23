/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.korsakow.ide.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author phoenix
 */
public class JFXRunTask {

    public static class SimpleGetterTask<U> extends Task<U>{
	private U value = null;

	public SimpleGetterTask() {
	}
	
	public SimpleGetterTask(U value) {
	    //sets a default value
	    this.value = value;
	}

	@Override
	public U call() throws Exception {
	    return value;
	}
	
	public void setValue(U value) {
	    this.value = value;
	}
    }

    private static ExecutorService pool;
    
    public static ExecutorService getPool() {
	if (pool == null) {
	    pool = Executors.newCachedThreadPool();
	}
	
	return pool;
    }
    
    /*	
    Returns the value T from Callable.  Simplifies the
    task of submitting and waiting for the response. Always
    submits the task to the JavaFX application thread.  This enables
    us to get values returned from helper classes (for instance,
    getting the value returned from MediaPlayer.getCurrentTime() )
    logs a message and returns null on error
    */
    //This version of get catches and reports exceptions internally, and 
    //does not propogate.
    public static <T> T get(final Callable<T> callable) {
	
	try {
	    if (Platform.isFxApplicationThread()) {
		return callable.call();
	    }
	    
	    final SimpleGetterTask<T> task = new SimpleGetterTask<>();

	    Platform.runLater(new Runnable() {
		@Override
		public void run() {
		    try {
			task.setValue(callable.call());

			getPool().execute(task);
		    } catch (Exception e) {
			LogFactory.getLog(JFXRunTask.class).info(e);
		    }	
		}
	    });

	    int count = 0;
	    while(true) {
		try {
		    count += 1;
		    return task.get(500L, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
		    //we just try again
		    LogFactory.getLog(JFXRunTask.class).info(e);
		    
		    if (count > 10)
			return null;
		}
	    }
	     
	} catch (Exception e) {
	    LogFactory.getLog(JFXRunTask.class).info(e);
	    
	    return null;
	}
    }
    
    public static void run(final Runnable runnable) {
	try {
	    if (Platform.isFxApplicationThread()) {
		runnable.run();
		return;
	    }
	    
	    Platform.runLater(runnable);
	    
	} catch (Exception e) {
	    LogFactory.getLog(JFXRunTask.class).info(e);	    
	}
    }
}
