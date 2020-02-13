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

package org.forome.annotation.favor.main.copy;

import com.google.common.collect.ImmutableList;
import org.forome.annotation.favor.utils.iterator.DumpIterator;
import org.forome.annotation.favor.utils.source.Source;
import org.forome.annotation.favor.utils.source.SourceLocal;
import org.forome.annotation.favor.utils.struct.table.Row;
import org.forome.annotation.favor.utils.struct.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
//            Source source = new SourceRemote(Source.PATH_REMOTE_FILE);
            Source source = new SourceLocal(Paths.get(Source.PATH_FILE));
            try (InputStream is = source.getInputStream()) {
                try (BufferedReader bf = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)))) {
                    DumpIterator dumpIterator = new DumpIterator(bf);

                    List<String> genes = ImmutableList.of("KCNQ1", "NF2", "SMAD4");
                    String outFilename = "Filtered[" + String.join(",", genes) + "]_Cloud_SQL_Export_2019-12-31.gz";
                    try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(Paths.get(outFilename)))) {
                        try (BufferedOutputStream bos = new BufferedOutputStream(os)) {

                            int countLineProcessed = 0;
                            int countFind = 0;

                            Table table = null;
                            while (dumpIterator.hasNext()) {
                                Row row = dumpIterator.next();
                                if (!"gds".equals(row.table.name)) {
                                    continue;
                                }

                                if (table == null) {
                                    table = row.table;
                                    bos.write(row.table.rawLine.getBytes(StandardCharsets.UTF_8));
                                    bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                                    bos.flush();
                                }

                                String geneName = row.getValue("GeneName");
                                if (genes.contains(geneName)) {
                                    bos.write(row.rawLine.getBytes(StandardCharsets.UTF_8));
                                    bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                                    bos.flush();

                                    countFind++;
                                }

                                countLineProcessed++;
                                if (countLineProcessed % 10000 == 0) {
                                    log.debug("Processing {}|{}", countFind, countLineProcessed);
                                }
                            }
                        }
                    }
                }
            }
            source.close();

            System.exit(0);
        } catch (
                Throwable e) {
            log.error("Exception", e);
            System.exit(1);
        }
    }
}
