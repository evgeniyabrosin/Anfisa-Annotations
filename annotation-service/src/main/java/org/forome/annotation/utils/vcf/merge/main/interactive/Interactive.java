/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.utils.vcf.merge.main.interactive;

import java.util.Scanner;

public class Interactive {

	private final Scanner scanner;

	public Interactive() {
		scanner = new Scanner(System.in);
	}

	public boolean questionBool(String question) {
		while (true) {
			System.out.println(question + " (Y/N)");
			String response = scanner.next().trim().toLowerCase();
			if ("y".equals(response)) {
				return true;
			} else if ("n".equals(response)) {
				return false;
			}
		}
	}
}
