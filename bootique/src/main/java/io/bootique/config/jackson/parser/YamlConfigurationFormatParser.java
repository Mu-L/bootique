/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.config.jackson.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import io.bootique.jackson.JacksonService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.inject.Inject;

/**
 * @since 2.0
 */
public class YamlConfigurationFormatParser implements ConfigurationFormatParser {

	private final YAMLFactory yamlFactory;
	private final ObjectMapper mapper;

	@Inject
	public YamlConfigurationFormatParser(JacksonService jackson) {
		this.mapper = jackson.newObjectMapper();
		this.yamlFactory = new YAMLFactory();
	}

	@Override
	public JsonNode parse(InputStream stream) {
		try {
			YAMLParser parser = yamlFactory.createParser(stream);
			return mapper.readTree(parser);
		} catch (IOException e) {
			throw new RuntimeException("Error reading config data", e);
		}
	}

	@Override
	public boolean shouldParse(URL url, String contentType) {
		// TODO: there's no official MIME type yet for YAML
		return  "application/x-yaml".equals(contentType)
				|| url.getPath().endsWith(".yml")
				|| url.getPath().endsWith(".yaml");
	}
}
