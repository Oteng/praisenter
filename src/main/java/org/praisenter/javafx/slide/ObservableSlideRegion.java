package org.praisenter.javafx.slide;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import org.praisenter.javafx.PraisenterContext;
import org.praisenter.javafx.utility.Fx;
import org.praisenter.slide.SlideRegion;
import org.praisenter.slide.graphics.Rectangle;
import org.praisenter.slide.graphics.SlidePaint;
import org.praisenter.slide.graphics.SlideStroke;

public abstract class ObservableSlideRegion<T extends SlideRegion> implements SlideRegion {
	// the data
	
	final T region;
	final PraisenterContext context;
	final SlideMode mode;
	
	// editable
	
	final IntegerProperty x = new SimpleIntegerProperty();
	final IntegerProperty y = new SimpleIntegerProperty();
	final IntegerProperty width = new SimpleIntegerProperty();
	final IntegerProperty height = new SimpleIntegerProperty();
	
	final ObjectProperty<SlidePaint> background = new SimpleObjectProperty<SlidePaint>();
	final ObjectProperty<SlideStroke> border = new SimpleObjectProperty<SlideStroke>();
	
	final ObjectProperty<Scaling> scale = new SimpleObjectProperty<Scaling>();
	
	// nodes
	
	final Scene scene;
	private final Pane root;
	private final FillPane backgroundNode;
	private final Region borderNode;
	
	// output
	
	WritableImage image;
	final Pane wrap;
	final ImageView imageView;
	
	public ObservableSlideRegion(T region, PraisenterContext context, SlideMode mode) {
		this.region = region;
		this.context = context;
		this.mode = mode;
		
		// set initial values
		this.x.set(region.getX());
		this.y.set(region.getY());
		this.width.set(region.getWidth());
		this.height.set(region.getHeight());
		this.background.set(region.getBackground());
		this.border.set(region.getBorder());
		
		// setup nodes
		this.root = new Pane();
		this.root.setBackground(null);
		this.borderNode = new Region();
		this.backgroundNode = new FillPane(context, mode);
		this.borderNode.setMouseTransparent(true);
		this.scene = new Scene(root, Color.TRANSPARENT);
		this.imageView = new ImageView();
		this.wrap = new Pane(this.imageView);
		this.wrap.setBackground(null);
		
		// listen for changes
		this.x.addListener((obs, ov, nv) -> { 
			this.region.setX(nv.intValue());
			this.root.setLayoutX(nv.intValue());
		});
		this.y.addListener((obs, ov, nv) -> { 
			this.region.setY(nv.intValue());
			this.root.setLayoutY(nv.intValue());
		});
		this.width.addListener((obs, ov, nv) -> { 
			this.region.setWidth(nv.intValue());
			updateSize();
//			updateImage();
		});
		this.height.addListener((obs, ov, nv) -> { 
			this.region.setHeight(nv.intValue());
			updateSize();
//			updateImage();
		});
		this.background.addListener((obs, ov, nv) -> { 
			this.region.setBackground(nv);
			updateFill();
//			updateImage();
		});
		this.border.addListener((obs, ov, nv) -> { 
			this.region.setBorder(nv);
			updateBorder();
//			updateImage();
		});
		
		this.scale.addListener((obs, ov, nv) -> {
			double w = this.region.getWidth();
			double h = this.region.getHeight();
			
			wrap.setLayoutX(nv.sx + this.x.get() * nv.scale);
			wrap.setLayoutY(nv.sy + this.y.get() * nv.scale);
			imageView.setFitWidth(w * nv.scale);
			imageView.setFitHeight(h * nv.scale);
			imageView.setPreserveRatio(true);
		});
	}
	
	void updatePosition() {
		// set position
		int x = this.x.get();
		int y = this.y.get();
		this.root.setLayoutX(x);
		this.root.setLayoutY(y);
	}
	
	void updateSize() {
		int w = this.width.get();
		int h = this.height.get();
		
		Fx.setSize(this.root, w, h);
		Fx.setSize(this.borderNode, w, h);
		
		this.backgroundNode.setSize(w, h);
	}
	
	void updateBorder() {
		SlideStroke ss = this.border.get();
		
		// create new border
		if (ss != null) {
			this.borderNode.setBorder(new Border(new BorderStroke(
					JavaFXTypeConverter.toJavaFX(ss.getPaint()), 
					JavaFXTypeConverter.toJavaFX(ss.getStyle()), 
					new CornerRadii(ss.getRadius()), 
					new BorderWidths(ss.getWidth()), 
					null)));
		} else {
			this.borderNode.setBorder(null);
		}
		
		double r = ss != null ? ss.getRadius() : 0.0;
		this.backgroundNode.setBorderRadius(r);
	}
	
	void updateFill() {
		SlidePaint paint = this.background.get();
		this.backgroundNode.setPaint(paint);
	}
	
	void build(Node content) {
		// set initial node properties
		this.updatePosition();
		this.updateBorder();
		this.updateFill();
		this.updateSize();
		
		if (content != null) {
			this.root.getChildren().addAll(
					this.backgroundNode,
					content,
					this.borderNode);
		} else {
			this.root.getChildren().addAll(
					this.backgroundNode,
					this.borderNode);
		}
		
		this.updateImage();
	}
	
	// FIXME this needs to be called when a bunch of stuff changes, but we really only want it to be called when all the stuff has been updated
	void updateImage() {
		SnapshotParameters params = new SnapshotParameters();
		params.setDepthBuffer(false);
		params.setFill(Color.TRANSPARENT);
		if (this.image != null) {
			if (this.width.get() != (int)this.image.getWidth() ||
				this.height.get() != (int)this.image.getHeight()) {
				this.image = null;
			}
		}
//		Platform.runLater(() -> {
			this.image = this.root.snapshot(params, this.image);
			// TODO for debugging
//			try {
//				ImageIO.write(SwingFXUtils.fromFXImage(this.image, null), "png", new File("C:\\Users\\William\\Desktop\\" + UUID.randomUUID().toString() + ".png"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			this.imageView.setImage(this.image);
//		});
		
		
		
		
	}
	
//	public Pane getRootNode() {
//		return this.root;
//	}
	
	// playable stuff
	
	public void play() {
		this.backgroundNode.play();
	}
	
	public void stop() {
		this.backgroundNode.stop();
	}
	
	public void dispose() {
		this.backgroundNode.dispose();
	}
	
	@Override
	public UUID getId() {
		return this.region.getId();
	}

	@Override
	public boolean isBackgroundTransitionRequired(SlideRegion region) {
		return this.region.isBackgroundTransitionRequired(region);
	}
	
	@Override
	public void adjust(double pw, double ph) {
		this.region.adjust(pw, ph);
		this.x.set(this.region.getX());
		this.y.set(this.region.getY());
		this.width.set(this.region.getWidth());
		this.height.set(this.region.getHeight());
	}
	
	@Override
	public Rectangle resize(int dw, int dh) {
		Rectangle r = this.region.resize(dw, dh);
		this.x.set(this.region.getX());
		this.y.set(this.region.getY());
		this.width.set(this.region.getWidth());
		this.height.set(this.region.getHeight());
		return r;
	}
	
	@Override
	public void translate(int dx, int dy) {
		this.region.translate(dx, dy);
		this.x.set(this.region.getX());
		this.y.set(this.region.getY());
	}

	// x
	
	@Override
	public int getX() {
		return this.x.get();
	}
	
	@Override
	public void setX(int x) {
		this.x.set(x);
	}
	
	public IntegerProperty xProperty() {
		return this.x;
	}
	
	// y
	
	@Override
	public int getY() {
		return this.y.get();
	}
	
	@Override
	public void setY(int y) {
		this.y.set(y);
	}
	
	public IntegerProperty yProperty() {
		return this.y;
	}
	
	// width

	@Override
	public int getWidth() {
		return this.width.get();
	}
	
	@Override
	public void setWidth(int width) {
		this.width.set(width);
	}
	
	public IntegerProperty widthProperty() {
		return this.width;
	}
	
	// height

	@Override
	public int getHeight() {
		return this.height.get();
	}
	
	@Override
	public void setHeight(int height) {
		this.height.set(height);
	}
	
	public IntegerProperty heightProperty() {
		return this.height;
	}
	
	// background
	
	@Override
	public void setBackground(SlidePaint background) {
		this.background.set(background);
	}
	
	@Override
	public SlidePaint getBackground() {
		return this.background.get();
	}
	
	public ObjectProperty<SlidePaint> backgroundProperty() {
		return this.background;
	}
	
	// border
	
	@Override
	public void setBorder(SlideStroke border) {
		this.border.set(border);
	}
	
	@Override
	public SlideStroke getBorder() {
		return this.border.get();
	}
	
	public ObjectProperty<SlideStroke> borderProperty() {
		return this.border;
	}
}
