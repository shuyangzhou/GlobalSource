/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.netbeansproject.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shuyang Zhou
 */
public class ArgumentsUtil {

	public static Map<String, String> parseArguments(String[] args) {
		Map<String, String> arguments = new HashMap<>();

		for (String arg : args) {
			int pos = arg.indexOf('=');

			if (pos <= 0) {
				throw new IllegalArgumentException("Bad argument " + arg);
			}

			String key = arg.substring(0, pos);
			String value = arg.substring(pos + 1);

			arguments.put(key.trim(), value.trim());
		}

		return arguments;
	}

}