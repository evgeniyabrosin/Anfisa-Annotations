package org.forome.annotation.connector.conservation;

import org.forome.annotation.config.connector.ConservationConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

public class ConservationConnector implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(ConservationConnector.class);

    private final DatabaseConnector databaseConnector;

    public ConservationConnector(
            ConservationConfigConnector conservationConfigConnector
    ) throws Exception {
        databaseConnector = new DatabaseConnector(conservationConfigConnector);
    }

    public Conservation getConservation(Chromosome chromosome, Position<Long> position, Position<Optional<Integer>> hg38, String ref, String alt) {
        if (!hg38.start.isPresent() || !hg38.end.isPresent()) {
            return null;
        }

        if (alt.length() == 1 && ref.length() == 1) {
            //Однобуквенный вариант
            Integer hg38s = hg38.start.orElse(null);
            if (!Objects.equals(hg38s, hg38.end.orElse(null))) {
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

    private Conservation getConservation(Chromosome chromosome, Position<Long> position, Position<Optional<Integer>> hg38) {
        String sqlFromGerp;
        String sqlFromConservation;
        if (position.isSingle()) {
            sqlFromGerp = String.format("select GerpN, GerpRS from GERP where Chrom='%s' and Pos = %s", chromosome.getChar(), position.start);
            sqlFromConservation = String.format("select priPhCons, mamPhCons, verPhCons, priPhyloP, mamPhyloP, verPhyloP, " +
                    "GerpRSpval, GerpS from CONSERVATION where Chrom='%s' and Pos = %s", chromosome.getChar(), hg38.start.get());
        } else {
            long hg19Pos1;
            long hg19Pos2;
            long hg38Pos1;
            long hg38Pos2;
            if (position.start > position.end) {
                hg19Pos1 = position.end - 1;
                hg19Pos2 = position.start;

                hg38Pos1 = hg38.end.get() - 1;
                hg38Pos2 = hg38.start.get();
                if (hg38.start.get() <= hg38.end.get()) {
                    throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", chromosome.getChar(), position));
                }
            } else {
                throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", chromosome.getChar(), position));
            }

            sqlFromGerp = String.format("select max(GerpN) as GerpN, max(GerpRS) as GerpRS from GERP " +
                    "where Chrom='%s' and Pos between %s and %s", chromosome.getChar(), hg19Pos1, hg19Pos2);

            sqlFromConservation = String.format("select max(priPhCons) as priPhCons, max(mamPhCons) as mamPhCons, max(verPhCons) as verPhCons, " +
                    "max(priPhyloP) as priPhyloP, max(mamPhyloP) as mamPhyloP, max(verPhyloP) as verPhyloP, " +
                    "max(GerpRSpval) as GerpRSpval, max(GerpS) as GerpS from CONSERVATION " +
                    "where Chrom='%s' and Pos between %s and %s", chromosome.getChar(), hg38Pos1, hg38Pos2);
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


//    public SpliceAIResult getAll(String chromosome, long position, String ref, List<String> altList){
//        return spliceAIDataConnector.getAll(chromosome, position, ref, altList);
//    }

//    public String getSpliceAIDataVersion() {
//        return spliceAIDataConnector.getSpliceAIDataVersion();
//    }

    @Override
    public void close() throws IOException {
        databaseConnector.close();
    }

}
