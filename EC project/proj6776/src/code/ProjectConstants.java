package code;

import java.io.*;
import java.net.URL;

/* This class contains a group of project constants.
 * Use import static tetris.code.ProjectConstants.
 */
class ProjectConstants
{
	/* Should the application start fullscreened ? */
	static final boolean STARTFS = false;
	
	/* Yes this adds leading zeroes. */
	static String addLeadingZeroes(int n, int zeroes)
	{
		if(zeroes > 10)throw new IllegalArgumentException();
		StringBuilder ret = new StringBuilder(Integer.toString(n));
		while(ret.length() < zeroes)
		{
			ret.insert(0, "0");
		}
		return ret.toString();
	}
	
	/* Sleeps the current thread. */
	static void sleep_(int n){
		try{
			Thread.sleep(n);
		}
		catch(Throwable t){
			// Might throw a ThreadDeath if we're sleeping while we terminate the thread
			// but we're just going to ignore it.
		}
	}

	/* Returns a resource as an InputStream. First it
	 * tries to create a FileInputStream from the parent
	 * directory (if contents are unzipped) and then tries
	 * to use getResourceAsStream if that fails.*/
	static InputStream getResStream() {
		try{
			File f = new File("."+ "/sound/tetris.midi");
			return new FileInputStream(f.getCanonicalFile());
		}catch(Exception ea)
		{
			try{
			return ProjectConstants.class.getResourceAsStream("/sound/tetris.midi");
			}catch(Exception ex){ex.printStackTrace();}
		}
		throw new RuntimeException("Filestream: " + "/sound/tetris.midi" + " not found.");
		
	}
	
	/* Returns a resource as a URL object, for certain file
	 * parsing. Should accomodate Eclipse and other clients/IDEs
	 * as well. Currently it loads resources from Eclipse, the
	 * jar file, and from Tortoise.*/
	@SuppressWarnings("deprecation")
	static URL getResURL(String path) {
		try{
			File f = new File("."+path);
			if(!f.exists())throw new Exception();
			
			return f.getCanonicalFile().toURL();
		}catch(Exception e)
		{
			//eclipse workaround.
			try{
			return ProjectConstants.class.getResource(path);
			}catch(Exception ex){ex.printStackTrace();}
		}
		throw new RuntimeException("File: " + path + " not found.");
		
	}
	
	/* In case of errors, call this. */
	static String formatStackTrace(StackTraceElement[] e)
	{
		StringBuilder ret = new StringBuilder();
		for(StackTraceElement el: e)
		{
			ret.append("[");
			ret.append(el.getFileName()==null?
					"Unknown source":el.getFileName());
			ret.append(":");
			ret.append(el.getMethodName());
			ret.append(":");
			ret.append(el.getLineNumber());
			ret.append("]\n");
		}
		return ret.toString();
	}

	/*Enum representation of the current game's state*/
	//Moving this here lol.
	public enum GameState
	{
		PLAYING,
		PAUSED,
		GAMEOVER,
		BUSY
	}
}
