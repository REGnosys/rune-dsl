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

package com.regnosys.rosetta.tools.modelimport;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportConfig {
	private final String schemaLocation;
	private final ImportTargetConfig target;
	
	@JsonCreator
	public ImportConfig(
			@JsonProperty("schemaLocation") String schemaLocation,
			@JsonProperty("target") ImportTargetConfig target) {
		this.schemaLocation = schemaLocation;
		this.target = target;
	}

	public String getSchemaLocation() {
		return schemaLocation;
	}

	public ImportTargetConfig getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		return Objects.hash(schemaLocation, target);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImportConfig other = (ImportConfig) obj;
		return Objects.equals(schemaLocation, other.schemaLocation) && Objects.equals(target, other.target);
	}
}
