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
package org.praisenter.data.slide;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.praisenter.Constants;
import org.praisenter.data.Copyable;
import org.praisenter.data.Identifiable;
import org.praisenter.data.Persistable;
import org.praisenter.data.Tag;
import org.praisenter.data.TextItem;
import org.praisenter.data.TextStore;
import org.praisenter.data.TextType;
import org.praisenter.data.TextVariant;
import org.praisenter.data.bible.BibleReferenceTextStore;
import org.praisenter.data.json.InstantJsonDeserializer;
import org.praisenter.data.json.InstantJsonSerializer;
import org.praisenter.data.search.Indexable;
import org.praisenter.data.slide.animation.Animation;
import org.praisenter.data.slide.animation.AnimationType;
import org.praisenter.data.slide.animation.SlideAnimation;
import org.praisenter.data.slide.text.TextPlaceholderComponent;
import org.praisenter.song.SongReferenceTextStore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

/**
 * Implementation of the {@link Slide} interface.
 * @author William Bittle
 * @version 3.0.0
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY)
public final class Slide extends SlideRegion implements ReadonlySlide, ReadonlySlideRegion, Indexable, Persistable, Copyable, Identifiable {
	/** Value indicating a slide should show forever */
	public static final long TIME_FOREVER = -1;
	
	/** A default transition duration value */
	public static final long DEFAULT_TRANSITION_DURATION = 400;
	
	/** Value indicating the animation or easing id is not assigned */
	public static final int ID_NOT_SET = -1;
	
	public static final String DATA_TYPE_SLIDE = "slide";
	
	private final StringProperty format;
	private final StringProperty version;
	private final ObjectProperty<Instant> createdDate;
	private final ObjectProperty<Instant> modifiedDate;
	private final LongProperty time;
	private final ObjectProperty<TextStore> placeholderData;
	private final ObjectProperty<Path> thumbnailPath;
	private final ObservableSet<Tag> tags;
	private final ObservableList<SlideComponent> components;
	private final ObservableList<SlideAnimation> animations;

	public Slide() {
		this.format = new SimpleStringProperty(Constants.FORMAT_NAME);
		this.version = new SimpleStringProperty(Constants.VERSION);
		this.createdDate = new SimpleObjectProperty<>(Instant.now());
		this.modifiedDate = new SimpleObjectProperty<>(this.createdDate.get());
		this.time = new SimpleLongProperty(0);
		this.placeholderData = new SimpleObjectProperty<>();

		this.tags = FXCollections.observableSet(new HashSet<>());
		this.components = FXCollections.observableArrayList();
		this.animations = FXCollections.observableArrayList();
		
		this.thumbnailPath = new SimpleObjectProperty<>();
		
		this.placeholderData.addListener((obs, ov, nv) -> {
			this.updatePlaceholders();
		});
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.SlideRegion#copy()
	 */
	@Override
	public Slide copy() {
		Slide slide = new Slide();
		this.copyTo(slide);
		slide.format.set(this.format.get());
		slide.version.set(this.version.get());
		slide.createdDate.set(this.createdDate.get());
		slide.modifiedDate.set(this.modifiedDate.get());
		slide.name.set(this.name.get());
		slide.placeholderData.set(this.placeholderData.get());
		slide.time.set(this.time.get());
		for (SlideComponent component : this.components) {
			slide.components.add(component.copy());
		}
		for (SlideAnimation animation: this.animations) {
			slide.animations.add(animation.copy());
		}
		slide.tags.addAll(this.tags);
		slide.thumbnailPath.set(this.thumbnailPath.get());
		return slide;
	}
	
	@Override
	public List<Document> index() {
		List<Document> documents = new ArrayList<Document>();
		
		Document document = new Document();

		// allow filtering by the bible id
		document.add(new StringField(FIELD_ID, this.getId().toString(), Field.Store.YES));
		
		// allow filtering by type
		document.add(new StringField(FIELD_TYPE, DATA_TYPE_SLIDE, Field.Store.YES));
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.name.get());
		
		for (Tag tag : this.tags) {
			sb.append(" ").append(tag.getName());
		}
		
		document.add(new TextField(FIELD_TEXT, sb.toString(), Field.Store.YES));
		
		documents.add(document);
		
		return documents;
	}

	@Override
	@JsonProperty(Constants.FORMAT_PROPERTY_NAME)
	public String getFormat() {
		return this.format.get();
	}
	
	@JsonProperty(Constants.FORMAT_PROPERTY_NAME)
	private void setFormat(String format) {
		this.format.set(format);
	}
	
	@Override
	public StringProperty formatProperty() {
		return this.format;
	}
	
	@Override
	@JsonProperty(Constants.VERSION_PROPERTY_NAME)
	public String getVersion() {
		return this.version.get();
	}
	
	@JsonProperty(Constants.VERSION_PROPERTY_NAME)
	public void setVersion(String version) {
		this.version.set(version);
	}
	
	@Override
	public StringProperty versionProperty() {
		return this.version;
	}

	@Override
	@JsonProperty
	public String getName() {
		return this.name.get();
	}
	
	@JsonProperty
	public void setName(String name) {
		this.name.set(name);
	}

	@Override
	public StringProperty nameProperty() {
		return this.name;
	}
	
	@Override
	@JsonProperty
	@JsonSerialize(using = InstantJsonSerializer.class)
	public Instant getCreatedDate() {
		return this.createdDate.get();
	}
	
	@JsonProperty
	@JsonDeserialize(using = InstantJsonDeserializer.class)
	public void setCreatedDate(Instant date) {
		this.createdDate.set(date);
	}
	
	@Override
	public ObjectProperty<Instant> createdDateProperty() {
		return this.createdDate;
	}

	@Override
	@JsonProperty
	@JsonSerialize(using = InstantJsonSerializer.class)
	public Instant getModifiedDate() {
		return this.modifiedDate.get();
	}

	@JsonProperty
	@JsonDeserialize(using = InstantJsonDeserializer.class)
	public void setModifiedDate(Instant date) {
		this.modifiedDate.set(date);
	}
	
	@Override
	public ObjectProperty<Instant> modifiedDateProperty() {
		return this.modifiedDate;
	}
	
	@Override
	@JsonProperty
	public long getTime() {
		return this.time.get();
	}

	@JsonProperty
	public void setTime(long time) {
		this.time.set(time);
	}

	@Override
	public LongProperty timeProperty() {
		return this.time;
	}
	
	public long getTotalTime() {
		long time = this.time.get();
		if (time == Slide.TIME_FOREVER) {
			return time;
		}
		long max = 0;
		List<SlideAnimation> animations = this.getAnimations(this.getId());
		if (animations != null && !animations.isEmpty()) {
			for (SlideAnimation animation : animations) {
				Animation ani = animation.getAnimation();
				if (ani.getRepeatCount() == Animation.INFINITE) {
					return Slide.TIME_FOREVER;
				}
				if (ani.getAnimationType() == AnimationType.IN) {
					long tx = Math.max(0, ani.getDuration()) * Math.max(1, ani.getRepeatCount()) * (ani.isAutoReverseEnabled() ? 2 : 1);
					tx += Math.max(0, ani.getDelay());
					if (tx > max) {
						max = tx;
					}
				}
			}
		}
		return time + max;
	}
	
	@Override
	@JsonProperty
	@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY)
	@JsonSubTypes({
		@Type(value = BibleReferenceTextStore.class, name = "bibleTextStore"),
		@Type(value = SongReferenceTextStore.class, name = "songTextStore")
	})
	public TextStore getPlaceholderData() {
		return this.placeholderData.get();
	}
	
	@JsonProperty
	@JsonSubTypes({
		@Type(value = BibleReferenceTextStore.class, name = "bibleTextStore"),
		@Type(value = SongReferenceTextStore.class, name = "songTextStore")
	})
	public void setPlaceholderData(TextStore data) {
		this.placeholderData.set(data);
	}
	
	@Override
	public ObjectProperty<TextStore> placeholderDataProperty() {
		return this.placeholderData;
	}
	
	@Override
	public boolean hasPlaceholders() {
		for (SlideComponent component : this.components) {
			if (TextPlaceholderComponent.class.isInstance(component)) {
				return true;
			}
		}
		return false;
	}
	
	private void updatePlaceholders() {
		// iterate all the placeholders
		for (TextPlaceholderComponent tpc : this.getComponents(TextPlaceholderComponent.class)) {
			TextItem data = this.getPlaceholderText(tpc.getPlaceholderType(), tpc.getPlaceholderVariant());
			if (data != null) {
				// override the text
				tpc.setText(data.getText());
				// override the font size if necessary
				if (data.getFontSize() > 0) {
					tpc.getFont().setSize(data.getFontSize());
				}
			} else {
				tpc.setText(null);
			}
		}
	}
	
	/**
	 * Returns the placeholder text for the given type and variant.
	 * @param type the type
	 * @param variant the variant
	 * @return String
	 */
	private TextItem getPlaceholderText(TextType type, TextVariant variant) {
		TextStore store = this.placeholderData.get();
		if (store == null) {
			return null;
		}
		
		return store.get(variant, type);
	}
	
	@JsonProperty
	public ObservableSet<Tag> getTags() {
		return this.tags;
	}
	
	@JsonProperty
	public void setTags(Set<Tag> tags) {
		this.tags.addAll(tags);
	}
	
	@Override
	public ObservableSet<Tag> getTagsUnmodifiable() {
		return this.tags;
	}
	
	@JsonProperty
	public ObservableList<SlideComponent> getComponents() {
		return this.components;
	}
	
	@JsonProperty
	public void setComponents(List<SlideComponent> coponents) {
		this.components.setAll(components);
	}
	
	@Override
	public ObservableList<SlideComponent> getComponentsUnmodifiable() {
		return FXCollections.unmodifiableObservableList(this.components);
	}
	
	public void addComponent(SlideComponent component) {
		this.components.add(component);
	}

	public boolean removeComponent(SlideComponent component) {
		// no re-sort required here
		if (this.components.remove(component)) {
			this.animations.removeIf(st -> st.getId().equals(component.getId()));
			return true;
		}
		return false;
	}
	
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
	
	@Override
	public SlideComponent getComponent(UUID id) {
		for (SlideComponent component : this.components) {
			if (component.getId().equals(id)) {
				return component;
			}
		}
		return null;
	}
	
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
	
	public int moveComponent(SlideComponent component, int index) {
		// get all the components
		List<SlideComponent> components = this.components;
		int size = components.size();
		
		// validate the index
		if (index < 0) index = 0;
		if (index >= size) index = size -1; 
		
		// verify the component exists on this slide
		if (components.contains(component) && components.size() > 0) {
			this.components.remove(component);
			this.components.add(index, component);
			return index;
		}
		
		return -1;
	}
	
	public ObservableList<SlideAnimation> getAnimations() {
		return this.animations;
	}
	
	public void setAnimations(List<SlideAnimation> animations) {
		this.animations.setAll(animations);
	}
	
	public ObservableList<SlideAnimation> getAnimationsUnmodifiable() {
		return FXCollections.unmodifiableObservableList(this.animations);
	}
	
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
	
	@Override
	public Path getThumbnailPath() {
		return this.thumbnailPath.get();
	}
	
	void setThumbnailPath(Path path) {
		this.thumbnailPath.set(path);
	}
	
	public ReadOnlyObjectProperty<Path> thumbnailPathProperty() {
		return this.thumbnailPath;
	}

	public void fit(double width, double height) {
		// compute the resize percentages
		double pw = width / this.getWidth();
		double ph = height / this.getHeight();
		// set the slide size
		this.setWidth(width);
		this.setHeight(height);
		// set the sizes for the components
		for (SlideComponent component : this.components) {
			component.adjust(pw, ph);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.slide.AbstractSlideRegion#getReferencedMedia()
	 */
	@Override
	public Set<UUID> getReferencedMedia() {
		Set<UUID> media = super.getReferencedMedia();
		for (SlideComponent component : this.components) {
			media.addAll(component.getReferencedMedia());
		}
		return media;
	}

	public boolean isBackgroundTransitionRequired(Slide slide) {
		if (slide == null) return true;
		if (slide == this) return false;
		
		// we need a transition if the position, size, background
		// or border are different
		if (this.x.get() != slide.x.get() ||
			this.y.get() != slide.y.get() ||
			this.width.get() != slide.width.get() || 
			this.height.get() != slide.height.get() ||
			!Objects.equals(this.background.get(), slide.background.get()) ||
			!Objects.equals(this.border.get(), slide.border.get()) ||
			this.opacity.get() != slide.opacity.get()) {
			return true;
		}
		
		return false;
	}
}
