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

package com.regnosys.rosetta.generator.java.scoping;


public class JavaMethodScope extends AbstractJavaScope<JavaClassScope> {
	private JavaStatementScope bodyScope;

	JavaMethodScope(String methodName, JavaClassScope parent) {
		super("Method[" + methodName + "]", parent);
	}

	public JavaStatementScope getBodyScope() {
		if (bodyScope == null) {
			bodyScope = new JavaStatementScope("Body", this);
		}
		return bodyScope;
	}
}
