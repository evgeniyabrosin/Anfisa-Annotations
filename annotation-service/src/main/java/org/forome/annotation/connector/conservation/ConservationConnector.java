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

package org.forome.annotation.connector.conservation;

import org.forome.annotation.config.connector.ConservationConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ConservationConnector implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(ConservationConnector.class);

    private final DatabaseConnector databaseConnector;

    public ConservationConnector(
            DatabaseConnectService databaseConnectService,
            ConservationConfigConnector conservationConfigConnector
    ) throws Exception {
        databaseConnector = new DatabaseConnector(databaseConnectService, conservationConfigConnector);
    }

    public List<DatabaseConnector.Metadata> getMetadata(){
        return databaseConnector.getMetadata();
    }

    public Conservation getConservation(Chromosome chromosome, Position<Integer> position, Position<Integer> hg38, String ref, String alt) {
        if (alt.length() == 1 && ref.length() == 1) {
            //Однобуквенный вариант
            if (hg38 != null && !hg38.isSingle()) {
                throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", chromosome.getChar(), position));
            }
            return getConservation(chromosome, position, hg38);
        } else if (alt.length() > 1 && ref.length() == 1) {
            //Инсерция
            return getConservation(chromosome, position, hg38);
        } else {
            return null;
        }
    }

    private Conservation getConservation(Chromosome chromosome, Position<Integer> position, Position<Integer> hg38) {
        String sqlFromGerp;
        String sqlFromConservation = null;
        if (position.isSingle()) {
            sqlFromGerp = String.format("select GerpN, GerpRS from conservation.GERP where Chrom='%s' and Pos = %s",
                    chromosome.getChar(), position.start
            );

            if (hg38 != null) {
                sqlFromConservation = String.format("select priPhCons, mamPhCons, verPhCons, priPhyloP, mamPhyloP, " +
                                "verPhyloP, GerpRSpval, GerpS from conservation.CONSERVATION where Chrom='%s' and Pos = %s",
                        chromosome.getChar(), hg38.start
                );
            }
        } else {
            long hg19Pos1;
            long hg19Pos2;
            int hg38Pos1 = Integer.MIN_VALUE;
            int hg38Pos2 = Integer.MIN_VALUE;
            if (position.start > position.end) {
                hg19Pos1 = position.end - 1;
                hg19Pos2 = position.start;

                if (hg38 != null) {
                    hg38Pos1 = hg38.end - 1;
                    hg38Pos2 = hg38.start;
                    if (hg38.start <= hg38.end) {
                        throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s, hg38: %s", chromosome.getChar(), position, hg38));
                    }
                }
            } else {
                throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", chromosome.getChar(), position));
            }

            sqlFromGerp = String.format("select max(GerpN) as GerpN, max(GerpRS) as GerpRS from conservation.GERP " +
                    "where Chrom='%s' and Pos between %s and %s", chromosome.getChar(), hg19Pos1, hg19Pos2);

            if (hg38 != null) {
                if ( hg38Pos1 == Integer.MIN_VALUE || hg38Pos2 == Integer.MIN_VALUE) {
                    throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", chromosome.getChar(), position));
                }
                sqlFromConservation = String.format("select max(priPhCons) as priPhCons, max(mamPhCons) as mamPhCons, " +
                                "max(verPhCons) as verPhCons, max(priPhyloP) as priPhyloP, max(mamPhyloP) as mamPhyloP, " +
                                "max(verPhyloP) as verPhyloP, max(GerpRSpval) as GerpRSpval, max(GerpS) as GerpS " +
                                "from conservation.CONSERVATION where Chrom='%s' and Pos between %s and %s",
                        chromosome.getChar(), hg38Pos1, hg38Pos2
                );
            }
        }
        return getConservation(sqlFromGerp, sqlFromConservation);
    }

    private Conservation getConservation(String sqlFromGerp, String sqlFromConservation) {
        Double priPhCons = null;
        Double mamPhCons = null;
        Double verPhCons = null;
        Double priPhyloP = null;
        Double mamPhyloP = null;
        Double verPhyloP = null;
        Double gerpRS = null;
        Double gerpRSpval = null;
        Double gerpN = null;
        Double gerpS = null;

        boolean success = false;
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sqlFromGerp)) {
                    if (resultSet.next()) {
                        gerpN = (Double) resultSet.getObject("GerpN");
                        gerpRS = (Double) resultSet.getObject("GerpRS");
                        success = true;
                    }
                }
                if (sqlFromConservation != null) {
                    try (ResultSet resultSet = statement.executeQuery(sqlFromConservation)) {
                        if (resultSet.next()) {
                            priPhCons = (Double) resultSet.getObject("priPhCons");
                            mamPhCons = (Double) resultSet.getObject("mamPhCons");
                            verPhCons = (Double) resultSet.getObject("verPhCons");
                            priPhyloP = (Double) resultSet.getObject("priPhyloP");
                            mamPhyloP = (Double) resultSet.getObject("mamPhyloP");
                            verPhyloP = (Double) resultSet.getObject("verPhyloP");
                            gerpRSpval = (Double) resultSet.getObject("GerpRSpval");
                            gerpS = (Double) resultSet.getObject("GerpS");
                            success = true;
                        }
                    }
                }
                if (success) {
                    return new Conservation(
                            priPhCons, mamPhCons,
                            verPhCons, priPhyloP,
                            mamPhyloP, verPhyloP,
                            gerpRS, gerpRSpval,
                            gerpN, gerpS
                    );
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return null;
    }

    @Override
    public void close() {
        databaseConnector.close();
    }

}
