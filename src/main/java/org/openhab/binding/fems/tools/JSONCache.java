/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Stack;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Cache JSONObjects in files
 */
public class JSONCache {	
	private static final Stack<Path> stack = new Stack<Path>();
	private static final String cacheFilePrefix = "cache.";
	private static final Charset defaultCharset = StandardCharsets.ISO_8859_1;
	private Logger logger = LoggerFactory.getLogger(JSONCache.class);
			
	public JSONCache() {
		// Load all cached paths on start up
		try (DirectoryStream<Path> cacheFiles = Files.newDirectoryStream(
				Paths.get(System.getProperty("user.dir")), cacheFilePrefix + '*')) {
		    for(Path cacheFile : cacheFiles) {
		    	stack.push(cacheFile);
		    }
		} catch(Exception e) {
			logger.error("Unable to load cached files: " + e.getMessage());
		}
	}

	public synchronized JSONObject pop() {
		Path cacheFile = stack.pop();
    	String jsonString = "";
    	JSONObject jsonObject = null;
    	try {
			for(String line : Files.readAllLines(cacheFile, defaultCharset)) {
				jsonString += line;
			}
			jsonObject = new JSONObject(jsonString);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			Files.delete(cacheFile);
		} catch (IOException e) {
			logger.error("Could not delete temporary file " + cacheFile + ": " + e.getMessage());
		}
		return jsonObject;
	}

	public synchronized void push(JSONObject obj) {
		try {
			File cacheFile = File.createTempFile(cacheFilePrefix, "", Paths.get(System.getProperty("user.dir")).toFile());
			stack.push(cacheFile.toPath());
			Files.write(cacheFile.toPath(), obj.toString().getBytes(),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.SYNC);
		} catch (IOException e) {
			logger.error("Unable to cache " + obj.toString() + ": " + e.getMessage());
		}
	}
	
	public synchronized boolean isEmpty() {
		return stack.isEmpty();
	}
}