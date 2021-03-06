/*
 * Copyright (c) 2011-2013 William Bittle  http://www.praisenter.org/
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
package org.praisenter.data.song;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.praisenter.common.xml.XmlIO;
import org.praisenter.data.DataException;
import org.praisenter.data.DataImportException;
import org.xml.sax.SAXException;

/**
 * Song list importer for song database files.
 * @author William Bittle
 * @version 2.0.1
 * @since 1.0.0
 */
public final class SongImporter {
	/** The class level logger */
	private static final Logger LOGGER = Logger.getLogger(SongImporter.class);
	
	/** Hidden default constructor */
	private SongImporter() {}
	
	/**
	 * Imports the songs contained in the given file.
	 * @param file the file
	 * @param format the file format
	 * @throws DataImportException if an error occurs during import
	 */
	public static final void importSongs(File file, SongFormat format) throws DataImportException {
		if (format == SongFormat.CHURCHVIEW) {
			importChurchViewSongs(file);
		} else if (format == SongFormat.PRAISENTER) {
			importPraisenterSongs(file);
		} else if (format == SongFormat.OPENLYRICS) {
			importOpenLyricsSongs(file);
		} else {
			throw new DataImportException("SongFormat [" + format + "] is not supported for import.");
		}
	}
	
	/**
	 * Imports the songs contained in the given file.
	 * @param file the file to import
	 * @throws DataImportException if an error occurs during import
	 */
	private static final void importPraisenterSongs(File file) throws DataImportException {
		LOGGER.debug("Reading Praisenter song file: " + file.getName());
		List<Song> songs = null;
		// load the songs xml file into objects
		try {
			// try loading it up using JAXB (2.0.0)
			// later down the road we may need to check the version before trying this
			SongList list = XmlIO.read(file.getAbsolutePath(), SongList.class);
			if (list != null) {
				songs = list.getSongs();
			}
		} catch (FileNotFoundException e) {
			throw new DataImportException(e);
		} catch (JAXBException e) {
			LOGGER.warn("The song file is not in the expected format. Trying v1.0.0 format.", e);
			// this is possible if we attempt to load up an older version (1.0.0 for example)
			// we need to try to use the 1.0.0 song reader
			try {
				songs = PraisenterSongReaderv1_0_0.fromXml(file);
			} catch (ParserConfigurationException e1) {
				throw new DataImportException(e1);
			} catch (SAXException e1) {
				throw new DataImportException(e1);
			} catch (IOException e1) {
				throw new DataImportException(e1);
			}
		} catch (IOException e) {
			throw new DataImportException(e);
		}
		LOGGER.debug("Praisenter song file read successfully: " + file.getName());
		
		try {
			// save the song
			Songs.saveSongs(songs);
		} catch (DataException e) {
			// just throw this error
			throw new DataImportException(e);
		}
		
		LOGGER.debug("Praisenter song file imported successfully: " + file.getName());
	}
	
	/**
	 * Imports the songs contained in the given file.
	 * @param file the file to import
	 * @throws DataImportException if an error occurs during import
	 */
	private static final void importChurchViewSongs(File file) throws DataImportException {
		try {
			LOGGER.debug("Reading ChurchView song file: " + file.getName());
			// load the songs xml file into objects
			List<Song> songs = ChurchViewSongReader.fromXml(file);
			LOGGER.debug("ChurchView song file read successfully: " + file.getName());
			
			// insert the songs into the database
			try {
				// save the song
				Songs.saveSongs(songs);
			} catch (DataException e) {
				// just throw this error
				throw new DataImportException(e);
			}
			
			LOGGER.debug("ChurchView song file imported successfully: " + file.getName());
		} catch (ParserConfigurationException e) {
			throw new DataImportException(e);
		} catch (SAXException e) {
			throw new DataImportException(e);
		} catch (IOException e) {
			throw new DataImportException(e);
		}
	}
	
	/**
	 * Imports the songs contained in the given file.
	 * @param file the file to import
	 * @throws DataImportException if an error occurs during import
	 */
	private static final void importOpenLyricsSongs(File file) throws DataImportException {
		try {
			LOGGER.debug("Reading OpenLyrics song file: " + file.getName());
			// load the songs xml file into objects
			List<Song> songs = OpenLyricsSongReader.fromXml(file);
			LOGGER.debug("OpenLyrics song file read successfully: " + file.getName());
			
			// insert the songs into the database
			try {
				// save the song
				Songs.saveSongs(songs);
			} catch (DataException e) {
				// just throw this error
				throw new DataImportException(e);
			}
			
			LOGGER.debug("OpenLyrics song file imported successfully: " + file.getName());
		} catch (ParserConfigurationException e) {
			throw new DataImportException(e);
		} catch (SAXException e) {
			throw new DataImportException(e);
		} catch (IOException e) {
			throw new DataImportException(e);
		}
	}
}
