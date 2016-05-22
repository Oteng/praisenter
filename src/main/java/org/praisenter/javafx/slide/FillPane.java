package org.praisenter.javafx.slide;

import java.util.UUID;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import org.praisenter.javafx.PraisenterContext;
import org.praisenter.javafx.utility.Fx;
import org.praisenter.media.Media;
import org.praisenter.media.MediaType;
import org.praisenter.slide.graphics.ScaleType;
import org.praisenter.slide.graphics.SlideColor;
import org.praisenter.slide.graphics.SlideLinearGradient;
import org.praisenter.slide.graphics.SlidePaint;
import org.praisenter.slide.graphics.SlideRadialGradient;
import org.praisenter.slide.object.MediaObject;

public final class FillPane extends StackPane {
	final PraisenterContext context;
	final SlideMode mode;
	
	final MediaView mediaView;
	final VBox paintView;
	Image image;
	
	Media media;
	MediaType type;
	ScaleType scaling;
	
	double width;
	double height;
	double borderRadius;
	
	public FillPane(PraisenterContext context, SlideMode mode) {
		this.context = context;
		this.mode = mode;
		this.mediaView = new MediaView();
		this.paintView = new VBox();
		
		this.getChildren().addAll(this.paintView, this.mediaView);
	}
	
	public void setSize(double w, double h) {
		this.setPrefSize(w, h);
		this.setMinSize(w, h);
		this.setMaxSize(w, h);
		this.width = w;
		this.height = h;
		this.setPaintViewSize();
		this.setMediaViewSize();
	}
	
	public void setBorderRadius(double r) {
		this.borderRadius = r;
		this.setPaintViewSize();
		this.setMediaViewSize();
	}
	
	public void setPaint(SlidePaint paint) {
		if (paint == null) {
			removePaint();
			return;
		}
		
		// what's the paint type
		if (paint instanceof MediaObject) {
			setMediaObject((MediaObject)paint);
		} else if (paint instanceof SlideColor ||
				   paint instanceof SlideLinearGradient ||
				   paint instanceof SlideRadialGradient) {
			setBackgroundPaint(paint);
		} else {
			// FIXME handle
		}
	}

	private void removePaint() {
		MediaPlayer player = this.mediaView.getMediaPlayer();
		if (player != null) {
			player.stop();
			player.dispose();
		}
		this.mediaView.setMediaPlayer(null);
		this.paintView.setBackground(null);
		this.image = null;
		this.media = null;
		this.type = null;
		this.scaling = null;
	}
	
	private void setPaintViewSize() {
		Fx.setSize(this.paintView, this.width, this.height);
		Rectangle r = new Rectangle(0, 0, this.width, this.height);
		if (this.borderRadius > 0) {
			r.setArcHeight(this.borderRadius * 2);
			r.setArcWidth(this.borderRadius * 2);
		}
		this.paintView.setClip(r);
	}
	
	private void setMediaViewSize() {
		double w = this.width;
		double h = this.height;
		double br = this.borderRadius;
		
		double mw = 0.0;
		double mh = 0.0;
		MediaPlayer player = this.mediaView.getMediaPlayer();
		if (player != null) {
			mw = player.getMedia().getWidth();
			mh = player.getMedia().getHeight();
		}
		
		Rectangle clip = new Rectangle(0, 0, w, h);
		if (br > 0) {
			clip.setArcHeight(br * 2);
			clip.setArcWidth(br * 2);
		}
		
		if (scaling == ScaleType.NONUNIFORM) { 
			this.mediaView.setFitWidth(w);
			this.mediaView.setFitHeight(h);
		} else if (scaling == ScaleType.UNIFORM) {
			// set the fit w/h based on the min
			if (w < h) {
				this.mediaView.setFitWidth(w);
			} else {
				this.mediaView.setFitWidth(h);
			}
		} else {
			// then center it
			this.mediaView.setLayoutX((w - mw) * 0.5);
			this.mediaView.setLayoutY((h - mh) * 0.5);
			// need to set a clip if its bigger than the component
			clip.setX(-(w - mw) * 0.5);
			clip.setY(-(h - mh) * 0.5);
		}
		this.mediaView.setClip(clip);
	}
	
	private void setBackgroundPaint(SlidePaint paint) {
		this.removePaint();
		
		Paint bgPaint = JavaFXTypeConverter.toJavaFX(paint);
		Background background = new Background(new BackgroundFill(bgPaint, new CornerRadii(this.borderRadius), null));
		this.paintView.setBackground(background);
	}
	
	private void setMediaObject(MediaObject mo) {
		// get the media
		Media media = null;
		UUID id = mo.getId();
		if (id != null) {
			media = context.getMediaLibrary().get(id);
		}
		
		if (media == null) {
			// TODO shouldn't happen but could i guess
		} else {
			MediaType type = media.getMetadata().getType();
			
			// did the media change
			if (!media.equals(this.media)) {
				// if so, we need to just start from scratch
				this.removePaint();
				
				// set data
				this.media = media;
				this.type = type;
				this.scaling = mo.getScaling();
				
				// create new image if we are in edit mode or if the media type is image
				if (this.mode == SlideMode.EDIT || type == MediaType.IMAGE) {
					this.image = JavaFXTypeConverter.toJavaFXImage(this.context, media);
					Background background = new Background(new BackgroundImage(
							this.image, 
							BackgroundRepeat.NO_REPEAT, 
							BackgroundRepeat.NO_REPEAT, 
							BackgroundPosition.CENTER, 
							JavaFXTypeConverter.toJavaFX(mo.getScaling())));
					this.paintView.setBackground(background);
				} else {
					// otherwise create a media player
					MediaPlayer player = JavaFXTypeConverter.toJavaFXMediaPlayer(this.context, media, mo.isLoop(), mo.isMute());
					this.mediaView.setMediaPlayer(player);
					setMediaViewSize();
				}
			} else {
				// they are the same media item
				this.scaling = mo.getScaling();
				
				// set player settings based on the given media
				MediaPlayer player = this.mediaView.getMediaPlayer();
				if (player != null) {
					player.setCycleCount(mo.isLoop() ? MediaPlayer.INDEFINITE : 1);
					player.setMute(mo.isMute());
				}
				setMediaViewSize();
				
				// the scale type may have changed
				if (this.image != null) {
					Background background = new Background(new BackgroundImage(
							this.image, 
							BackgroundRepeat.NO_REPEAT, 
							BackgroundRepeat.NO_REPEAT, 
							BackgroundPosition.CENTER, 
							JavaFXTypeConverter.toJavaFX(mo.getScaling())));
					this.paintView.setBackground(background);
				}
			}
		}
	}

	// playable stuff
	
	public void play() {
		MediaPlayer player = this.mediaView.getMediaPlayer();
		if (player != null) {
			player.play();
		}
	}
	
	public void stop() {
		MediaPlayer player = this.mediaView.getMediaPlayer();
		if (player != null) {
			player.stop();
		}
	}
	
	public void dispose() {
		MediaPlayer player = this.mediaView.getMediaPlayer();
		if (player != null) {
			player.dispose();
		}
	}
}