package org.praisenter.javafx.slide.editor;

import org.praisenter.MediaType;
import org.praisenter.javafx.ApplicationGlyphs;
import org.praisenter.javafx.PraisenterContext;
import org.praisenter.javafx.slide.ObservableMediaComponent;
import org.praisenter.javafx.slide.ObservableSlide;
import org.praisenter.javafx.slide.SlideMode;
import org.praisenter.slide.MediaComponent;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class InsertMediaRibbonTab extends EditorRibbonTab<ObservableSlide<?>> {
	private final PraisenterContext context;
	
	public InsertMediaRibbonTab(PraisenterContext context) {
		super("Media");
		
		this.context = context;
		
		Button image = new Button("Image", ApplicationGlyphs.IMAGE_MEDIA_COMPONENT.duplicate().size(40));
		Button video = new Button("Video", ApplicationGlyphs.VIDEO_MEDIA_COMPONENT.duplicate().size(40));
		Button audio = new Button("Audio", ApplicationGlyphs.AUDIO_MEDIA_COMPONENT.duplicate().size(40));
		
		image.setContentDisplay(ContentDisplay.TOP);
		video.setContentDisplay(ContentDisplay.TOP);
		audio.setContentDisplay(ContentDisplay.TOP);
		
		// layout
		
		HBox row1 = new HBox(2, image, video, audio);
		VBox layout = new VBox(2, row1);
		this.container.setCenter(layout);
	
		// events

		image.setOnAction((e) -> {
			MediaComponent mc = new MediaComponent();
			mc.setWidth(400);
			mc.setHeight(300);
			fireEvent(new MediaComponentAddEvent(image, InsertMediaRibbonTab.this, new ObservableMediaComponent(mc, context, SlideMode.EDIT), MediaType.IMAGE));
		});
		video.setOnAction((e) -> {
			MediaComponent mc = new MediaComponent();
			mc.setWidth(400);
			mc.setHeight(300);
			fireEvent(new MediaComponentAddEvent(video, InsertMediaRibbonTab.this, new ObservableMediaComponent(mc, context, SlideMode.EDIT), MediaType.VIDEO));
		});
		audio.setOnAction((e) -> {
			MediaComponent mc = new MediaComponent();
			mc.setWidth(400);
			mc.setHeight(300);
			fireEvent(new MediaComponentAddEvent(audio, InsertMediaRibbonTab.this, new ObservableMediaComponent(mc, context, SlideMode.EDIT), MediaType.AUDIO));
		});
	}
}
