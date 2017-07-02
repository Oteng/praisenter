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
package org.praisenter.javafx;

import java.io.Serializable;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Represents a generic event from ApplicationPanes.
 * @author William Bittle
 * @version 3.0.0
 */
public class ApplicationPaneEvent extends Event implements Serializable {
	/** The serialization id */
	private static final long serialVersionUID = -2602432492272796559L;

	/** Event type for the state of the application pane changing */
	public static final EventType<ApplicationPaneEvent> STATE_CHANGED = new EventType<ApplicationPaneEvent>("APPLICATION_PANE_STATE_CHANGED");
	
	/** The selected item changed reason */
	public static final String REASON_SELECTION_CHANGED = "Selection changed";
	
	/** Data was copied or cut reason */
	public static final String REASON_DATA_COPIED = "Data copied or cut";
	
	/** An undo/redo was performed reason */
	public static final String REASON_UNDO_REDO_STATE_CHANGED = "Undo/redo state changed";
	
	/** The application pane */
	private final ApplicationPane pane;
	
	/** The reason */
	private final String reason;
	
	/**
	 * Full constructor.
	 * @param source the event source
	 * @param target the event target
	 * @param type the event type
	 * @param pane the application pane
	 * @param reason the event reason
	 */
	public ApplicationPaneEvent(Object source, EventTarget target, EventType<? extends ApplicationPaneEvent> type, ApplicationPane pane, String reason) {
		super(source, target, type);
		this.pane = pane;
		this.reason = reason;
	}
	
	/**
	 * Returns the application pane that emitted this event.
	 * @return {@link ApplicationPane}
	 */
	public ApplicationPane getApplicationPane() {
		return this.pane;
	}
	
	/**
	 * Returns the event reason.
	 * @return String
	 */
	public String getReason() {
		return this.reason;
	}
}
