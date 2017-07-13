package i5.las2peer.tools.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import i5.las2peer.logging.L2pLogger;

public class ConfigFile {

	private final L2pLogger logger = L2pLogger.getInstance(ConfigFile.class);
	private final HashMap<String, ConfigSection> sections = new HashMap<>();

	public ConfigFile() {
	}

	public ConfigFile(String filename) throws IOException {
		try {
			FileInputStream fis = new FileInputStream(filename);
			setFromInput(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			logger.info("Config file '" + filename + "' not found. No values preset.");
		}
	}

	public ConfigFile(InputStream is) throws IOException {
		setFromInput(is);
	}

	private void setFromInput(InputStream is) throws IOException {
		ConfigSection currentSection = getSection(null); // default global section
		BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.matches("\\[.+\\]")) { // section
				String sectionName = line.substring(1, line.length() - 1);
				logger.fine("section '" + sectionName + "' found");
				currentSection = getSection(sectionName);
			} else if (line.matches(".+[^\\\\]=.+")) { // value
				int sepInd = line.indexOf("=");
				String key = line.substring(0, sepInd).trim();
				String value = line.substring(sepInd + 1).trim();
				currentSection.put(key, value);
				String nameString = currentSection.name == null ? "**none**" : "'" + currentSection.name + "'";
				logger.fine(
						"keyed value found, section: " + nameString + ", key: '" + key + "', value: '" + value + "'");
			} else if (!line.isEmpty()) {
				String value = line.replace("\\=", "="); // unescape '='
				currentSection.add(value);
				String nameString = currentSection.name == null ? "**none**" : "'" + currentSection.name + "'";
				logger.fine("value found, section: " + nameString + ", value: '" + value + "'");
			}
		}
	}

	private ConfigSection getSection(String sectionName) {
		ConfigSection result = sections.get(sectionName);
		if (result == null) {
			result = new ConfigSection(sectionName);
			sections.put(sectionName, result);
		}
		return result;
	}

	/**
	 * If value is {@code null}, nothing is addded
	 * 
	 * If key is empty or {@code null}, just the value is added
	 * 
	 * If a value for the given key already exists, the value is overwritten
	 * 
	 * If the value has type {@linkplain java.lang.Iterable}, all elements are added to the section with key as name
	 * 
	 * An existing section with that name is overwritten, except no name is given (global section)!
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value) {
		if (value == null) {
			return; // nothing to do here
		}
		if (value instanceof Iterable<?>) { // key is the section name, for the bunch of values
			ConfigSection section;
			if (key == null || key.isEmpty()) {
				section = getSection(null);
			} else {
				section = new ConfigSection(key); // overwrite existing section
				sections.put(key, section);
			}
			section.add(value);
		} else {
			getSection(null).put(key, value);
		}
	}

	/**
	 * Gets a keyed value from the global section.
	 * 
	 * @param key The key to identify the value.
	 * @return Returns the value or {@code null}, if the key is unknown.
	 */
	public String get(String key) {
		return get(null, key);
	}

	/**
	 * Gets a keyed value from the given section.
	 * 
	 * @param sectionName The name of the section.
	 * @param key The key to identify the value.
	 * @return Returns the value or {@code null}, if the key is unknown.
	 */
	public String get(String sectionName, String key) {
		if (key == null) {
			throw new NullPointerException("value key must not be null");
		}
		return getSection(sectionName).keyValues.get(key);
	}

	/**
	 * Gets all non keyed values from the global section.
	 * 
	 * @return Returns a list of values or an empty list.
	 */
	public List<String> getAll() {
		return getAll(null);
	}

	/**
	 * Gets all non keyed values from the given section.
	 * 
	 * @param sectionName The name of the section.
	 * @return Returns a list of values or {@code null}, if no such section exists.
	 */
	public List<String> getAll(String sectionName) {
		ConfigSection section = sections.get(sectionName);
		if (section == null) {
			return null;
		} else {
			return new ArrayList<>(section.values);
		}
	}

	/**
	 * Writes this configuration to the given output stream.
	 * 
	 * @param os The output stream to write to.
	 * @throws IOException If writing to the stream fails.
	 */
	public void store(OutputStream os) throws IOException {
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(os));
		// write global section first
		getSection(null).write(br);
		// write other sections
		for (ConfigSection section : sections.values()) {
			if (section.name == null || section.name.isEmpty()) { // skip global section
				continue;
			}
			section.write(br);
		}
		br.close();
	}

	/**
	 * Checks if a section with the given name exists.
	 * 
	 * @param sectionName The section name to check.
	 * @return Returns {@code true}, if a section with the given name exists, false otherwise.
	 */
	public boolean hasSection(String sectionName) {
		return sections.containsKey(sectionName);
	}

	private class ConfigSection {

		public final String name;
		public final HashMap<String, String> keyValues = new HashMap<>();
		public final ArrayList<String> values = new ArrayList<>();

		public ConfigSection(String name) {
			this.name = name;
		}

		public void add(Object value) {
			if (value instanceof Iterable<?>) {
				for (Object v : (Iterable<?>) value) {
					values.add(valToString(v));
				}
			} else {
				values.add(valToString(value));
			}
		}

		public void put(String key, Object value) {
			if (value instanceof Iterable<?>) {
				throw new IllegalArgumentException("Iterable not expected");
			}
			String strValue = valToString(value);
			if (key == null || key.isEmpty()) {
				values.add(strValue);
			} else {
				keyValues.put(key, strValue);
			}
		}

		private String valToString(Object value) {
			if (value instanceof String) {
				return (String) value;
			} else {
				return value.toString();
			}
		}

		public void write(BufferedWriter br) throws IOException {
			if (name != null && !name.isEmpty()) {
				br.write("[" + name + "]\n");
			}
			for (Entry<String, String> keyValueEntry : keyValues.entrySet()) {
				br.write(keyValueEntry.getKey() + " = " + keyValueEntry.getValue() + "\n");
			}
			for (String value : values) {
				br.write(value.replace("=", "\\=") + "\n"); // escape '='
			}
			br.write("\n"); // empty line at the end of each section
		}

	}

}
