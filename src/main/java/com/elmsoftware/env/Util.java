package com.elmsoftware.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class Util {

	private static final Logger log = LoggerFactory.getLogger(Util.class);

	public boolean isBlank(final String string) {
		return null == string || string.trim().isEmpty();
	}

	public void closeQuietly(final Closeable closeable) {
		if (null != closeable) {
			try {
				closeable.close();
			} catch (final IOException e) {
				log.warn("Unable to close: {}", e);
			}
		}
	}

	public void mergeProperties(
			final String environment,
			final Map<String, String> mergeTo,
			final String localResourceName,
			final EnvironmentSettings mergeFrom
	) {
		if (null != mergeFrom) {
			log.info("adding local properties from {}", localResourceName);
			final Map<String, String> localProperties = mergeFrom.merge(environment);
			for (final String localKey : localProperties.keySet()) {
				final String value = localProperties.get(localKey);
				log.debug("replacing shared property '{}' with value of '{}' (was '{}')", localKey, value, mergeTo.get(localKey));
				mergeTo.put(localKey, value);
			}
		}
	}

	public String determineEnvironment(final String systemVariableName) {

		final String environmentName;

		final String systemEnvironmentValue = System.getenv(systemVariableName);
		log.trace("system variable {} is {}", systemVariableName, systemEnvironmentValue);

		if (isBlank(systemEnvironmentValue)) {
			environmentName = System.getProperty("environment", "LOCAL");
		} else {
			environmentName = systemEnvironmentValue;
		}
		log.trace("environment name is {}", environmentName);

		return environmentName;

	}
}
