package org.praisenter.ui;

import org.praisenter.data.DataManager;
import org.praisenter.data.configuration.Configuration;

public interface ReadOnlyPraisenterContext {
	public DataManager getDataManager();
	public Configuration getConfiguration();
	public ApplicationState getApplicationState();
}