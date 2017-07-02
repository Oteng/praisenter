package org.praisenter.javafx.slide.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.praisenter.MediaType;
import org.praisenter.javafx.ApplicationAction;
import org.praisenter.javafx.ApplicationEditorPane;
import org.praisenter.javafx.ApplicationEvent;
import org.praisenter.javafx.ApplicationPane;
import org.praisenter.javafx.ApplicationPaneEvent;
import org.praisenter.javafx.DataFormats;
import org.praisenter.javafx.PraisenterContext;
import org.praisenter.javafx.async.AsyncTask;
import org.praisenter.javafx.command.CommandFactory;
import org.praisenter.javafx.command.EditCommand;
import org.praisenter.javafx.media.MediaLibraryDialog;
import org.praisenter.javafx.slide.ObservableMediaComponent;
import org.praisenter.javafx.slide.ObservableSlide;
import org.praisenter.javafx.slide.ObservableSlideComponent;
import org.praisenter.javafx.slide.ObservableSlideRegion;
import org.praisenter.javafx.slide.SlideActions;
import org.praisenter.javafx.slide.SlideMode;
import org.praisenter.javafx.slide.editor.events.MediaComponentAddEvent;
import org.praisenter.javafx.slide.editor.events.SlideComponentAddEvent;
import org.praisenter.javafx.slide.editor.events.SlideComponentOrderEvent;
import org.praisenter.javafx.slide.editor.events.SlideEditorEvent;
import org.praisenter.javafx.slide.editor.ribbon.SlideEditorRibbon;
import org.praisenter.javafx.themes.Styles;
import org.praisenter.javafx.utility.Fx;
import org.praisenter.media.Media;
import org.praisenter.slide.AbstractSlideComponent;
import org.praisenter.slide.Slide;
import org.praisenter.slide.SlideComponent;
import org.praisenter.slide.graphics.ScaleType;
import org.praisenter.slide.object.MediaObject;
import org.praisenter.utility.Scaling;
import org.praisenter.xml.XmlIO;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

// FEATURE (L) Allow grouping of components to easily move them together
// JAVABUG (L) 06/30/16 Text border really slow when the stroke style is INSIDE or OUTSIDE - https://bugs.openjdk.java.net/browse/JDK-8089081
// JAVABUG (M) 02/04/17 DropShadow and Glow effects cannot be mouse transparent - https://bugs.openjdk.java.net/browse/JDK-8092268, https://bugs.openjdk.java.net/browse/JDK-8101376

public final class SlideEditorPane extends BorderPane implements ApplicationPane, ApplicationEditorPane {
	private static final Logger LOGGER = LogManager.getLogger();
	
	private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("edit-selected");
	
	private final SlideEditorContext context;
	
	private final SlideEditorRibbon ribbon;
	private final AnimationsPane animations;
	private final StackPane slidePreview;
	
//	private boolean hasUnsavedChanges = false;
	
	public SlideEditorPane(PraisenterContext context) {
		this.getStyleClass().add(Styles.SLIDE_EDITOR_PANE);
		
		this.context = new SlideEditorContext(context);
		
		// create the ribbon
		this.ribbon = new SlideEditorRibbon(this.context);
		VBox top = new VBox(this.ribbon);
		top.setBorder(new Border(new BorderStroke(null, null, Color.GRAY, null, null, null, new BorderStrokeStyle(StrokeType.CENTERED, StrokeLineJoin.MITER, StrokeLineCap.SQUARE, 1.0, 0.0, null), null, null, new BorderWidths(0, 0, 1, 0), null)));
		this.setTop(top);
		
		// create the animation picker
		this.animations = new AnimationsPane();
		this.setRight(this.animations);
		
		// Node hierarchy:
		// +-------------------------------+--------------+---------------------------------------------------------+
		// | Name                          | Type         | Role                                                    |
		// +-------------------------------+--------------+---------------------------------------------------------+
		// | slidePreview                  | StackPane    | Editor background color                                 |
		// | +- slideBounds                | Pane         | Transparent background, uniform sizing, and drop shadow |
		// |    +- slideCanvas             | Pane         | Contains all the Observable Slide nodes                 |
		// |       +- rootPane             | Pane         | The root pane for the slide                             |
		// |          +- container         | Pane         | Provides scaling                                        |
		// |             +- backgroundNode | FillPane     | For the slide background                                |
		// |             +- borderNode     | Region       | The slide border                                        |
		// |          +- editBorderNode    | Region       | The edit border                                         |
		// +-------------------------------+--Components--+---------------------------------------------------------+
		// |          +- componentCanvas   | Pane         | The slide components                                    |
		// |             +- rootPane       | Pane         | Component 1                                             |
		// |             +- rootPane       | Pane         | Component 2                                             |
		// |             +- ....           | Pane         | Component N                                             |
		// +-------------------------------+--------------+---------------------------------------------------------+
		
		final double padding = 10;
		
		// create the slidePreview area
		this.slidePreview = new StackPane();
		this.slidePreview.setPrefSize(500, 400);
		this.slidePreview.setPadding(new Insets(padding));
		this.slidePreview.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, null)));
		this.slidePreview.setSnapToPixel(true);
		
		// clip by the slidePreview area
		Rectangle clipRect = new Rectangle(this.slidePreview.getWidth(), this.slidePreview.getHeight());
		clipRect.heightProperty().bind(this.slidePreview.heightProperty());
		clipRect.widthProperty().bind(this.slidePreview.widthProperty());
		this.slidePreview.setClip(clipRect);
		
		// create the slideBounds area for the
		// unscaled transparency background
		Pane slideBounds = new Pane();
		slideBounds.setBackground(new Background(new BackgroundImage(Fx.TRANSPARENT_PATTERN, BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, null, null)));
		// add a drop shadow effect for better looks
		DropShadow sdw = new DropShadow();
		sdw.setRadius(5);
		sdw.setColor(Color.rgb(0, 0, 0, 0.3));
		slideBounds.setEffect(sdw);
		
		// we size the slideBounds to a uniform scaled version
		// using the current available space and the slide's 
		// target resolution
		DoubleBinding widthSizing = new DoubleBinding() {
			{
				bind(slidePreview.widthProperty(), 
					 slidePreview.heightProperty());
			}
			@Override
			protected double computeValue() {
				ObservableSlide<?> s = SlideEditorPane.this.context.getSlide();
				if (s != null) {
					double w = s.getWidth();
					double h = s.getHeight();
					double tw = Math.min(slidePreview.getWidth() - padding * 2, w);
					double th = Math.min(slidePreview.getHeight() - padding * 2, h);
					return Math.floor(Scaling.getUniformScaling(w, h, tw, th).width) - 1;
				}
				return 0;
			}
		};
		DoubleBinding heightSizing = new DoubleBinding() {
			{
				bind(slidePreview.widthProperty(), 
					 slidePreview.heightProperty());
			}
			@Override
			protected double computeValue() {
				ObservableSlide<?> s = SlideEditorPane.this.context.getSlide();
				if (s != null) {
					double w = s.getWidth();
					double h = s.getHeight();
					double tw = Math.min(slidePreview.getWidth() - padding * 2, w);
					double th = Math.min(slidePreview.getHeight() - padding * 2, h);
					return Math.floor(Scaling.getUniformScaling(w, h, tw, th).height) - 1;
				}
				return 0;
			}
		};
		slideBounds.maxWidthProperty().bind(widthSizing);
		slideBounds.maxHeightProperty().bind(heightSizing);
		
		// create the slideCanvas
		Pane slideCanvas = new Pane();
		slideCanvas.setMinSize(0, 0);
		slideCanvas.setSnapToPixel(true);
		
		// build the preview hierarchy
		slideBounds.getChildren().add(slideCanvas);
		this.slidePreview.getChildren().addAll(slideBounds);
		StackPane.setAlignment(slideBounds, Pos.CENTER);
		StackPane.setAlignment(slideCanvas, Pos.CENTER);
		this.setCenter(this.slidePreview);

		// events
		
		// listener for selection changes
		this.context.selectedProperty().addListener((obs, ov, nv) -> {
			if (ov != null) {
				ov.getEditBorderNode().pseudoClassStateChanged(SELECTED, false);
			}
			if (nv != null) {
				nv.getEditBorderNode().pseudoClassStateChanged(SELECTED, true);
				nv.getEditBorderNode().requestFocus();
			}
		});
		
		// scaling must be applied to the slide and components separately so that
		// we avoid scaling the selection border
		ObjectBinding<Scaling> scaleFactor = new ObjectBinding<Scaling>() {
			{
				bind(slidePreview.widthProperty(), slidePreview.heightProperty());
			}
			@Override
			protected Scaling computeValue() {
				double tw = slidePreview.getWidth() - padding * 2;
				double th = slidePreview.getHeight() - padding * 2;
				
				ObservableSlide<?> s = SlideEditorPane.this.context.getSlide();
				if (s == null) {
					return Scaling.getNoScaling(tw, th);
				}
				
				double w = s.getWidth();
				double h = s.getHeight();
				
				return Scaling.getUniformScaling(w, h, Math.min(tw, w), Math.min(th, h));
			}
		};
		
		// setup of the editor when the slide being edited changes
		this.context.slideProperty().addListener((obs, ov, nv) -> {
			slideCanvas.getChildren().clear();
			this.context.setSelected(null);
			
			// unbind the scaling from the old value
			if (ov != null) {
				ov.scalingProperty().unbind();
				for (ObservableSlideComponent<?> component : ov.getComponents()) {
					component.scalingProperty().unbind();
				}
			}
			
			if (nv != null) {
				// add the slide node to the slideCanvas
				Node rootEditPane = nv.getDisplayPane();
				slideCanvas.getChildren().add(rootEditPane);
				// bind the scale factor
				nv.scalingProperty().bind(scaleFactor);
				// setup the mouse event handler
				SlideRegionMouseEventHandler slideMouseHandler = new SlideRegionMouseEventHandler(nv, nv);
				rootEditPane.addEventHandler(MouseEvent.ANY, slideMouseHandler);
				rootEditPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
					this.context.setSelected(nv);
				});
				// track mouse drag events
				rootEditPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> { 
					fireEvent(new SlideEditorEvent(this, this, SlideEditorEvent.CHANGED));
				});
				
				for (ObservableSlideComponent<?> component : nv.getComponents()) {
					// bind the scale factor
					component.scalingProperty().bind(scaleFactor);
					// setup the mouse event handler
					SlideRegionMouseEventHandler mouseHandler = new SlideRegionMouseEventHandler(nv, component);
					component.getEditBorderNode().addEventHandler(MouseEvent.ANY, mouseHandler);
					component.getEditBorderNode().addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
						this.context.setSelected(component);
					});
					// track mouse drag events
					component.getEditBorderNode().addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> { 
						fireEvent(new SlideEditorEvent(this, this, SlideEditorEvent.CHANGED));
					});
				}
				
				// invalidate the scale and sizes because the slide
				// might be a different target resolution than the last
				scaleFactor.invalidate();
				widthSizing.invalidate();
				heightSizing.invalidate();
			}
			
			this.hasUnsavedChanges = false;
		});
		
		// setting the target resolution
		this.ribbon.addEventHandler(SlideEditorEvent.TARGET_RESOLUTION, (e) -> {
			scaleFactor.invalidate();
			widthSizing.invalidate();
			heightSizing.invalidate();
		});

		// change tracking
		this.ribbon.addEventHandler(SlideEditorEvent.CHANGED, (e) -> {
			this.stateChanged(ApplicationPaneEvent.REASON_UNDO_REDO_STATE_CHANGED);
//			EditCommand command = e.getCommand();
//			// always add a select component action
//			EditCommand wrapper = CommandFactory.chain(command, );
//			applyCommand(wrapper);
		});
		
		// adding components
		this.ribbon.addEventHandler(SlideEditorEvent.ADD_COMPONENT, (e) -> {
			ObservableSlide<?> slide = this.context.getSlide();
			ObservableSlideComponent<?> component = e.getComponent();
			
			if (e.isCentered()) {
				// compute the location (center it)
				double sw = slide.getWidth();
				double sh = slide.getHeight();
				double w = component.getWidth();
				double h = component.getHeight();
				component.setX((sw - w) * 0.5);
				component.setY((sh - h) * 0.5);
			}
			
			// bind the scale factor
			component.scalingProperty().bind(scaleFactor);
			// setup the mouse event handler
			SlideRegionMouseEventHandler mouseHandler = new SlideRegionMouseEventHandler(slide, component);
			component.getEditBorderNode().addEventHandler(MouseEvent.ANY, mouseHandler);
			component.getEditBorderNode().addEventHandler(MouseEvent.MOUSE_PRESSED, ev -> {
				this.context.setSelected(component);
			});
			// track mouse drag events
			component.getEditBorderNode().addEventHandler(MouseEvent.MOUSE_DRAGGED, ev -> { 
				fireEvent(new SlideEditorEvent(this, this, SlideEditorEvent.CHANGED));
			});
			
			// if the component type is media, then show the media dialog
			// before adding the component to the slide
			if (e instanceof MediaComponentAddEvent) {
				MediaType type = ((MediaComponentAddEvent)e).getMediaType();
				ObservableMediaComponent omc = (ObservableMediaComponent)component;
				MediaLibraryDialog dialog = new MediaLibraryDialog(
						getScene().getWindow(), 
						context,
						type);
				
				dialog.show(m -> {
					// don't add the component unless they choose a
					// media item
					if (m != null) {
						UUID id = m.getId();
						MediaObject mo = null;
						MediaObject omo = omc.getMedia();
						if (omo != null) {
							// copy old settings
							mo = new MediaObject(id, m.getName(), m.getType(), omo.getScaling(), omo.isLoop(), omo.isMute());
						} else {
							// default settings
							mo = new MediaObject(id, m.getName(), m.getType(), ScaleType.UNIFORM, false, false);
						}
						
						omc.setMedia(mo);
						slide.addComponent(component);
						
						// set it as the selected component after the user has chosen a media
						// item (if we don't then the media ribbon doesn't know that this media
						// was selected and therefore clears it when they change anything about it
						// like the scaling)
						this.context.setSelected(component);
						fireEvent(new SlideEditorEvent(this, this, SlideEditorEvent.CHANGED));
					}
				});
			} else {
				slide.addComponent(component);
				slide.updatePlaceholders();
				this.context.setSelected(component);
				fireEvent(new SlideEditorEvent(this, this, SlideEditorEvent.CHANGED));
			}
		});
		
		// bindings

		this.animations.slideProperty().bind(this.context.slideProperty());
		this.animations.componentProperty().bind(this.context.selectedProperty());
		
		// set values
		
		this.addEventHandler(ApplicationEvent.ALL, e -> {
			handleApplicationEvent(e.getAction());
		});
	}
	
	public void setSlide(Slide slide) {
		if (slide == null) {
			this.context.setSlide(null);
		} else {
			this.context.setSlide(new ObservableSlide<Slide>(slide, this.context.getContext(), SlideMode.EDIT));
		}
	}
	
	public Slide getSlide() {
		return this.context.getSlide().getRegion();
	}
	
	// METHODS
	
	private final void save(boolean saveAs) {
		ObservableSlide<?> os = this.context.getSlide();
		Slide slide = os.getRegion();
		PraisenterContext context = this.context.getContext();
		
		AsyncTask<Slide> task = null;
		if (saveAs) {
			task = SlideActions.slidePromptSaveAs(
					context.getSlideLibrary(), 
					this.getScene().getWindow(), 
					slide);
		} else {
			task = SlideActions.slideSave(
					context.getSlideLibrary(), 
					this.getScene().getWindow(), 
					slide);
		}
		
		final boolean hasChanged = this.hasUnsavedChanges;
		this.hasUnsavedChanges = false;
		task.addSuccessHandler(e -> {
			Slide saved = (Slide)e.getSource().getValue();
			// make the current slide being edited act
			// as the one we saved so that any subsequent
			// saves save to this one and so that we don't
			// lose any changes made by the user
			// while the save action was processing
			boolean moreChanges = this.hasUnsavedChanges;
			// NOTE: these will change the hasUnsavedChanges value, so
			//       we store the current value before we call these
			this.context.getSlide().getRegion().as(saved);
			// manually update the name field
			this.ribbon.setSlideName(saved.getName());
			// did the user make changes while the save was happening?
			if (!moreChanges) {
				// if not, then force the unsaved changes flag to false
				this.hasUnsavedChanges = false;
			}
		}).addCancelledOrFailedHandler(e -> {
			// if the save fails, make sure it gets set back to it's original value
			this.hasUnsavedChanges = hasChanged;
		}).execute(context.getExecutorService());
	}
	
	private final void copy(boolean cut) {
		ObservableSlide<?> slide = this.context.getSlide();
		ObservableSlideRegion<?> selected = this.context.getSelected();
		PraisenterContext context = this.context.getContext();
		
		if (selected != null) {
			Clipboard cb = Clipboard.getSystemClipboard();
			ClipboardContent cc = new ClipboardContent();
			cc.putString(selected.getName());
			
			// if it's a media component then try to add the media to the clipboard
			if (selected instanceof ObservableMediaComponent) {
				ObservableMediaComponent omc = (ObservableMediaComponent)selected;
				MediaObject mo = omc.getMedia();
				if (mo != null) {
					Media media = context.getMediaLibrary().get(mo.getId());
					// did we find the media?
					if (media != null) {
						// for images
						if (media.getType() == MediaType.IMAGE) {
							// this works for stuff like paint and gimp but not Word
							cc.putImage(context.getImageCache().getOrLoadImageMediaImage(media.getId(), media.getPath()));
						}
						// for image/video/audio
						// this works for Word
						List<File> files = new ArrayList<File>();
						files.add(media.getPath().toFile());
						cc.putFiles(files);
					}
				}
			}
			
			try {
				cc.put(DataFormats.SLIDE_COMPONENT, XmlIO.save(selected.getRegion().copy()));
				cb.setContent(cc);
				
				if (cut) {
					// remove the component
					slide.removeComponent((ObservableSlideComponent<?>)selected);
				}
				
				// notify we changed
				this.stateChanged(ApplicationPaneEvent.REASON_DATA_COPIED);
			} catch (Exception e) {
				LOGGER.warn("Failed to serialize the slide component to the clipboard.", e);
			}
		}
	}
	
	private final void paste() {
		ObservableSlide<?> slide = this.context.getSlide();
		
		Clipboard cb = Clipboard.getSystemClipboard();
		Object data = cb.getContent(DataFormats.SLIDE_COMPONENT);
		if (data != null && data instanceof String) {
			SlideComponent sc;
			try {
				sc = (SlideComponent)XmlIO.read((String)data, AbstractSlideComponent.class);
				sc = sc.copy();
				sc.translate(20, 20);
				ObservableSlideComponent<?> osc = slide.observableSlideComponent(sc);
				this.ribbon.fireEvent(new SlideComponentAddEvent(this, this, osc, false, true));
			} catch (Exception e) {
				LOGGER.warn("Failed to parse the copied slide component that's in the clipboard: '" + data + "'", e);
			}
		}
	}
	
	private final void delete() {
		ObservableSlide<?> slide = this.context.getSlide();
		ObservableSlideRegion<?> selected = this.context.getSelected();
		
		if (selected != null && selected instanceof ObservableSlideComponent) {
			if (slide.removeComponent((ObservableSlideComponent<?>)selected)) {
				this.context.setSelected(slide);
			}
		}
	}
	
	private void handleApplicationEvent(ApplicationAction action) {
		switch (action) {
			case SAVE:
				this.save(false);
				break;
			case SAVE_AS:
				this.save(true);
				break;
			case CUT:
				this.copy(true);
				break;
			case COPY:
				this.copy(false);
				break;
			case PASTE:
				this.paste();
				break;
			case DELETE:
				this.delete();
				break;
			default:
				return;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.javafx.ApplicationPane#isApplicationActionEnabled(org.praisenter.javafx.ApplicationAction)
	 */
	@Override
	public boolean isApplicationActionEnabled(ApplicationAction action) {
		Node focused = this.getScene().getFocusOwner();
		ObservableSlideRegion<?> selected = this.context.getSelected();
		
		switch (action) {
			case SAVE:
			case SAVE_AS:
				return true;
			case COPY:
			case CUT:
			case DELETE:
				return selected != null && selected instanceof ObservableSlideComponent;
			case PASTE:
				Clipboard cb = Clipboard.getSystemClipboard();
				if (Fx.isNodeInFocusChain(focused, this.slidePreview)) {
					return cb.hasContent(DataFormats.SLIDE_COMPONENT);
				}
				return false;
			default:
				return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.praisenter.javafx.ApplicationPane#setDefaultFocus()
	 */
	@Override
	public void setDefaultFocus() {
		// NOTE: this is here so that the slide ribbon has focus initially so that
		// the focus is inside the editor pane, allowing the application actions
		// to work. selection of components in the editor isn't the same as standard
		// java fx selection
		this.ribbon.requestFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.javafx.ApplicationPane#isApplicationActionVisible(org.praisenter.javafx.ApplicationAction)
	 */
	@Override
	public boolean isApplicationActionVisible(ApplicationAction action) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.praisenter.javafx.ApplicationPane#cleanup()
	 */
	@Override
	public void cleanup() {
		this.context.setSlide(null);
		this.context.setSelected(null);
		this.context.getEditManager().reset();
	}
	
    /**
     * Called when the state of this pane changes.
     * @param reason the reason
     */
    private final void stateChanged(String reason) {
    	Scene scene = this.getScene();
    	// don't bother if there's no place to send the event to
    	if (scene != null) {
    		fireEvent(new ApplicationPaneEvent(this, this, ApplicationPaneEvent.STATE_CHANGED, this, reason));
    	}
    }
    
    /* (non-Javadoc)
     * @see org.praisenter.javafx.ApplicationEditorPane#getTargetName()
     */
    @Override
    public String getEditTargetName() {
    	ObservableSlide<?> slide = this.context.getSlide();
    	String name = "Untitled";
    	if (slide != null) {
    		String sn = slide.getName();
    		if (sn != null && !sn.trim().isEmpty()) {
    			name = sn;
    		}
    	}
    	return name;
    }
    
    /* (non-Javadoc)
     * @see org.praisenter.javafx.ApplicationEditorPane#hasUnsavedChanges()
     */
    @Override
    public boolean hasUnsavedChanges() {
    	return this.hasUnsavedChanges;
    }
    
    /* (non-Javadoc)
     * @see org.praisenter.javafx.ApplicationEditorPane#saveChanges()
     */
    @Override
    public void saveChanges() {
    	this.save(false);
    }
}
