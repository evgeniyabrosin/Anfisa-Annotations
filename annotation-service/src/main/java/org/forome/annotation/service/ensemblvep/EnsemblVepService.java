package org.forome.annotation.service.ensemblvep;

import com.jcraft.jsch.JSchException;
import net.minidev.json.JSONObject;
import org.forome.annotation.config.ensemblvep.EnsemblVepConfig;
import org.forome.annotation.service.ssh.SSHConnectService;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;


public class EnsemblVepService implements Closeable {

    private final EnsemblVepRunner vepExecutor;

    public EnsemblVepService(SSHConnectService sshTunnelService, EnsemblVepConfig ensemblVepConfigConnector) throws JSchException {
        this.vepExecutor = new EnsemblVepRunner(sshTunnelService, ensemblVepConfigConnector);



    }

    public JSONObject getVepJson(String chromosome, long sPosition, long ePosition, String alt) throws Exception {
        String cmd = new StringBuilder("/db/vep-93/ensembl-vep/vep ")
                .append("--cache --dir /db/data/vep/cache --dir_cache /db/data/vep/cache ")
                .append("--fork 8 ")
                .append("--uniprot --hgvs --symbol --numbers --domains --regulatory --canonical --protein --biotype --tsl --appris --gene_phenotype --variant_class ")
                .append("--fasta /db/data/vep/cache/homo_sapiens/93_GRCh37/Homo_sapiens.GRCh37.75.dna.primary_assembly.fa.gz ")
                .append("--force_overwrite ")
                .append("--merged ")
                .append("--json ")
                .append("--port 3337 ")
                .append("--output_file tmp.vep.json ")
                .append("--plugin ExACpLI,/db/data/misc/ExACpLI_values.txt ")
                .append("--plugin MaxEntScan,/db/data/MaxEntScan/fordownload ")
                .append("--plugin LoFtool,/db/data/loftoll/LoFtool_scores.txt ")
                .append("--plugin dbNSFP,/db/data/dbNSFPa/dbNSFP_hg19.gz,Polyphen2_HDIV_pred,Polyphen2_HVAR_pred,Polyphen2_HDIV_score,Polyphen2_HVAR_score,SIFT_pred,SIFT_score,MutationTaster_pred,MutationTaster_score,FATHMM_pred,FATHMM_score,REVEL_score,CADD_phred,CADD_raw,MutationAssessor_score,MutationAssessor_pred,clinvar_rs,clinvar_clnsig ")
                .append("--plugin SpliceRegion ")
                .toString();

        InputStream stdin = new ByteArrayInputStream(
                "".getBytes()
        );

        vepExecutor.exec(cmd, stdin);
        return null;
    }

    @Override
    public void close() {
        vepExecutor.close();
    }
}
