package org.praisenter.javafx;

import java.sql.SQLException;

import org.controlsfx.control.BreadCrumbBar;
import org.praisenter.javafx.bible.BibleLibraryPane;
import org.praisenter.javafx.bible.BiblePane;
import org.praisenter.javafx.media.MediaLibraryPane;
import org.praisenter.javafx.slide.SlideLibraryPane;
import org.praisenter.javafx.slide.editor.SlideEditorPane;

import javafx.geometry.Orientation;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;

public final class MainPane extends BorderPane {
	
	final PraisenterContext context;
	
	public MainPane(PraisenterContext context) {
		this.context = context;
		
		this.setTop(createMenus());
		
		try {
			this.setCenter(new BiblePane(context));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// TODO menu options
	// TODO translate
	private MenuBar createMenus() {
		MenuBar menu = new MenuBar();
		
		Menu file = new Menu("File");
		Menu media = new Menu("Media");
		Menu songs = new Menu("Songs");
		Menu bibles = new Menu("Bibles");
		Menu slides = new Menu("Slides");
		Menu help = new Menu("Help");
		
		menu.getMenus().addAll(file, media, songs, bibles, slides, help);
		menu.setUseSystemMenuBar(true);
		
		MenuItem fSetup = new MenuItem("Setup");
		MenuItem fNew = new MenuItem("New");
		MenuItem fSave = new MenuItem("Save");
		MenuItem fSaveAs = new MenuItem("Save As...");
		MenuItem fExit = new MenuItem("Exit");
		
		file.getItems().addAll(fSave, fSaveAs, new SeparatorMenuItem(), fSetup, new SeparatorMenuItem(), fExit);
		
		MenuItem mManage = new MenuItem("Manage media");
		MenuItem mImport = new MenuItem("Import media");
		
		// maybe...
		MenuItem mTranscode = new MenuItem("Transcode");
		media.getItems().addAll(mManage, mImport);
		
		// add/edit
		MenuItem soManage = new MenuItem("Manage songs");
		MenuItem soImport = new MenuItem("Import songs");
		MenuItem soNew = new MenuItem("Create a new song");
		// manage
		songs.getItems().addAll(soManage, soImport, soNew);

		MenuItem slManage = new MenuItem("Manage slides");
		MenuItem slNew = new MenuItem("Create a new slide");
		slides.getItems().addAll(slManage, slNew);
		
		MenuItem blManage = new MenuItem("Manage bibles");
		MenuItem blImport = new MenuItem("Import bibles");
		bibles.getItems().addAll(blManage, blImport);
		
		MenuItem hAbout = new MenuItem("About");
		help.getItems().addAll(hAbout);
		
		BreadCrumbBar<Object> bar = new BreadCrumbBar<>();
		
		
		// panes
		
		SetupPane sp = new SetupPane(context.getConfiguration());
		BibleLibraryPane blp = new BibleLibraryPane(context);
		MediaLibraryPane mlp = new MediaLibraryPane(context, Orientation.HORIZONTAL);
		SlideLibraryPane slp = new SlideLibraryPane(context);
		
		// menu actions

		fSetup.setOnAction((e) -> {
			setCenter(sp);
		});
		
		blManage.setOnAction((e) -> {
			setCenter(blp);
		});
		
		mManage.setOnAction((e) -> {
			setCenter(mlp);
		});
		
		slManage.setOnAction((e) -> {
			setCenter(slp);
		});
		
		return menu;
	}
	
	// TODO slide show pane
	// TODO song pane
	// TODO slide pane
	
}
