package org.forome.annotation.service.vcf;

import net.minidev.json.JSONObject;
import org.forome.annotation.Main;
import org.forome.annotation.annotator.input.VCFFileIterator;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaInput;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.old.GnomadConnectorOld;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.variant.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

public class VCFMain {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = new ServiceConfig();
        SSHConnectService sshTunnelService = new SSHConnectService();
        DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService);

        GnomadConnector gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
//        GnomadConnector gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
        SpliceAIConnector spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
        ConservationConnector conservationConnector = new ConservationConnector(databaseConnectService, serviceConfig.conservationConfigConnector);
        HgmdConnector hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);
        ClinvarConnector clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);
        LiftoverConnector liftoverConnector = new LiftoverConnector();
        GTFConnector gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> crash(e));
        EnsemblVepService ensemblVepService = new EnsemblVepExternalService((t, e) -> crash(e));
        AnfisaConnector anfisaConnector = new AnfisaConnector(
                gnomadConnector,
                spliceAIConnector,
                conservationConnector,
                hgmdConnector,
                clinvarConnector,
                liftoverConnector,
                gtfConnector
        );

        Path pathVcf = Paths.get("/home/kris/processtech/tmp/newvcf/HG002_GRCh37_GIAB_highconf_CG-IllFB-IllGATKHC-Ion-10X-SOLID_CHROM1-22_v.3.3.2_highconf_triophased.vcf");
        VCFFileIterator vcfFileIterator = new VCFFileIterator(pathVcf);

        while (true) {
            try {
                Variant variant = vcfFileIterator.next();
                JSONObject vepJson = ensemblVepService.getVepJson(variant, "-").get();
                AnfisaInput anfisaInput = new AnfisaInput.Builder().build();
                AnfisaResult anfisaResult = anfisaConnector.build(anfisaInput, variant, vepJson);
                log.debug("anfisaResult: " + anfisaResult);
            } catch (NoSuchElementException e) {
                break;
            }
        }

        log.debug("end");
    }

    public static void crash(Throwable e) {
        log.error("Application crashing ", e);
        System.exit(1);
    }
}
