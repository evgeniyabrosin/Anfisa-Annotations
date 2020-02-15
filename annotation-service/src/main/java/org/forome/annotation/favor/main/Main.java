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

package org.forome.annotation.favor.main;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.favor.processing.Processing;
import org.forome.annotation.favor.struct.out.JMetadata;
import org.forome.annotation.favor.utils.iterator.DumpIterator;
import org.forome.annotation.favor.utils.source.Source;
import org.forome.annotation.favor.utils.source.SourceLocal;
import org.forome.annotation.favor.utils.struct.table.Row;
import org.forome.annotation.favor.utils.struct.table.Table;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            ServiceConfig serviceConfig = new ServiceConfig();
            SSHConnectService sshTunnelService = new SSHConnectService();
            DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
            HgmdConnector hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);

//            Source source = new SourceRemote(Source.PATH_FILE);
            Source source = new SourceLocal(Paths.get("data/favor/Filtered[KCNQ1,NF2,SMAD4]_14_Cloud_SQL_Export_2019-12-31.gz"));

            Processing processing = new Processing(hgmdConnector);
            try (InputStream is = source.getInputStream()) {
                try (BufferedReader bf = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)))) {
                    DumpIterator dumpIterator = new DumpIterator(bf);

                    try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(Paths.get("favor_anfisa.json.gz")))) {
                        try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                            bos.write(new JMetadata().toJSON().toJSONString().getBytes(StandardCharsets.UTF_8));
                            bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

                            Table currentTable = null;

                            int line = 0;
                            while (dumpIterator.hasNext()) {
                                line++;
//                                if (line > 100) break;

                                Row row = dumpIterator.next();
                                if (!row.table.equals(currentTable)) {
                                    currentTable = row.table;
                                    System.out.println("Table: " + currentTable.name);
                                    System.out.println(currentTable.fields.stream().map(field -> field.name).collect(Collectors.joining(", ")));
                                }

                                ProcessingResult processingResult = processing.exec(row);

                                bos.write(processingResult.toJSON().toJSONString().getBytes(StandardCharsets.UTF_8));
                                bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

                                if (line % 10 == 0) {
                                    log.debug("Processing line: " + line);
                                }
                            }
                        }
                    }
                }
            }

            source.close();

            System.exit(0);
        } catch (Throwable e) {
            log.error("Exception", e);
            System.exit(1);
        }
    }
}
