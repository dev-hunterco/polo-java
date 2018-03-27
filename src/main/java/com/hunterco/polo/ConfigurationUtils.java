package com.hunterco.polo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationUtils {
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static Object get(Map<String, Object> map, String... keys) {
		Object value = null;
		int idx = 0;
		for(String key : keys) {
			idx++;
			value = map.get(key);
			
			if(idx == keys.length)
				return value;
			
			if(value == null || !(value instanceof Map))
				return null;
			map = (Map) value;
		}
		return value;
	}
	
	public static void set(Map<String, Object> map, Collection<String> keys, Object value) {
		int idx = 0;
		for(String key : keys) {
			idx++;
			
			if(idx == keys.size()) {
				map.put(key, value);
				return;
			}
			
			Object m = map.get(key);
			if(m == null || !(m instanceof Map)) {
				m = new HashMap<>();
				map.put(key, m);
			}
			map = (Map) m;
		}
	}
	
	public static Map<String, Object> loadJson(String json) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(json, Map.class);
	}
	
	public static Map<String, Object> loadJsonFile(File file) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		return mapper.readValue(new FileInputStream(file), Map.class);
	}
}
