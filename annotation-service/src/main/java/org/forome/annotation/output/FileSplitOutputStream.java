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

package org.forome.annotation.output;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class FileSplitOutputStream implements Closeable {

	private final Path target;
	private final int limit;

	private int index;
	private BufferedOutputStream activeOutputStream;
	private int countLimit;

	public FileSplitOutputStream(Path target, int limit) throws IOException {
		if (!target.getFileName().toString().endsWith(".gz")) {
			throw new IllegalArgumentException();
		}

		this.target = target;
		this.limit = limit;

		this.index = 0;
		this.activeOutputStream = buildOutputStream(target, index);
		this.countLimit = 0;
	}

	public void writeLine(byte[] b) throws IOException {
		if (countLimit >= limit) {
			close(activeOutputStream);
			activeOutputStream = buildOutputStream(target, ++index);
			countLimit = 0;
		}

		writeLineWithIgnoreLimit(b);
		countLimit++;
	}

	public void writeLineWithIgnoreLimit(byte[] b) throws IOException {
		activeOutputStream.write(b);
		activeOutputStream.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
	}

	private static BufferedOutputStream buildOutputStream(Path target, int index) throws IOException {
		Path path;
		if (index == 0) {
			path = target;
		} else {
			String sourceFileName = target.getFileName().toString();
			int i = sourceFileName.indexOf('.');

			String fileName = sourceFileName.substring(0, i + 1) + index + sourceFileName.substring(i);
			path = target.getParent().resolve(fileName);
		}

		OutputStream os = new GZIPOutputStream(Files.newOutputStream(path));
		return new BufferedOutputStream(os);
	}

	private static void close(OutputStream outputStream) throws IOException {
		outputStream.flush();
		outputStream.close();
	}

	@Override
	public void close() throws IOException {
		close(activeOutputStream);
		activeOutputStream = null;
	}
}
