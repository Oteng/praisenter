package org.praisenter.data.slide.text;

import java.time.LocalDateTime;

import org.praisenter.data.Copyable;
import org.praisenter.data.Identifiable;
import org.praisenter.data.slide.ReadonlySlideComponent;
import org.praisenter.data.slide.ReadonlySlideRegion;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;

public interface ReadonlyCountdownComponent extends ReaonlyTextComponent, ReadonlySlideComponent, ReadonlySlideRegion, Copyable, Identifiable {
	public LocalDateTime getCountdownTarget();
	public boolean isCountdownTimeOnly();
	public String getCountdownFormat();
	
	public ReadOnlyObjectProperty<LocalDateTime> countdownTargetProperty();
	public ReadOnlyBooleanProperty countdownTimeOnlyProperty();
	public ReadOnlyStringProperty countdownFormatProperty();
}
