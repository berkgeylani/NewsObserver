package com.galaksiya.newsobserver.master;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
/**
 * It helps to read a file with given path.
 * @author francium
 *
 */
public class FileParser {

	private static final Logger LOG = Logger.getLogger(FileParser.class);
	private BlockingQueue<URL> rssLinksBlockingQueue;
/**
 * It sets file path with it's arguman.
 * @param filePath A path which will be read.
 */
	public FileParser(String filePath) {
		rssLinksBlockingQueue = new LinkedBlockingQueue<>();
		readFile(filePath);
	}
	/**
	 * If you call after readerOfFile function,it returns arraylist which occurs urls.
	 * @return  It returns an arraylist which occurs url's(rss links).
	 */
	public BlockingQueue<URL> getRssLinks() {// Return URLs of rss links
		return rssLinksBlockingQueue;
	}
	
	
	
	/**
	 * It takes file path and handle the file which is in given file path then push all the urls
	 * to an arraylist.
	 * @param filePath will be read file's path
	 * @return 0:fault 1:success
	 */
	public boolean readFile(String filePath) {
		try {
			Paths.get(filePath);
		}catch(InvalidPathException | NullPointerException exception){
			LOG.error("Given pathway is null or invalid.",exception);
			return false;//path yoksa 0
		}
		try (BufferedReader br = new BufferedReader(new FileReader(filePath));) {// ANSWER : http://stackoverflow.com/questions/17650970/am-i-using-the-java-7-try-with-resources-correctly
			String rssLink;
			while ((rssLink = br.readLine()) != null) {
				
				// printing out each line in the file
				URL url=null;
				try{
					 url= new URL(rssLink);
				}
				catch (MalformedURLException e) {
					LOG.error("In file(.txt),One of links isn't a Url --> "+url,e);
					return false;
				} 
				rssLinksBlockingQueue.put(url);
			}
			return true; // herşey okeyse return true
		} catch (FileNotFoundException e) {
			LOG.error("File Not Found In Given Path",e);
		} catch (IOException e) {
			LOG.error("Input or output problem",e);
		} catch (InterruptedException e) {
			LOG.error("Problem while adding url to blocking queue.",e);
		}
		return false; 
	}

}