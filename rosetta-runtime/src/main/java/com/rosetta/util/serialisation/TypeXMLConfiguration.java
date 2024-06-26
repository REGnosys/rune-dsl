/*
 * Copyright 2024 REGnosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rosetta.util.serialisation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TypeXMLConfiguration {
	private final Optional<String> xmlRootElementName;
	private final Optional<Map<String, String>> xmlAttributes;
	private final Optional<Map<String, AttributeXMLConfiguration>> attributes;
	
	@JsonCreator
	public TypeXMLConfiguration(
			@JsonProperty("xmlRootElementName") Optional<String> xmlRootElementName,
			@JsonProperty("xmlAttributes") Optional<Map<String, String>> xmlAttributes,
			@JsonProperty("attributes") Optional<Map<String, AttributeXMLConfiguration>> attributes) {
		this.xmlRootElementName = xmlRootElementName;
		this.xmlAttributes = xmlAttributes;
		this.attributes = attributes;
	}

	public Optional<String> getXmlRootElementName() {
		return xmlRootElementName;
	}

	public Optional<Map<String, String>> getXmlAttributes() {
		return xmlAttributes;
	}

	public Optional<Map<String, AttributeXMLConfiguration>> getAttributes() {
		return attributes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attributes, xmlAttributes, xmlRootElementName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeXMLConfiguration other = (TypeXMLConfiguration) obj;
		return Objects.equals(attributes, other.attributes)
				&& Objects.equals(xmlAttributes, other.xmlAttributes)
				&& Objects.equals(xmlRootElementName, other.xmlRootElementName);
	}
}
