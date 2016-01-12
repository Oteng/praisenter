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
package org.praisenter.media;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

/**
 * Interface for performing filtering on media before being added to a {@link MediaLibrary}.
 * @author William Bittle
 * @version 3.0.0
 */
public interface MediaImportFilter {
	/**
	 * Returns the path to the imported media.
	 * <p>
	 * This method only creates the path, no other file operation is performed.
	 * <p>
	 * This method may return a path with a file name differing from the name supplied
	 * to conform to the filter's desired output.
	 * @param location the media library location
	 * @param name the file name of the media
	 * @param type the media type
	 * @return Path
	 */
	public Path getTarget(Path location, String name, MediaType type);
	
	/**
	 * Performs the filtering operation on the source media to the target.
	 * @param source the source media file
	 * @param target the target media file location and name
	 * @param type the media type
	 * @throws TranscodeException if the media failed to transcode into the filter's intended format
	 * @throws FileAlreadyExistsException if the target media file name already exists
	 * @throws IOException if an IO error occurs
	 */
	public void filter(Path source, Path target, MediaType type) throws TranscodeException, FileAlreadyExistsException, IOException;
}
