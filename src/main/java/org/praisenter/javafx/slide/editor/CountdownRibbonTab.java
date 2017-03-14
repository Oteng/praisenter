package org.praisenter.javafx.slide.editor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.praisenter.javafx.slide.ObservableCountdownComponent;
import org.praisenter.javafx.slide.ObservableSlideRegion;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

class CountdownRibbonTab extends ComponentEditorRibbonTab {
	private final DateTimePicker pkrCountdownTime;
	private final ComboBox<String> cmbCountdownFormat;
	
	private final List<Pair<String, String>> formatMapping;
	
	public CountdownRibbonTab() {
		super("Countdown");

		formatMapping = new ArrayList<Pair<String, String>>();
		formatMapping.add(new Pair<String, String>("YY", "%1$02d"));
		formatMapping.add(new Pair<String, String>("MM", "%2$02d"));
		formatMapping.add(new Pair<String, String>("DD", "%3$02d"));
		formatMapping.add(new Pair<String, String>("hh", "%4$02d"));
		formatMapping.add(new Pair<String, String>("mm", "%5$02d"));
		formatMapping.add(new Pair<String, String>("ss", "%6$02d"));
		formatMapping.add(new Pair<String, String>("Y", "%1$01d"));
		formatMapping.add(new Pair<String, String>("M", "%2$01d"));
		formatMapping.add(new Pair<String, String>("D", "%3$01d"));
		formatMapping.add(new Pair<String, String>("h", "%4$01d"));
		formatMapping.add(new Pair<String, String>("m", "%5$01d"));
		formatMapping.add(new Pair<String, String>("s", "%6$01d"));
		
		ObservableList<String> countdownFormats = FXCollections.observableArrayList();
		countdownFormats.add("YY:MM:DD:hh:mm:ss");
		countdownFormats.add("MM:DD:hh:mm:ss");
		countdownFormats.add("DD:hh:mm:ss");
		countdownFormats.add("hh:mm:ss");
		countdownFormats.add("mm:ss");
		countdownFormats.add("ss");
		
		this.cmbCountdownFormat = new ComboBox<String>(countdownFormats);
		this.cmbCountdownFormat.setPrefWidth(175);
		this.cmbCountdownFormat.setEditable(true);
		this.cmbCountdownFormat.setValue(countdownFormats.get(2));
		
		this.pkrCountdownTime = new DateTimePicker();
		
		// layout
		
		HBox row1 = new HBox(2, this.pkrCountdownTime);
		HBox row2 = new HBox(2, this.cmbCountdownFormat);

		VBox layout = new VBox(2, row1, row2);
		
		this.container.setCenter(layout);
		
		// events
		this.managedProperty().bind(this.visibleProperty());
		
		this.cmbCountdownFormat.getEditor().textProperty().addListener((obs, ov, nv) -> {
			if (mutating) return;
			String format = getFormat(nv);
			ObservableSlideRegion<?> comp = this.component.get();
			if (comp != null && comp instanceof ObservableCountdownComponent) {
				ObservableCountdownComponent otc = (ObservableCountdownComponent)comp;
				otc.setCountdownFormat(format);
			}
		});
		
		this.component.addListener((obs, ov, nv) -> {
			mutating = true;
			if (nv != null && nv instanceof ObservableCountdownComponent) {
				ObservableCountdownComponent otc = (ObservableCountdownComponent)nv;
				String format = fromPattern(otc.getCountdownFormat());
				this.cmbCountdownFormat.setValue(format);
				this.pkrCountdownTime.setValue(otc.getCountdownTarget());
				this.setVisible(true);
			} else {
				this.cmbCountdownFormat.setValue(countdownFormats.get(2));
				this.pkrCountdownTime.setValue(LocalDateTime.now());
				this.setVisible(false);
			}
			mutating = false;
		});
		
		this.pkrCountdownTime.valueProperty().addListener((obs, ov, nv) -> {
			if (mutating) return;
			ObservableSlideRegion<?> comp = this.component.get();
			if (comp != null && comp instanceof ObservableCountdownComponent) {
				ObservableCountdownComponent otc = (ObservableCountdownComponent)comp;
				otc.setCountdownTarget(nv);
			}
		});
	}
	
	private String getFormat(String format) {
		if (format != null && format.trim().length() > 0) {
			return toPattern(format);
		}
		return null;
	}
	
	private String toPattern(String format) {
		String fmt = format;
		for (Pair<String, String> pair : formatMapping) {
			fmt = fmt.replaceAll(pair.getKey(), Matcher.quoteReplacement(pair.getValue()));
		}
		return fmt;
	}
	
	private String fromPattern(String pattern) {
		String fmt = pattern;
		for (Pair<String, String> pair : formatMapping) {
			fmt = fmt.replaceAll(Pattern.quote(pair.getValue()), pair.getKey());
		}
		return fmt;
	}
}
