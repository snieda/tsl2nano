package de.tsl2.nano.core.update;

import java.io.File;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * base for all tsl2nano env updaters (with names like ENVUpdaterXvYvZ)
 * 
 * @author ts
 */
public interface IVersionRunner {
	
	default String getVersion() {
		return StringUtil.substring(getClass().getSimpleName(), "ENVUpdate", null).replace("v", ".");
	}
	/** will be called before the own update */
	default void runPreviousVersionUpdate(ENV env, String currentVersion) {
		new Updater().run(env.getConfigFile().getPath(), currentVersion, previousVersion(), env);
	}

	/** version number to be run before own update */
	String previousVersion();

	default boolean isUpdated(String envFile, String version) {
		return getVersion().compareTo(version) < 0;
	}

	/**
	 * will directly return, if {@link #isUpdated(String, String)} returns true.
	 * else {@link #runPreviousVersionUpdate(ENV, String)} and {@link #update()}
	 * will be run.
	 */
	default void run(ENV env, String currentVersion) {
		if (isUpdated(env.getConfigFile().getPath(), currentVersion))
			return;
		if (previousVersion() != null)
			runPreviousVersionUpdate(env, currentVersion);
        LogFactory.log("VERSION UPDATE: " + previousVersion() + " -> " + getVersion());
		update(env, currentVersion);
        LogFactory.log("VERSION NOW   : " + getVersion());
	}

	/** own update */
	void update(ENV env, String currentVersion);
}
