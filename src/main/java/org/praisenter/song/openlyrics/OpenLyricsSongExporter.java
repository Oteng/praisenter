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
package org.praisenter.song.openlyrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.praisenter.song.Author;
import org.praisenter.song.Br;
import org.praisenter.song.Chord;
import org.praisenter.song.Comment;
import org.praisenter.song.Lyrics;
import org.praisenter.song.Song;
import org.praisenter.song.SongExportException;
import org.praisenter.song.SongExporter;
import org.praisenter.song.Songbook;
import org.praisenter.song.TextFragment;
import org.praisenter.song.Theme;
import org.praisenter.song.Title;
import org.praisenter.song.Verse;
import org.praisenter.song.VerseFragment;
import org.praisenter.xml.XmlIO;

/**
 * Class used to export a listing of songs in the current praisenter format.
 * @author William Bittle
 * @version 3.0.0
 */
public final class OpenLyricsSongExporter implements SongExporter {
	/* (non-Javadoc)
	 * @see org.praisenter.song.SongExporter#write(java.nio.file.Path, java.util.List)
	 */
	@Override
	public void write(Path path, List<Song> songs) throws IOException, SongExportException {
		for (Song song : songs) {
			Title ttl = song.getDefaultTitle();
			String variant = song.getVariant();
			
			// replace any non-alpha-numeric characters that might not be valid for a file name
			String name = ttl.getText().replaceAll("\\W+", "") + "_" + variant.replaceAll("\\W+", "");
			Path file = path.resolve(name + ".xml");
			
			// see if it exists
			if (Files.exists(file)) {
				// append a UUID to the name
				file = path.resolve(name + "_" + UUID.randomUUID().toString().replaceAll("-", "") + ".xml");
			}
			
			// convert the song to openlyrics format
			OpenLyricsSong olsong = new OpenLyricsSong();
			
			olsong.createdIn = song.getCreatedIn();
			olsong.modifiedDate = song.getLastModifiedDate();
			olsong.modifiedIn = song.getLastModifiedIn();
			olsong.properties.ccli = song.getCcli();
			olsong.properties.copyright = song.getCopyright();
			olsong.properties.key = song.getKey();
			olsong.properties.keywords = song.getKeywords();
			olsong.properties.publisher = song.getPublisher();
			olsong.properties.released = song.getReleased();
			olsong.properties.tempo = new OpenLyricsTempo();
			olsong.properties.tempo.text = song.getTempo();
			olsong.properties.transposition = song.getTransposition();
			olsong.properties.variant = song.getVariant();
			olsong.properties.verseOrder = song.getSequence();
			olsong.properties.version = song.getVersion();
			
			// titles
			for (Title title : song.getTitles()) {
				OpenLyricsTitle t = new OpenLyricsTitle();
				t.language = title.getLanguage();
				t.original = title.isOriginal();
				t.text = title.getText();
				t.transliteration = title.getTransliteration();
				olsong.properties.titles.add(t);
			}
			
			// themes
			for (Theme theme : song.getThemes()) {
				OpenLyricsTheme t = new OpenLyricsTheme();
				t.language = theme.getLanguage();
				t.text = theme.getText();
				t.transliteration = theme.getTransliteration();
				olsong.properties.themes.add(t);
			}
			
			// songbooks
			for (Songbook book : song.getSongbooks()) {
				OpenLyricsSongbook b = new OpenLyricsSongbook();
				b.entry = book.getEntry();
				b.name = book.getName();
				olsong.properties.songbooks.add(b);
			}
			
			// comments
			if (song.getComments() != null && song.getComments().length() > 0) {
				OpenLyricsComment comment = new OpenLyricsComment();
				comment.text = song.getComments();
				olsong.properties.comments.add(comment);
			}
			
			// comments
			for (Author author : song.getAuthors()) {
				OpenLyricsAuthor a = new OpenLyricsAuthor();
				a.language = author.getLanguage();
				a.name = author.getName();
				a.type = author.getType();
				olsong.properties.authors.add(a);
			}
			
			// verses
			for (Lyrics lyrics : song.getLyrics()) {
				for (Verse verse : lyrics.getVerses()) {
					OpenLyricsVerse v = new OpenLyricsVerse();
					
					v.language = lyrics.getLanguage();
					v.transliteration = lyrics.getTransliteration();
					
					v.name = verse.getName();
					
					OpenLyricsLine line = new OpenLyricsLine();
					for (VerseFragment fragment : verse.getFragments()) {
						if (fragment instanceof TextFragment) {
							line.elements.add(((TextFragment)fragment).getText());
						} else if (fragment instanceof Br) {
							line.elements.add(new OpenLyricsBr());
						} else if (fragment instanceof Chord) {
							OpenLyricsChord c = new OpenLyricsChord();
							c.name = ((Chord)fragment).getName();
							line.elements.add(c);
						} else if (fragment instanceof Comment) {
							OpenLyricsLineComment c = new OpenLyricsLineComment();
							c.text = ((Comment)fragment).getText();
							line.elements.add(c);
						}
					}
					v.lines.add(line);
					
					olsong.verses.add(v);
				}
			}
			
			try {
				XmlIO.save(file, olsong);
			} catch (JAXBException e) {
				throw new SongExportException(e);
			}
		}
	}
}