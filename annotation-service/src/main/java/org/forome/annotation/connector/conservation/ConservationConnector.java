package org.forome.annotation.connector.conservation;

import org.forome.annotation.config.connector.ConservationConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Chromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConservationConnector implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(ConservationConnector.class);

    private final DatabaseConnector databaseConnector;

    public ConservationConnector(
            ConservationConfigConnector conservationConfigConnector
    ) throws Exception {
        databaseConnector = new DatabaseConnector(conservationConfigConnector);
    }

    public Conservation getConservation(Chromosome chromosome, long hg38) {
        String sql = String.format("select priPhCons, mamPhCons, verPhCons, priPhyloP, mamPhyloP, verPhyloP, GerpRS, " +
                "GerpRSpval, GerpN, GerpS from CONSERVATION where Chrom='%s' and Pos = %s", chromosome.getChar(), hg38);
        return getConservation(sql);
    }

    public Conservation getConservation(Chromosome chromosome, long hg38Start, long hg38End) {
        long pos1 = Math.min(hg38Start, hg38End);
        long pos2 = Math.max(hg38Start, hg38End);
        String sql = String.format("select max(priPhCons) as priPhCons, max(mamPhCons) as mamPhCons, max(verPhCons) as verPhCons, " +
                "max(priPhyloP) as priPhyloP, max(mamPhyloP) as mamPhyloP, max(verPhyloP) as verPhyloP, max(GerpRS) as GerpRS, " +
                "max(GerpRSpval) as GerpRSpval, max(GerpN) as GerpN, max(GerpS) as GerpS from CONSERVATION " +
                "where Chrom='%s' and Pos between %s and %s", chromosome.getChar(), pos1, pos2);
        return getConservation(sql);
    }

    private Conservation getConservation(String sql) {
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    if (resultSet.next()) {
                        return new Conservation(
                                (Double) resultSet.getObject("priPhCons"), (Double) resultSet.getObject("mamPhCons"),
                                (Double) resultSet.getObject("verPhCons"), (Double) resultSet.getObject("priPhyloP"),
                                (Double) resultSet.getObject("mamPhyloP"), (Double) resultSet.getObject("verPhyloP"),
                                (Double) resultSet.getObject("GerpRS"), (Double) resultSet.getObject("GerpRSpval"),
                                (Double) resultSet.getObject("GerpN"), (Double) resultSet.getObject("GerpS")
                        );
                    }
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
