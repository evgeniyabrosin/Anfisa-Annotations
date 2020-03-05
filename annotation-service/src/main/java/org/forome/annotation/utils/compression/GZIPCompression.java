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

package org.forome.annotation.utils.compression;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class GZIPCompression {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	public static byte[] compress(final String stringToCompress) {
		if (Objects.isNull(stringToCompress) || stringToCompress.length() == 0) {
			return null;
		}

		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 final GZIPOutputStream gzipOutput = new GZIPOutputStream(baos)) {
			gzipOutput.write(stringToCompress.getBytes(CHARSET));
			gzipOutput.finish();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Error while compression!", e);
		}
	}

	public static String decompress(final byte[] compressed) {
		if (Objects.isNull(compressed) || compressed.length == 0) {
			return null;
		}

		try (final GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(compressed));
			 final StringWriter stringWriter = new StringWriter()) {
			IOUtils.copy(gzipInput, stringWriter, CHARSET.name());
			return stringWriter.toString();
		} catch (IOException e) {
			throw new UncheckedIOException("Error while decompression!", e);
		}
	}

}
