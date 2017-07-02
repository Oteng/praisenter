package org.praisenter.javafx;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.praisenter.Constants;
import org.praisenter.javafx.async.AsyncTask;
import org.praisenter.javafx.configuration.Display;
import org.praisenter.javafx.configuration.ObservableConfiguration;
import org.praisenter.javafx.configuration.Setting;
import org.praisenter.javafx.configuration.SettingBatch;
import org.praisenter.javafx.media.JavaFXMediaImportFilter;
import org.praisenter.javafx.screen.ScreenView;
import org.praisenter.javafx.screen.ScreenViewDragDropManager;
import org.praisenter.javafx.themes.Styles;
import org.praisenter.javafx.themes.Theme;
import org.praisenter.resources.translations.Translations;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

// TODO translate
// TODO fix UI - layout looks good, now just need a good way to group stuff - maybe add some text about each item
// FEATURE (M) Add options to remove themes or translations
// FEATURE (L) Edit tags (like rename, remove)

public final class SetupPane extends BorderPane {
	/** The class level logger */
	private static final Logger LOGGER = LogManager.getLogger();

	private static final String ROOT = "root";
	private static final String GENERAL = "general";
	private static final String DISPLAYS = "displays";
	private static final String MEDIA = "media";
	
	// the layout
	
	private final TreeView<SetupTreeData> setupTree;
	
	// for identifying the displays
	
	/** The windows doing the identification */
	private List<Stage> identifiers = new ArrayList<Stage>();
	
	/** A timeline to play to make sure they close after x amount time */
	private Timeline identifierClose = null;
	
	public SetupPane(PraisenterContext context) {
		this.getStyleClass().add(Styles.SETUP_PANE);
		
		TreeItem<SetupTreeData> root = new TreeItem<SetupTreeData>(new SetupTreeData(ROOT, "Preferences"));
		TreeItem<SetupTreeData> general = new TreeItem<SetupTreeData>(new SetupTreeData(GENERAL, "General"));
		TreeItem<SetupTreeData> displays = new TreeItem<SetupTreeData>(new SetupTreeData(DISPLAYS, "Displays"));
		TreeItem<SetupTreeData> media = new TreeItem<SetupTreeData>(new SetupTreeData(MEDIA, "Media"));
		root.getChildren().add(general);
		root.getChildren().add(displays);
		root.getChildren().add(media);
		
		this.setupTree = new TreeView<SetupTreeData>(root);
		this.setupTree.setShowRoot(false);
		this.setupTree.getSelectionModel().select(general);
		this.setupTree.setMaxHeight(Double.MAX_VALUE);
		
		// GENERAL

		List<Option<Locale>> locales = new ArrayList<Option<Locale>>();
		for (Locale locale : Translations.getAvailableLocales()) {
			locales.add(new Option<Locale>(locale.getDisplayName(), locale));
		}
		
		Locale locale = context.getConfiguration().getLanguage();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		
		Theme theme = context.getConfiguration().getTheme();
		if (theme == null) {
			theme = Theme.DEFAULT;
		}
		
		GridPane gridGeneral = new GridPane();
		gridGeneral.setHgap(5);
		gridGeneral.setVgap(5);
		
		// language
		Label lblLocale = new Label("Language");
		ComboBox<Option<Locale>> cmbLocale = new ComboBox<Option<Locale>>(FXCollections.observableArrayList(locales));
		cmbLocale.setValue(new Option<Locale>(null, locale));
		Button btnRefreshLocales = new Button("", ApplicationGlyphs.REFRESH.duplicate());
		Button btnDownloadLocale = new Button("", ApplicationGlyphs.EXPORT.duplicate());
		Button btnUploadLocale = new Button("", ApplicationGlyphs.IMPORT.duplicate());
		gridGeneral.add(lblLocale, 0, 0);
		gridGeneral.add(cmbLocale, 1, 0);
		gridGeneral.add(btnRefreshLocales, 2, 0);
		gridGeneral.add(btnDownloadLocale, 3, 0);
		gridGeneral.add(btnUploadLocale, 4, 0);
		cmbLocale.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(cmbLocale, true);
		
		// theme
		Label lblTheme = new Label("Theme");
		ComboBox<Theme> cmbTheme = new ComboBox<Theme>(FXCollections.observableArrayList(Theme.getAvailableThemes()));
		cmbTheme.setValue(theme);
		Button btnRefreshThemes = new Button("", ApplicationGlyphs.REFRESH.duplicate());
		Button btnDownloadTheme = new Button("", ApplicationGlyphs.EXPORT.duplicate());
		Button btnUploadTheme = new Button("", ApplicationGlyphs.IMPORT.duplicate());
		gridGeneral.add(lblTheme, 0, 1);
		gridGeneral.add(cmbTheme, 1, 1);
		gridGeneral.add(btnRefreshThemes, 2, 1);
		gridGeneral.add(btnDownloadTheme, 3, 1);
		gridGeneral.add(btnUploadTheme, 4, 1);
		cmbTheme.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(cmbTheme, true);
		
		// debug mode
		Label lblDebugMode = new Label("Debug Mode");
		CheckBox chkDebugMode = new CheckBox();
		chkDebugMode.setSelected(context.getConfiguration().getBoolean(Setting.APP_DEBUG_MODE, false));
		gridGeneral.add(lblDebugMode, 0, 2);
		gridGeneral.add(chkDebugMode, 1, 2);

		Label lblRestartWarning = new Label("Changing the Theme, Language, or Debug Mode requires the application to be restarted to take effect.", ApplicationGlyphs.INFO.duplicate());
		lblRestartWarning.setPadding(new Insets(0, 0, 5, 0));
		
		VBox vboxGeneral = new VBox(lblRestartWarning, gridGeneral);
		vboxGeneral.setPadding(new Insets(10));
		
		// MEDIA
		
		GridPane gridMedia = new GridPane();
		gridMedia.setHgap(5);
		gridMedia.setVgap(5);

		// transcoding
		Label lblTranscodeAudioVideo = new Label("Transcode");
		CheckBox chkTranscodeAudioVideo = new CheckBox();
		chkTranscodeAudioVideo.setSelected(context.getConfiguration().getBoolean(Setting.MEDIA_TRANSCODING_ENABLED, true));
		gridMedia.add(lblTranscodeAudioVideo, 0, 0);
		gridMedia.add(chkTranscodeAudioVideo, 1, 0);
		
		// transcoding video
		Label lblVideo = new Label("Video");
		TextField txtVideoExtension = new TextField(context.getConfiguration().getString(Setting.MEDIA_TRANSCODING_VIDEO_EXTENSION, JavaFXMediaImportFilter.DEFAULT_VIDEO_EXTENSION));
		TextField txtVideoCommand = new TextField(context.getConfiguration().getString(Setting.MEDIA_TRANSCODING_VIDEO_COMMAND, JavaFXMediaImportFilter.DEFAULT_COMMAND));
		txtVideoExtension.setPrefWidth(75);
		txtVideoCommand.setPrefWidth(600);
		gridMedia.add(lblVideo, 0, 1);
		gridMedia.add(txtVideoExtension, 1, 1);
		gridMedia.add(txtVideoCommand, 2, 1);

		// transcoding audio
		Label lblAudio = new Label("Audio");
		TextField txtAudioExtension = new TextField(context.getConfiguration().getString(Setting.MEDIA_TRANSCODING_AUDIO_EXTENSION, JavaFXMediaImportFilter.DEFAULT_AUDIO_EXTENSION));
		TextField txtAudioCommand = new TextField(context.getConfiguration().getString(Setting.MEDIA_TRANSCODING_AUDIO_COMMAND, JavaFXMediaImportFilter.DEFAULT_COMMAND));
		txtAudioExtension.setPrefWidth(75);
		txtAudioCommand.setPrefWidth(600);
		gridMedia.add(lblAudio, 0, 2);
		gridMedia.add(txtAudioExtension, 1, 2);
		gridMedia.add(txtAudioCommand, 2, 2);
		
		VBox vboxMedia = new VBox(gridMedia);
		vboxMedia.setPadding(new Insets(10));
		
		// SCREENS
		
		Label lblScreenWarning = new Label("Changing the screen assignment will close any currently displayed slides or notifications.", ApplicationGlyphs.WARN.duplicate());
		Label lblScreenHowTo = new Label("Drag and drop the screen to assign its role.");
		lblScreenHowTo.setPadding(new Insets(0, 0, 10, 0));
		lblScreenWarning.setPadding(new Insets(0, 0, 10, 0));
		
		Button btnIdentify = new Button("Identify Displays");
		Label lblIdentifyWarning = new Label("A number will show on each screen.", ApplicationGlyphs.WARN.duplicate());
		HBox boxIdentify = new HBox(5, btnIdentify, lblIdentifyWarning);
		boxIdentify.setAlignment(Pos.BASELINE_LEFT);
		
		GridPane screenPane = new GridPane();
		screenPane.setHgap(10);
		screenPane.setVgap(10);
		screenPane.setPadding(new Insets(0, 0, 10, 0));
		
		Label lblOperatorScreen = new Label("Operator");
		Label lblPrimaryScreen = new Label("Primary");
		Label lblMusicianScreen = new Label("Musician");
		lblOperatorScreen.setFont(Font.font("System", FontWeight.BOLD, 15));
		lblPrimaryScreen.setFont(Font.font("System", FontWeight.BOLD, 15));
		lblMusicianScreen.setFont(Font.font("System", FontWeight.BOLD, 15));
		
		Label lblOperatorDescription = new Label("This is the screen that you will be operating from with the Praisenter application and any other tools. Typically your default desktop.");
		Label lblPrimaryDescription = new Label("This is the screen that will show the presentations and notifications.");
		Label lblMusicianDescription = new Label("This is the screen that will show specialized musician or song related information (optional).");
		
		lblOperatorDescription.setWrapText(true);
		lblOperatorDescription.setTextAlignment(TextAlignment.CENTER);
		GridPane.setHgrow(lblOperatorDescription, Priority.NEVER);
		GridPane.setValignment(lblOperatorDescription, VPos.TOP);
		
		lblPrimaryDescription.setWrapText(true);
		lblPrimaryDescription.setTextAlignment(TextAlignment.CENTER);
		GridPane.setHgrow(lblPrimaryDescription, Priority.NEVER);
		GridPane.setValignment(lblPrimaryDescription, VPos.TOP);

		lblMusicianDescription.setWrapText(true);
		lblMusicianDescription.setTextAlignment(TextAlignment.CENTER);
		GridPane.setHgrow(lblMusicianDescription, Priority.NEVER);
		GridPane.setValignment(lblMusicianDescription, VPos.TOP);
		
		screenPane.add(lblOperatorScreen, 0, 1);
		screenPane.add(lblPrimaryScreen, 1, 1);
		screenPane.add(lblMusicianScreen, 2, 1);
		screenPane.add(lblOperatorDescription, 0, 2);
		screenPane.add(lblPrimaryDescription, 1, 2);
		screenPane.add(lblMusicianDescription, 2, 2);
		
		GridPane.setHalignment(lblOperatorScreen, HPos.CENTER);
		GridPane.setHalignment(lblPrimaryScreen, HPos.CENTER);
		GridPane.setHalignment(lblMusicianScreen, HPos.CENTER);
		
		VBox vboxScreens = new VBox(lblScreenHowTo, lblScreenWarning, screenPane, boxIdentify);
		vboxScreens.setPadding(new Insets(10));

		// LAYOUT
		
		BorderPane right = new BorderPane();
		right.setCenter(vboxGeneral);
		
		SplitPane split = new SplitPane(this.setupTree, right);
		split.setOrientation(Orientation.HORIZONTAL);
		split.setDividerPositions(0.15);
		SplitPane.setResizableWithParent(this.setupTree, false);
		
		this.setCenter(split);
		
		// EVENTS

		cmbLocale.valueProperty().addListener((obs, ov, nv) -> {
			if (nv != null) {
				context.getConfiguration()
					.setString(Setting.APP_LANGUAGE, nv.value.toLanguageTag())
					.execute(context.getExecutorService());
			}
		});
		
		btnRefreshLocales.setOnAction(e -> {
			cmbLocale.setItems(refreshLocales());
		});
		
		btnDownloadLocale.setOnAction(e -> {
			Path path = null;
	    	Option<Locale> selected = cmbLocale.getValue();
	    	if (selected != null) {
	    		Locale loc = selected.getValue();
	    		if (!loc.equals(Locale.ENGLISH)) {
		    		String tag = loc.toLanguageTag();
		    		path = Paths.get(Constants.LOCALES_ABSOLUTE_FILE_PATH).resolve("messages_" + tag + ".properties");
	    		}
	    	}
			
			FileChooser chooser = new FileChooser();
	    	chooser.setInitialFileName(path != null ? path.getFileName().toString() : "messages.properties");
	    	chooser.setTitle("Export Translation");
	    	chooser.getExtensionFilters().add(new ExtensionFilter("Java Translations", "*.properties"));
	    	File file = chooser.showSaveDialog(getScene().getWindow());
	    	
	    	if (file != null) {
		    	if (path != null) {
			    	try {
			    		Files.copy(path, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			    	} catch (Exception ex) {
			    		LOGGER.error("Failed to export translation", ex);
			    	}
		    	} else {
		    		try (InputStream def = SetupPane.class.getResourceAsStream("/org/praisenter/resources/translations/messages.properties")) {
		    			Files.copy(def, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    		} catch (Exception ex) {
		    			LOGGER.error("Failed to export default translation", ex);
		    		}
		    	}
	    	}
		});
		
		btnUploadLocale.setOnAction(e -> {
			FileChooser chooser = new FileChooser();
	    	chooser.setTitle("Import Translation");
	    	chooser.getExtensionFilters().add(new ExtensionFilter("Java Translations", "*.properties"));
	    	File file = chooser.showOpenDialog(getScene().getWindow());
	    	if (file != null) {
	    		try {
	    			Path path = Paths.get(Constants.LOCALES_ABSOLUTE_FILE_PATH).resolve(file.getName());
		    		Files.copy(file.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
		    	} catch (Exception ex) {
		    		LOGGER.error("Failed to import translation", ex);
		    	}
	    		cmbLocale.setItems(refreshLocales());
	    	}
		});
		
		cmbTheme.valueProperty().addListener((obs, ov, nv) -> {
			if (nv != null) {
				context.getConfiguration()
					.setString(Setting.APP_THEME, nv.getName())
					.execute(context.getExecutorService());
			}
		});
		
		btnRefreshThemes.setOnAction(e -> {
			cmbTheme.setItems(FXCollections.observableArrayList(Theme.getAvailableThemes()));
		});
		
		btnDownloadTheme.setOnAction(e -> {
			Path path = null;
	    	Theme selected = cmbTheme.getValue();
	    	if (selected != null && !selected.equals(Theme.DEFAULT) && !selected.equals(Theme.DARK)) {
	    		path = Paths.get(Constants.THEMES_ABSOLUTE_FILE_PATH).resolve(selected.getName() + ".css");
	    	}
			
			FileChooser chooser = new FileChooser();
	    	chooser.setInitialFileName(path != null ? path.getFileName().toString() : "default.css");
	    	chooser.setTitle("Export Theme");
	    	chooser.getExtensionFilters().add(new ExtensionFilter("Cascading Style Sheet", "*.css"));
	    	File file = chooser.showSaveDialog(getScene().getWindow());
	    	
	    	if (path != null) {
		    	try {
		    		Files.copy(path, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    	} catch (Exception ex) {
		    		LOGGER.error("Failed to export theme", ex);
		    	}
	    	} else {
	    		try (InputStream def = SetupPane.class.getResourceAsStream("/org/praisenter/javafx/themes/" + selected.getName() + ".css")) {
	    			Files.copy(def, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	    		} catch (Exception ex) {
	    			LOGGER.error("Failed to export default theme", ex);
	    		}
	    	}
		});
		
		btnUploadTheme.setOnAction(e -> {
			FileChooser chooser = new FileChooser();
	    	chooser.setTitle("Import Theme");
	    	chooser.getExtensionFilters().add(new ExtensionFilter("Cascading Style Sheet", "*.css"));
	    	File file = chooser.showOpenDialog(getScene().getWindow());
	    	if (file != null) {
	    		try {
	    			Path path = Paths.get(Constants.THEMES_ABSOLUTE_FILE_PATH).resolve(file.getName());
		    		Files.copy(file.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
		    	} catch (Exception ex) {
		    		LOGGER.error("Failed to import theme", ex);
		    	}
	    		cmbTheme.setItems(FXCollections.observableArrayList(Theme.getAvailableThemes()));
	    	}
		});
		
		chkDebugMode.selectedProperty().addListener((obs, ov, nv) -> {
			if (nv) {
				context.getConfiguration()
					.setBoolean(Setting.APP_DEBUG_MODE, true)
					.execute(context.getExecutorService());
			} else {
				context.getConfiguration()
					.remove(Setting.APP_DEBUG_MODE)
					.execute(context.getExecutorService());
			}
		});
		
		btnIdentify.setOnAction(e -> {
			// show a window for each screen with a number on it
			List<Screen> screens = Screen.getScreens();
			
			if (identifierClose != null) {
				identifierClose.stop();
			}
			for (Stage stage : this.identifiers) {
				stage.close();
			}
			identifiers.clear();
			identifierClose = new Timeline(new KeyFrame(Duration.seconds(3), 
					ae -> {
						for (Stage stage : this.identifiers) {
							stage.close();
						}
					}));
			
			int i = 1;
			for (Screen screen : screens) {
				Stage stage = new Stage(StageStyle.TRANSPARENT);
				stage.initOwner(getScene().getWindow());
				stage.initModality(Modality.NONE);
				stage.setTitle("IDENTIFY-" + i);
				stage.setAlwaysOnTop(true);
				stage.setResizable(false);
				// position and size
				Rectangle2D bounds = screen.getBounds();
				stage.setX(bounds.getMinX());
				stage.setY(bounds.getMinY());
				stage.setWidth(bounds.getWidth());
				stage.setHeight(bounds.getHeight());
				// content
				Pane container = new Pane();
				container.setBackground(null);
				StackPane block = new StackPane();
				block.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
				block.setPadding(new Insets(50));
				block.setTranslateX(50);
				block.setTranslateY(50);
				Text text = new Text(String.valueOf(i));
				text.setFill(Color.WHITE);
				text.setFont(Font.font(text.getFont().getName(), 80));
				block.getChildren().add(text);
				container.getChildren().add(block);
				
				stage.setScene(new Scene(container, null));
				identifiers.add(stage);
				
				stage.show();
				i++;
			}
			
			identifierClose.play();
		});
		
		// create a custom manager
		ScreenViewDragDropManager manager = new ScreenViewDragDropManager() {
			@Override
			public void swap(ScreenView view1, ScreenView view2) {
				int col1 = GridPane.getColumnIndex(view1);
				int row1 = GridPane.getRowIndex(view1);
				int col2 = GridPane.getColumnIndex(view2);
				int row2 = GridPane.getRowIndex(view2);
				screenPane.getChildren().removeAll(view1, view2);
				screenPane.add(view2, col1, row1);
				screenPane.add(view1, col2, row2);
				
				// record the changes (will save automatically)
				ObservableConfiguration conf = context.getConfiguration();
				SettingBatch<AsyncTask<Void>> batch = conf.createBatch();
				if (col1 == 0) {
					batch.setObject(Setting.DISPLAY_OPERATOR, view2.getDisplay());
					lblOperatorDescription.setPrefWidth(view2.getPrefWidth());
				} else if (col1 == 1) {
					batch.setObject(Setting.DISPLAY_MAIN, view2.getDisplay());
					lblPrimaryDescription.setPrefWidth(view2.getPrefWidth());
				} else if (col1 == 2) {
					batch.setObject(Setting.DISPLAY_MUSICIAN, view2.getDisplay());
					lblMusicianDescription.setPrefWidth(view2.getPrefWidth());
				}
				if (col2 == 0) {
					batch.setObject(Setting.DISPLAY_OPERATOR, view1.getDisplay());
					lblOperatorDescription.setPrefWidth(view1.getPrefWidth());
				} else if (col2 == 1) {
					batch.setObject(Setting.DISPLAY_MAIN, view1.getDisplay());
					lblPrimaryDescription.setPrefWidth(view1.getPrefWidth());
				} else if (col2 == 2) {
					batch.setObject(Setting.DISPLAY_MUSICIAN, view1.getDisplay());
					lblMusicianDescription.setPrefWidth(view1.getPrefWidth());
				}
				
				batch.commitBatch()
					.execute(context.getExecutorService());
			}
		};
		
		// listener for updating the screen views when the 
		// screens change or when the parent node changes
		InvalidationListener screenListener = new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				ObservableConfiguration conf = context.getConfiguration();
				
				Display os = conf.getObject(Setting.DISPLAY_OPERATOR, Display.class, null);
				Display ms = conf.getObject(Setting.DISPLAY_MAIN, Display.class, null);
				Display cs = conf.getObject(Setting.DISPLAY_MUSICIAN, Display.class, null);
				
				Map<Integer, ScreenView> views = ScreenView.createScreenViews(manager);
				
				ScreenView operator = null;
				ScreenView main = null;
				ScreenView musician = null;
				
				if (os != null) {
					operator = views.remove(os.getId());
				}
				if (ms != null) {
					main = views.remove(ms.getId());
				}
				if (cs != null) {
					musician = views.remove(cs.getId());
				}
				
				if (operator == null) {
					operator = ScreenView.createUnassignedScreenView(manager);
				}
				if (main == null) {
					main = ScreenView.createUnassignedScreenView(manager);
				}
				if (musician == null) {
					musician = ScreenView.createUnassignedScreenView(manager);
				}
				
				screenPane.add(operator, 0, 0);
				screenPane.add(main, 1, 0);
				screenPane.add(musician, 2, 0);
				
				GridPane.setHalignment(operator, HPos.CENTER);
				GridPane.setHalignment(main, HPos.CENTER);
				GridPane.setHalignment(musician, HPos.CENTER);
				
				lblOperatorDescription.setPrefWidth(operator.getPrefWidth());
				lblPrimaryDescription.setPrefWidth(main.getPrefWidth());
				lblMusicianDescription.setPrefWidth(musician.getPrefWidth());
				
				int i = 0;
				int j = 3;
				for (ScreenView view : views.values()) {
					Label lblUnused = new Label("Alternate Output");
					lblUnused.setFont(Font.font("System", FontWeight.BOLD, 15));
					screenPane.add(view, i, j);
					screenPane.add(lblUnused, i, j + 1);
					GridPane.setHalignment(lblUnused, HPos.CENTER);
					i++;
					if (i % 3 == 0) {
						i = 0;
						j += 2;
					}
				}
			}
		};

		this.setupTree.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
			if (nv != null) {
				switch (nv.getValue().getName()) {
				case GENERAL:
					right.setCenter(vboxGeneral);
					break;
				case DISPLAYS:
					right.setCenter(vboxScreens);
					screenListener.invalidated(obs);
					break;
				case MEDIA:
					right.setCenter(vboxMedia);
					break;
				default:
					break;
				}
			}
		});
		
		// update when the parent changes
		parentProperty().addListener((obs, ov, nv) -> {
			TreeItem<SetupTreeData> selected = this.setupTree.getSelectionModel().getSelectedItem();
			if (nv != null && selected != null && selected.getValue().getName() == DISPLAYS) {
				screenListener.invalidated(obs);
			}
		});
		
		// update when the screens change
		Screen.getScreens().addListener(screenListener);
	}
	
	private ObservableList<Option<Locale>> refreshLocales() {
		List<Option<Locale>> locs = new ArrayList<Option<Locale>>();
		for (Locale loc : Translations.getAvailableLocales()) {
			locs.add(new Option<Locale>(loc.getDisplayName(), loc));
		}
		return FXCollections.observableArrayList(locs);
	}
}
