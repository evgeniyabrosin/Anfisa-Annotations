/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.exception;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

public class AnnotatorException extends RuntimeException {

	private final String code;
	private final Map<String, Object> parameters;
	private final String comment;

	AnnotatorException(String code, String comment, Map<String, Object> parameters, Throwable cause) {
		super(
				buildMessage(code, parameters, comment),
				cause
		);

		if (StringUtils.isEmpty(code)) {
			throw new IllegalArgumentException();
		}

		this.code = code;
		this.parameters = parameters == null ? null : Collections.unmodifiableMap(parameters);
		this.comment = comment;
	}

	public String getCode() {
		return code;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public String getComment() {
		return comment;
	}

	private static String buildMessage(String code, Map<String, Object> parameters, String comment) {
		StringJoiner builder = new StringJoiner(", ");
		if (comment != null) builder.add(comment);
		builder.add("code=" + code);
		if (parameters != null) builder.add("parameters=" + parameters);
		return builder.toString();
	}
}
