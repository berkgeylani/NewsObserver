package com.galaksiya.newsObserver.master;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.crypto.Data;

import org.apache.log4j.Logger;

import com.galaksiya.newsObserver.database.Database;
import com.galaksiya.newsObserver.database.DatabaseFactory;
import com.galaksiya.newsObserver.parser.FeedMessage;
import com.galaksiya.newsObserver.parser.RssReader;

public class NewsChecker {

	private final static Logger LOG = Logger.getLogger(NewsChecker.class);

	private static  Hashtable<String, String> lastNews = new Hashtable<String, String>();//her newsCheckerda ortak olmasını istediğmiz bir field.
	
	private Database db;
	private DatabaseFactory databaseFactory;
	public NewsChecker(){
	}
	public NewsChecker(Database dbObject){
		db=dbObject;
	}
	public NewsChecker(DatabaseFactory databaseFactory){
		this.databaseFactory = databaseFactory;
	}
	/**
	 * It takes rss links and give one by one to travelInNews.
	 * @param RssLinksAL This arraylist is rss links list.
	 * @return true :success false :fail
	 */
	public boolean updateActualNews(ArrayList<URL> RssLinksAL) {
		if (RssLinksAL == null || RssLinksAL.isEmpty() )
			return false;

		for (URL rssURLs : RssLinksAL) { 
			if(!(lastNews.containsKey(rssURLs.toString())))
				lastNews.put(rssURLs.toString(), "");
			travelInNews( rssURLs);
			LOG.debug(rssURLs + " checked.");
		}
		return true;
	}

	/**
	 * It travel news in url which is given with param. It gives news one by one
	 * to handleMessage. Also this function save the last news.
	 * 
	 * @param lastNews
	 *            It is hashtable which occurs rssLink-lastNew for this link.
	 * @param rssURLs
	 *            This is the url which will be read.
	 */
	public void travelInNews(URL rssURLs) {
		String[] lastNewsArray = new String[2];
		boolean updateNew = true, updated = false;
		RssReader parserOfRss = new RssReader();
		ArrayList<FeedMessage> itemsAL = parserOfRss.parseFeed(rssURLs);
		if (itemsAL == null || itemsAL.isEmpty()) {
			LOG.error("There no news to handle.");
			return;
		}
		for (FeedMessage message : itemsAL) {
			boolean isThereAnyNewNews = !message.getTitle().equals(lastNews.get(rssURLs.toString()));
			if (isThereAnyNewNews) { // if there is a new news we should insert
				if (updateNew) {
					lastNewsArray[0] = rssURLs.toString();
					lastNewsArray[1] = message.getTitle();
					updateNew = false;
					updated = true;
				}
				handleMessage(message);
			} else {// if we come the lately new we can break
				break;
			}
		}
		if (updateNew == false && updated == true) {
			lastNews.put(lastNewsArray[0], lastNewsArray[1]);
		}
	}
	/**
	 * It takes message and handle it to date-word-frequency.Then,It increment(update) or insert it.
	 * @param message It is only one new with title-description-pubdate.
	 * @return wordFrequencyPerNew It is a hash table which occurs word-frequency
	 */
	public Hashtable<String, Integer> handleMessage(FeedMessage message) {
		WordProcessor processOfWords = new WordProcessor();
		databaseFactory =  DatabaseFactory.getInstance();
		DateUtils dateUtils = new DateUtils();
		Database dbForNews;
		if(databaseFactory.getDatabaseType()==null){
			databaseFactory.setDatabaseType("mongo");//for test
			dbForNews = databaseFactory.getDatabase("newsTest");
		}
		else {
			dbForNews = databaseFactory.getDatabase("news");
		}
		FeedMessage messageNews = processOfWords.cleanMessageForNews(message);
		dbForNews.saveNews(message);
		Hashtable<String, Integer> wordFrequencyPerNew = new Hashtable<String, Integer>();
		String datePerNew = dateUtils.dateCustomize(message.getpubDate());
		wordFrequencyPerNew = processOfWords.splitAndHashing(message.getTitle() + " " + message.getDescription());
		// wordFrequency test edecez
		if (!(travelWordByWord(datePerNew, wordFrequencyPerNew)))
			return null;
		return wordFrequencyPerNew;
	}
	/**
	 * It controls 'Is this new in rss link?'
	 * @param title A new of title
	 * @param rssURLs A URL to read.
	 * @return true :Success false :fail
	 */
	public boolean containNewsTitle(String title, URL rssURLs) {
		RssReader parserOfRss = new RssReader();
		for (FeedMessage message : parserOfRss.parseFeed(rssURLs)) { 
			if (message.getTitle().equals(title)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * It travel word by word and control the database has it already inserted.
	 * If yes,then increment it to database.
	 * If not,the insert it.
	 * @param datePerNew A hash table occurs message's pubdate.
	 * @param wordFrequencyPerNew It occurs from word-frequency.
	 * @return true :Success false :fail
	 */
	private boolean travelWordByWord(String datePerNew, Hashtable<String, Integer> wordFrequencyPerNew) {
		DbHelper dbHelper;
		if (db!=null) {
			dbHelper = new DbHelper(db);
		}
		dbHelper = new DbHelper();
		boolean proccessSuccessful = false;
		Enumeration<String> e = wordFrequencyPerNew.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();// key:key
													// wordFrequencyPerNew.get(key):value
			proccessSuccessful = dbHelper.addDatabase(datePerNew, key, wordFrequencyPerNew.get(key));
		}
		return proccessSuccessful;
	}
}
