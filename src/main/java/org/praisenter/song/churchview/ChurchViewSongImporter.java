/*
 * Copyright (c) 2015-2016 William Bittle  http://www.praisenter.org/
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted 
 * provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions 
 *     and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 *     and the following disclaimer in the documentation and/or other materials provided with the 
 *     distribution.
 *   * Neither the name of Praisenter nor the names of its contributors may be used to endorse or 
 *     promote products derived from this software without specific prior written permission.
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.praisenter.song.churchview;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.praisenter.song.Br;
import org.praisenter.song.Lyrics;
import org.praisenter.song.Song;
import org.praisenter.song.SongImportException;
import org.praisenter.song.SongImporter;
import org.praisenter.song.TextFragment;
import org.praisenter.song.Verse;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX XML reader for the ChurchView program's song format.
 * @author William Bittle
 * @version 3.0.0
 */
public final class ChurchViewSongImporter extends DefaultHandler implements SongImporter {
	/** The class-level logger */
	private static final Logger LOGGER = LogManager.getLogger();
	
	/* (non-Javadoc)
	 * @see org.praisenter.data.song.SongFormatReader#read(java.nio.file.Path)
	 */
	@Override
	public List<Song> read(Path path) throws IOException, SongImportException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			
			parser.parse(new InputSource(new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile())))), this);
			
			return this.songs;
		} catch (SAXException | ParserConfigurationException e) {
			throw new SongImportException(e.getMessage(), e);
		}
	}
	
	// SAX parser implementation

	/** Regular expression pattern used to parse the song part name */
	private static final Pattern SONG_PART_PATTERN = Pattern.compile("(.*)(\\d+)", Pattern.CASE_INSENSITIVE);
	
	/** Regular expression pattern used to parse the song part font size */
	private static final Pattern SONG_PART_SIZE_PATTERN = Pattern.compile("([CVBTE])(\\d+)?Size", Pattern.CASE_INSENSITIVE);
	
	/** Scale factor for the font size */
	private static final double FONT_SIZE_SCALE = 1.5;
	
	/** The songs */
	private List<Song> songs;
	
	/** The song currently being processed */
	private Song song;
	
	/** The lyrics currently being processed */
	private Lyrics lyrics;
	
	/** Buffer for tag contents */
	private StringBuilder dataBuilder;
	
	/**
	 * Default constructor.
	 */
	public ChurchViewSongImporter() {
		this.songs = new ArrayList<Song>();
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// inspect the tag name
		if (qName.equalsIgnoreCase("Songs")) {
			// when we see the <Songs> tag we create a new song
			this.song = new Song();
			this.lyrics = new Lyrics();
			this.song.setPrimaryLyrics(this.lyrics.getId());
			this.song.getLyrics().add(this.lyrics);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// this method can be called a number of times for the contents of a tag
		// this is done to improve performance so we need to append the text before
		// using it
		String s = new String(ch, start, length);
		if (this.dataBuilder == null) {
			this.dataBuilder = new StringBuilder();
		}
		this.dataBuilder.append(s);
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("Songs".equalsIgnoreCase(qName)) {
			// we are done with the song so add it to the list
			this.songs.add(this.song);
			this.song = null;
			this.lyrics = null;
		} else if ("SongTitle".equalsIgnoreCase(qName)) {
			// make sure the tag was not self terminating
			if (this.dataBuilder != null) {
				// set the song title
				this.lyrics.setTitle(StringEscapeUtils.unescapeXml(this.dataBuilder.toString().trim()));
			}
		} else if ("Song".equalsIgnoreCase(qName) ||
				"Bridge".equalsIgnoreCase(qName) ||
				qName.startsWith("Verse") ||
				qName.startsWith("Chorus") ||
				"Tag".equalsIgnoreCase(qName) ||
				"Ending".equalsIgnoreCase(qName) ||
				"Vamp".equalsIgnoreCase(qName)) {
			// make sure the tag was not self terminating
			if (this.dataBuilder != null) {
				// get the text
				String text = this.dataBuilder.toString().trim();
				// only add the part if its not empty
				if (!text.isEmpty()) {
					Verse verse = new Verse();
					
					// set the type
					String type = getType(qName);
					int number = 1;
					
					// set the number
					if (qName.startsWith("Verse") || qName.startsWith("Chorus")) {
						Matcher matcher = SONG_PART_PATTERN.matcher(qName);
						if (matcher.matches()) {
							String n = matcher.group(2);
							try {
								number = Integer.parseInt(n);
							} catch (NumberFormatException e) {
								LOGGER.warn("Failed to read verse part number: {}", n);
							}
						} else {
							LOGGER.warn("Failed to read verse part number from: {}", qName);
						}
					}
					
					verse.setName(type, number, null);
					
					// set the text
					String[] lines = text.split("(\\r|\\r\\n|\\n\\r|\\n)");
					for (int i = 0; i < lines.length; i++) {
						if (i != 0) {
							verse.getFragments().add(new Br());
						}
						TextFragment txt = new TextFragment();
						txt.setText(lines[i]);
						verse.getFragments().add(txt);
					}
					
					this.lyrics.getVerses().add(verse);
				}
			}
		} else if ("cDate".equalsIgnoreCase(qName)) {
			// ignore, use today's date
		} else if ("NOTES".equalsIgnoreCase(qName)) {
			// make sure the tag was not self terminating
			if (this.dataBuilder != null) {
				String data = this.dataBuilder.toString().trim();
				this.song.setComments(data);
			}
		} else if (qName.contains("Size")) {
			// make sure the tag was not self terminating
			if (this.dataBuilder != null) {
				Verse verse = this.getVerseForSize(qName);
				if (verse != null) {
					// interpret the size
					String s = this.dataBuilder.toString().trim();
					try {
						int size = (int)Math.floor(Integer.parseInt(s) * FONT_SIZE_SCALE);
						verse.setFontSize(size);
					} catch (NumberFormatException e) {
						LOGGER.warn("Failed to read verse font size: {}", s);
					}
				}
			}
		}
		
		this.dataBuilder = null;
	}
	
	/**
	 * Returns the type of verse based on the tag name.
	 * @param name the tag name
	 * @return String
	 */
	private static final String getType(String name) {
		if (name.startsWith("Verse")) {
			return "v";
		} else if (name.startsWith("Chorus") || name.equalsIgnoreCase("Song")) {
			return "c";
		} else if (name.equalsIgnoreCase("Bridge")) {
			return "b";
		} else if (name.equalsIgnoreCase("Ending")) {
			return "e";
		} else if (name.equalsIgnoreCase("Tag")) {
			return "t";
		} else if (name.equalsIgnoreCase("Vamp")) {
			return "e";
		} else {
			return "o";
		}
	}
	
	/**
	 * Returns the verse for the given font size tag name.
	 * @param name the font size tag name
	 * @return List&lt;{@link Verse}&gt;
	 */
	private final Verse getVerseForSize(String name) {
		if ("FontSize".equalsIgnoreCase(name)) {
			return null;
		}
		Matcher matcher = SONG_PART_SIZE_PATTERN.matcher(name);
		if (matcher.matches()) {
			String t = matcher.group(1);
			String i = matcher.group(2);
			if (i == null) {
				// then its a vamp, bridge, tag or end size
				if ("V".equalsIgnoreCase(t)) {
					return this.lyrics.getVerse("v1");
				} else if ("B".equalsIgnoreCase(t)) {
					return this.lyrics.getVerse("b1");
				} else if ("T".equalsIgnoreCase(t)) {
					return this.lyrics.getVerse("t1");
				} else if ("E".equalsIgnoreCase(t)) {
					return this.lyrics.getVerse("e1");
				} else {
					return null;
				}
			} else {
				int index = 0;
				try {
					index = Integer.parseInt(i);
				} catch (NumberFormatException e) {
					LOGGER.warn("Failed to read verse part number: {}", i);
					return null;
				}
				// then its a chorus or verse tag
				if ("C".equalsIgnoreCase(t)) {
					// chorus
					return this.lyrics.getVerse("c" + index);
				} else if ("V".equalsIgnoreCase(t)) {
					// verse
					return this.lyrics.getVerse("v" + index);
				} else {
					return null;
				}
			}
		}
		
		return null;
	}
}
