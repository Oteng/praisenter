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
package org.praisenter.slide;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.praisenter.Constants;
import org.praisenter.ReadonlyIterator;
import org.praisenter.Tag;
import org.praisenter.bible.ReferenceSet;
import org.praisenter.slide.animation.SlideAnimation;
import org.praisenter.slide.text.BasicTextComponent;
import org.praisenter.slide.text.CountdownComponent;
import org.praisenter.slide.text.DateTimeComponent;
import org.praisenter.slide.text.PlaceholderType;
import org.praisenter.slide.text.PlaceholderVariant;
import org.praisenter.slide.text.TextPlaceholderComponent;
import org.praisenter.xml.adapters.BufferedImageTypeAdapter;

/**
 * Implementation of the {@link Slide} interface.
 * @author William Bittle
 * @version 3.0.0
 */
@XmlRootElement(name = "slide")
@XmlAccessorType(XmlAccessType.NONE)
public class BasicSlide extends AbstractSlideRegion implements Slide, SlideRegion, Comparable<Slide> {
	/** The format (for format identification only) */
	@XmlAttribute(name = "format", required = false)
	final String format = Constants.FORMAT_NAME;
	
	/** The slide format version */
	@XmlAttribute(name = "version", required = false)
	final String version = Slide.CURRENT_VERSION;

	// for internal use really
	/** The slide path; can be null */
	Path path;
	
	/** The slide name */
	@XmlElement(name = "name")
	String name;
	
	/** The time the slide will show in milliseconds */
	@XmlElement(name = "time", required = false)
	long time;
	
	/** The slide components */
	@XmlElementRefs({
		@XmlElementRef(type = MediaComponent.class),
		@XmlElementRef(type = BasicTextComponent.class),
		@XmlElementRef(type = DateTimeComponent.class),
		@XmlElementRef(type = TextPlaceholderComponent.class),
		@XmlElementRef(type = CountdownComponent.class)
	})
	@XmlElementWrapper(name = "components", required = false)
	final List<SlideComponent> components;

	/** The slide animations */
	@XmlElement(name = "animation", required = false)
	@XmlElementWrapper(name = "animations", required = false)
	final List<SlideAnimation> animations;

	/** Any placeholder data */
	@XmlElement(name = "placeholderData", required = false)
	final Map<PlaceholderVariant, Object> placeholderData;
	
	/** The tags */
	@XmlElement(name = "tag", required = false)
	@XmlElementWrapper(name = "tags", required = false)
	final Set<Tag> tags;
	
	/** The thumbnail for this slide */
	@XmlElement(name = "thumbnail", required = false)
	@XmlJavaTypeAdapter(BufferedImageTypeAdapter.class)
	BufferedImage thumbnail;
	
	/**
	 * Default constructor.
	 */
	public BasicSlide() {
		// JAXB should overwrite the id
		this(UUID.randomUUID());
	}
	
	/**
	 * Constructor for setting an explicit id.
	 * @param id the id
	 */
	public BasicSlide(UUID id) {
		super(id);
		this.components = new ArrayList<SlideComponent>();
		this.animations = new ArrayList<SlideAnimation>();
		this.placeholderData = new HashMap<PlaceholderVariant, Object>();
		this.time = Slide.TIME_FOREVER;
		this.tags = new TreeSet<Tag>();
	}
	
	/**
	 * Copy constructor.
	 * @param other the component to copy
	 * @param exact whether to copy the component exactly
	 */
	public BasicSlide(BasicSlide other, boolean exact) {
		// copy over the super class stuff
		super(other, exact);
		
		this.components = new ArrayList<SlideComponent>();
		this.animations = new ArrayList<SlideAnimation>();
		this.placeholderData = new HashMap<PlaceholderVariant, Object>(other.placeholderData);
		this.tags = new TreeSet<Tag>();
		
		if (exact) {
			this.path = other.path;
		}
		
		this.time = other.time;
		this.name = other.name;
		
		// copy the components
		for (int i = 0; i < other.components.size(); i++) {
			SlideComponent sc = other.components.get(i);
			SlideComponent copy = sc.copy(exact);
			this.components.add(copy);
			
			// component animations
			List<SlideAnimation> animations = other.getAnimations(sc.getId());
			for(SlideAnimation animation : animations) {
				this.animations.add(animation.copy(copy.getId()));
			}
		}
		
		// slide animations
		List<SlideAnimation> animations = other.getAnimations(other.getId());
		for(SlideAnimation animation : animations) {
			this.animations.add(animation.copy(this.id));
		}
		
		// tags
		this.tags.addAll(other.tags);
		
		// thumbnail
		this.thumbnail = other.thumbnail;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.SlideRegion#copy()
	 */
	@Override
	public BasicSlide copy() {
		return this.copy(false);
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.SlideRegion#copy(boolean)
	 */
	@Override
	public BasicSlide copy(boolean exact) {
		return new BasicSlide(this, exact);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Slide other) {
		if (other == null) {
			return -1;
		} else if (other.getName() == null) {
			return -1;
		} else if (this.name == null) {
			return 1;
		} else {
			return this.name.compareTo(other.getName());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getPath()
	 */
	@Override
	public Path getPath() {
		return this.path;
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getVersion()
	 */
	@Override
	public String getVersion() {
		return this.version;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getTime()
	 */
	@Override
	public long getTime() {
		return time;
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#setTime(long)
	 */
	@Override
	public void setTime(long time) {
		this.time = time;
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#addComponent(org.praisenter.slide.SlideComponent)
	 */
	@Override
	public void addComponent(SlideComponent component) {
		this.components.add(component);
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#removeComponent(org.praisenter.slide.SlideComponent)
	 */
	@Override
	public boolean removeComponent(SlideComponent component) {
		// no re-sort required here
		if (this.components.remove(component)) {
			this.animations.removeIf(st -> st.getId().equals(component.getId()));
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getComponentIterator()
	 */
	@Override
	public Iterator<SlideComponent> getComponentIterator() {
		return new ReadonlyIterator<SlideComponent>(this.components.iterator());
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getComponents(java.lang.Class)
	 */
	@Override
	public <E extends SlideComponent> List<E> getComponents(Class<E> clazz) {
		List<E> components = new ArrayList<E>();
		for (SlideComponent component : this.components) {
			if (clazz.isInstance(component)) {
				components.add(clazz.cast(component));
			}
		}
		return components;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#moveComponentUp(org.praisenter.slide.SlideComponent)
	 */
	@Override
	public void moveComponentUp(SlideComponent component) {
		// move the given component up in the order
		
		// get all the components
		List<SlideComponent> components = this.components;
		// verify the component exists on this slide
		if (components.contains(component) && components.size() > 0) {
			int size = components.size();
			int index = components.indexOf(component);
			// see if the component is already in the last position
			if (components.get(size - 1).equals(component)) {
				// if it is, then just return
				return;
			} else {
				Collections.swap(components, index, index + 1);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#moveComponentDown(org.praisenter.slide.SlideComponent)
	 */
	@Override
	public void moveComponentDown(SlideComponent component) {
		// move the given component down in the order
		
		// get all the components
		List<SlideComponent> components = this.components;
		// verify the component exists on this slide
		if (components.contains(component) && components.size() > 0) {
			int index = components.indexOf(component);
			// see if the component is already in the first position
			if (components.get(0).equals(component)) {
				// if it is, then just return its order
				return;
			} else {
				Collections.swap(components, index, index - 1);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#moveComponentFront(org.praisenter.slide.SlideComponent)
	 */
	@Override
	public void moveComponentFront(SlideComponent component) {
		// move the given component up in the order
		
		// get all the components
		List<SlideComponent> components = this.components;
		// verify the component exists on this slide
		if (components.contains(component) && components.size() > 0) {
			int size = components.size();
			// see if the component is already in the last position
			if (components.get(size - 1).equals(component)) {
				// if it is, then just return its order
				return;
			} else {
				components.remove(component);
				components.add(component);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#moveComponentBack(org.praisenter.slide.SlideComponent)
	 */
	@Override
	public void moveComponentBack(SlideComponent component) {
		// move the given component up in the order
		
		// get all the components
		List<SlideComponent> components = this.components;
		// verify the component exists on this slide
		if (components.contains(component) && components.size() > 0) {
			// see if the component is already in the last position
			if (components.get(0).equals(component)) {
				// if it is, then just return its order
				return;
			} else {
				components.remove(component);
				components.add(0, component);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#hasPlaceholders()
	 */
	@Override
	public boolean hasPlaceholders() {
		for (SlideComponent component : this.components) {
			if (TextPlaceholderComponent.class.isInstance(component)) {
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#fit(int, int)
	 */
	@Override
	public void fit(int width, int height) {
		// compute the resize percentages
		double pw = (double)width / (double)this.width;
		double ph = (double)height / (double)this.height;
		// set the slide size
		this.width = width;
		this.height = height;
		// set the sizes for the components
		for (SlideComponent component : this.components) {
			component.adjust(pw, ph);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getAnimations()
	 */
	@Override
	public List<SlideAnimation> getAnimations() {
		return this.animations;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getAnimations(java.util.UUID)
	 */
	@Override
	public List<SlideAnimation> getAnimations(UUID id) {
		List<SlideAnimation> animations = new ArrayList<SlideAnimation>();
		for (int i = 0; i < this.animations.size(); i++) {
			SlideAnimation st = this.animations.get(i);
			if (st.getId().equals(id)) {
				animations.add(st);
			}
		}
		return animations;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getTags()
	 */
	@Override
	public Set<Tag> getTags() {
		return this.tags;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#getThumbnail()
	 */
	@Override
	public BufferedImage getThumbnail() {
		return this.thumbnail;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.Slide#setThumbnail(java.awt.image.BufferedImage)
	 */
	@Override
	public void setThumbnail(BufferedImage thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	@Override
	public Object getPlaceholderData(PlaceholderVariant variant) {
		return this.placeholderData.get(variant);
	}
	
	@Override
	public Object setPlaceholderData(PlaceholderVariant variant, Object data) {
		Object value = this.placeholderData.put(variant, data);
		this.updatePlaceholders();
		return value;
	}
	
	@Override
	public Object removePlaceholderData(PlaceholderVariant variant) {
		Object value = this.placeholderData.remove(variant);
		if (value != null) {
			this.updatePlaceholders();
		}
		return value;
	}
	
	protected void updatePlaceholders() {
		// iterate all the placeholders
		for (TextPlaceholderComponent tpc : this.getComponents(TextPlaceholderComponent.class)) {
			String text = null;
			// determine the placeholder type and get the text for that placeholder
			// for all it's configured variants
			if (tpc.getPlaceholderType() == PlaceholderType.TITLE) {
				text = this.getTitleText(tpc.getPlaceholderVariants());
			} else if (tpc.getPlaceholderType() == PlaceholderType.TEXT) {
				text = this.getText(tpc.getPlaceholderVariants());				
			}
			tpc.setText(text);
		}
	}
	
	private String getTitleText(Set<PlaceholderVariant> variants) {
		if (this.placeholderData == null) {
			return null;
		}
		List<String> items = new ArrayList<String>();
		for (PlaceholderVariant variant : variants) {
			Object data = this.placeholderData.get(variant);
			if (data instanceof ReferenceSet) {
				items.add(((ReferenceSet)data).getReference());
			}
		}
		return String.join(Constants.NEW_LINE, items);
	}
	
	private String getText(Set<PlaceholderVariant> variants) {
		if (this.placeholderData == null) {
			return null;
		}
		List<String> items = new ArrayList<String>();
		for (PlaceholderVariant variant : variants) {
			Object data = this.placeholderData.get(variant);
			if (data instanceof ReferenceSet) {
				items.add(((ReferenceSet)data).getText());
			}
		}
		return String.join(Constants.NEW_LINE, items);
	}
}
