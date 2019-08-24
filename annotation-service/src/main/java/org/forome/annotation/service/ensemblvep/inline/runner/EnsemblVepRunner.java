package org.forome.annotation.service.ensemblvep.inline.runner;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.service.ensemblvep.inline.EnsemblVepCallback;
import org.forome.annotation.service.ensemblvep.inline.struct.EnsembleVepRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class EnsemblVepRunner implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(EnsemblVepRunner.class);

    protected final static String cmd = "/db/vep-93/ensembl-vep/vep"
            + " --buffer_size 1"
            + " --cache --dir /db/data/vep/cache --dir_cache /db/data/vep/cache "
            + " --uniprot --hgvs --symbol --numbers --domains --regulatory --canonical --protein --biotype --tsl --appris --gene_phenotype --variant_class"
            + " --fasta /db/data/vep/cache/homo_sapiens/93_GRCh37/Homo_sapiens.GRCh37.75.dna.primary_assembly.fa.gz"
            + " --force_overwrite"
            + " --merged"
            + " --json"
            + " --port 3337"
            + " --format ensembl"
            + " --output_file STDOUT"
            + " --plugin ExACpLI,/db/data/misc/ExACpLI_values.txt"
            + " --plugin MaxEntScan,/db/data/MaxEntScan/fordownload"
            + " --plugin LoFtool,/db/data/loftoll/LoFtool_scores.txt"
            + " --plugin dbNSFP,/db/data/dbNSFPa/dbNSFP_hg19.gz,Polyphen2_HDIV_pred,Polyphen2_HVAR_pred,Polyphen2_HDIV_score,Polyphen2_HVAR_score,SIFT_pred,SIFT_score,MutationTaster_pred,MutationTaster_score,FATHMM_pred,FATHMM_score,REVEL_score,CADD_phred,CADD_raw,MutationAssessor_score,MutationAssessor_pred,clinvar_rs,clinvar_clnsig"
            + " --plugin SpliceRegion"
            + " --everything";

    protected final static String extrusionRequest = "1\t881907\t881906\t-/C\t+\textrusion_request";

    protected OutputStream stdin;
    protected InputStream stdout;
    protected InputStream stderr;

    private boolean isRun = true;

    public EnsemblVepRunner() {}

    public void start(EnsemblVepCallback callback) throws Exception {
        connect();

        Thread threadStdOut = new Thread(() -> {
            try (InputStreamReader inputReader = new InputStreamReader(stdout)) {
                String line = null;
                try(BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                    while ((line = bufferedReader.readLine()) != null && isRun) {
                        JSONObject response = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(line);
                        String request = response.getAsString("input");
                        log.debug("response: {}", request);
                        if (!extrusionRequest.equals(request)) {
                            callback.apply(request, response);
                        }
                    }
                } catch (ParseException e) {
                    log.debug("Exception, line: {}", line, e);
                }
            } catch (IOException ex) {
                log.debug("Exception", ex);
            }
        });
        threadStdOut.setDaemon(true);
        threadStdOut.start();

        Thread threadStdErr = new Thread(() -> {
            try (InputStreamReader inputReader = new InputStreamReader(stderr)) {
                String line = null;
                try(BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                    while ((line = bufferedReader.readLine()) != null && isRun) {
                        log.debug("error: {}", line);
                    }
                }
            } catch (IOException ex) {
                log.debug("Exception", ex);
            }
        });
        threadStdErr.setDaemon(true);
        threadStdErr.start();

        Thread threadExtrusion = new Thread(() -> {
            while (isRun){
                try {
                    send(extrusionRequest);
                    Thread.sleep(2000L);
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            }
        });
        threadExtrusion.setDaemon(true);
        threadExtrusion.start();

        //Даем время на старт
        Thread.sleep(1000L);
    }

    protected synchronized void connect() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd.split(" "));
        Process process = processBuilder.start();
        stdin = process.getOutputStream();
        stdout = process.getInputStream();
        stderr = process.getErrorStream();
    }


    private void send(String request) throws IOException {
        stdin.write((request + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }


    public void send(EnsembleVepRequest ensembleVepRequestt) throws IOException {
        stdin.write((ensembleVepRequestt.request + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
        ensembleVepRequestt.touch();
    }

    private synchronized void disconnect() {
        isRun = false;
    }


    @Override
    public void close() {
        disconnect();
    }


}
