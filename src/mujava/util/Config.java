package mujava.util;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.SystemUtils;

/**
 * This class allows access to a configuration loaded from a properties file
 * but also allows access to some values that either doesn't belong to the
 * properties file (e.g.: file separator) and properties that need arguments to
 * from an usable value (e.g.: output dir and compilation sandbox that need the
 * file separator  
 * 
 * @author Simón Emmanuel Gutiérrez Brida
 * @version 2.0b
 */
public class Config {
	
	//TODO: document
	public static enum Config_key {
		
		//DIRECTORIES
		ORIGINAL_SOURCE_DIR {
			public String getKey() {
				return "path.original.source";
			}
			public String getFlag() {
				return "-O";
			}
		},
		ORIGINAL_BIN_DIR {
			public String getKey() {
				return "path.original.bin";
			}
			public String getFlag() {
				return "-B";
			}
		},
		TESTS_BIN_DIR {
			public String getKey() {
				return "path.tests.bin";
			}
			public String getFlag() {
				return "-T";
			}
		},
		MUTANTS_DIR {
			public String getKey() {
				return "path.mutants";
			}
			public String getFlag() {
				return "-M";
			}
		},
		//DIRECTORIES
		//MUTATION BASIC
		CLASS {
			public String getKey() {
				return "mutation.basic.class";
			}
			public String getFlag() {
				return "-m";
			}
		},
		METHODS {
			public String getKey() {
				return "mutation.basic.methods";
			}
			public String getFlag() {
				return "-s";
			}
		},
		OPERATORS {
			public String getKey() {
				return "mutation.basic.operators";
			}
			public String getFlag() {
				return "-x";
			}
		},
		MUTATION_SCORE {
			public String getKey() {
				return "mutation.basic.mutationScore";
			}
			public String getFlag() {
				return "-S";
			}
		},
		TESTS {
			public String getKey() {
				return "mutation.basic.tests";
			}
			public String getFlag() {
				return "-t";
			}
		},
		//MUTATION BASIC
		//MUTATION ADVANCED
		BANNED_METHODS {
			public String getKey() {
				return "mutation.advanced.bannedMethods";
			}
			public String getFlag() {
				return "-N";
			}
		},
		BANNED_FIELDS {
			public String getKey() {
				return "mutation.advanced.bannedFields";
			}
			public String getFlag() {
				return "-J";
			}
		},
		MUTGENLIMIT {
			public String getKey() {
				return "mutation.advanced.ignoreMutGenLimit";
			}
			public String getFlag() {
				return "-A";
			}
		},
		ALLOW_FIELD_MUTATIONS {
			public String getKey() {
				return "mutation.advanced.allowFieldMutations";
			}
			public String getFlag() {
				return "-F";
			}
		},
		ALLOW_CLASS_MUTATIONS {
			public String getKey() {
				return "mutation.advanced.allowClassMutations";
			}
			public String getFlag() {
				return "-C";
			}
		},
		ALLOWED_PACKAGES_TO_RELOAD {
			public String getKey() {
				return "mutation.advanced.allowedPackagesToReload";
			}
			public String getFlag() {
				return "-P";
			}
		},
		ALLOW_NUMERIC_LITERAL_VARIATIONS {
			public String getKey() {
				return "mutation.advanced.allowNumericLiteralVariations";
			}
			public String getFlag() {
				return "-L";
			}
		};
		//MUTATION ADVANCED

		public abstract String getKey();
		public abstract String getFlag();
	};
	
	/**
	 * The path to a default .properties file
	 */
	public static final String DEFAULT_PROPERTIES = "default.properties";
	
	/**
	 * The {@code StrykerConfig} instance that will be returned by {@link Config#getInstance(String)}
	 */
	private static Config instance = null;

	/**
	 * The properties file that will be loaded
	 */
	private String propertiesFile;
	/**
	 * The configuration loaded, especified by {@link Config#propertiesFile}
	 */
	private Configuration config;
	
	/**
	 * Gets an instance of {@code StrykerConfig}
	 * 
	 * @param configFile	:	the properties file that will be loaded	:	{@code String}
	 * @return an instance of {@code StrykerConfig} that uses {@code configFile} to load a configuration
	 * @throws IllegalStateException if an instance is already built and this method is called with a different config file
	 */
	public static Config getInstance(String configFile) throws IllegalStateException {
		if (instance != null) {
			if (instance.propertiesFile.compareTo(configFile) != 0) {
				throw new IllegalStateException("Config instance is already built using config file : " + instance.propertiesFile);
			}
		} else {
			instance = new Config(configFile);
		}
		return instance;
	}
	
	/**
	 * @return a previously built instance or construct a new instance using {@code StrykerConfig#DEFAULT_PROPERTIES}
	 */
	public static Config getInstance() {
		if (instance == null) {
			instance = new Config(Config.DEFAULT_PROPERTIES);
		}
		return instance;
	}
	
	/**
	 * Private constructor
	 * 
	 * This will set the value of {@link Config#propertiesFile} and will load the configuration
	 * 
	 * @param configFile	:	the properties file that will be loaded	:	{@code String}
	 */
	private Config(String configFile) {
		this.propertiesFile = configFile;
		this.config = null;
		loadConfig();
	}

	/**
	 * loads the configuration defined in {@link Config#propertiesFile}
	 */
	private void loadConfig() {
		try {
			this.config = new PropertiesConfiguration(this.propertiesFile);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the configuration especified by {@link Config#propertiesFile}
	 */
	public Configuration getConfiguration() {
		return this.config;
	}
	
	/**
	 * @return the file separator for the current os (e.g.: "/" for unix)
	 */
	public String getFileSeparator() {
		return SystemUtils.FILE_SEPARATOR;
	}
	
	/**
	 * @return the path separator for the current os (e.g.: ":" for unix)
	 */
	public String getPathSeparator() {
		return SystemUtils.PATH_SEPARATOR;
	}
	
	public String[] configAsArgs() {
		List<String> args = new LinkedList<String>();
		Config_key[] configKeys = Config_key.values();
		for (Config_key ck : configKeys) {
			if (argumentExist(ck)) {
				if (isBooleanKey(ck)) {
					if (getBooleanArgument(ck)) args.add(ck.getFlag());
				} else {
					args.add(ck.getFlag());
					for (String arg : this.stringArgumentsAsArray(getStringArgument(ck))) {
						args.add(arg);
					}
				}
			}
		}
		return args.toArray(new String[args.size()]);
	}
	
	//TODO: comment
	public String getStringArgument(Config_key key) {
		if (!argumentExist(key) || isBooleanKey(key)) {
			return "";
		}
		return this.config.getString(key.getKey());
	}
	
	//TODO: comment
	public boolean getBooleanArgument(Config_key key) {
		if (!argumentExist(key) || !isBooleanKey(key)) return false;
		return this.config.getBoolean(key.getKey());
	}
	
	//TODO: comment
	public boolean argumentExist(Config_key key) {
		if (this.config.containsKey(key.getKey())) {
			if (isBooleanKey(key)) {
				try {
					this.config.getBoolean(key.getKey());
					return true;
				} catch (ConversionException ex) {
					return false;
				}
			} else {
				return !this.config.getString(key.getKey()).trim().isEmpty();
			}
		} else {
			return false;
		}
	}
	
	public boolean isBooleanKey(Config_key key) {
		switch (key) {
			case ALLOWED_PACKAGES_TO_RELOAD: return false;
			case ALLOW_CLASS_MUTATIONS:
			case ALLOW_FIELD_MUTATIONS:
			case ALLOW_NUMERIC_LITERAL_VARIATIONS: return true;
			case BANNED_FIELDS:
			case BANNED_METHODS:
			case CLASS:
			case METHODS:
			case MUTANTS_DIR: return false;
			case MUTATION_SCORE:
			case MUTGENLIMIT: return true;
			case OPERATORS:
			case ORIGINAL_BIN_DIR:
			case ORIGINAL_SOURCE_DIR:
			case TESTS: 
			case TESTS_BIN_DIR: return false;
			default : return false;
		}
	}
	
	public String[] stringArgumentsAsArray(String arguments) {
		return arguments.split(" ");
	}
	
}
