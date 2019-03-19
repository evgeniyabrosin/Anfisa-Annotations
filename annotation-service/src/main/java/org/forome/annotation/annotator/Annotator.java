package org.forome.annotation.annotator;

import io.reactivex.Observable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.annotator.utils.CaseUtils;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.struct.Sample;
import pro.parseq.vcf.VcfExplorer;
import pro.parseq.vcf.exceptions.InvalidVcfFileException;
import pro.parseq.vcf.types.DataLine;
import pro.parseq.vcf.types.VcfLine;
import pro.parseq.vcf.utils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Annotator {

    private final AnfisaConnector anfisaConnector;

    public Annotator(AnfisaConnector anfisaConnector) {
        this.anfisaConnector = anfisaConnector;
    }

    public Observable<AnfisaResult> exec(
            String caseName,
            Path pathFam,
            Path pathVepFilteredVcf,
            Path pathVepFilteredVepJson,
            int startPosition
    ) throws IOException, ParseException, InvalidVcfFileException {
        if (!pathFam.getFileName().toString().endsWith(".fam")) {
            throw new IllegalArgumentException("Bad name fam file: " + pathFam.toAbsolutePath());
        }
        if (!pathVepFilteredVcf.getFileName().toString().endsWith(".vep.filtered.vcf")) {
            throw new IllegalArgumentException("Bad name VepFilteredVcf file: " + pathVepFilteredVcf.toAbsolutePath());
        }
        if (!pathVepFilteredVepJson.getFileName().toString().endsWith(".vep.filtered.vep.json")) {
            throw new IllegalArgumentException("Bad name pathVepFilteredVepJson file: " + pathVepFilteredVepJson.toAbsolutePath());
        }

        try (
                InputStream isFam = Files.newInputStream(pathFam);
                InputStream isVepFilteredVcf = Files.newInputStream(pathVepFilteredVcf);
                InputStream isVepFilteredVepJson = Files.newInputStream(pathVepFilteredVepJson);
        ) {
            return exec(
                    caseName,
                    isFam,
                    isVepFilteredVcf,
                    isVepFilteredVepJson,
                    startPosition
            );
        }
    }

    public Observable<AnfisaResult> exec(
            String caseName,
            InputStream isFam,
            InputStream isVepFilteredVcf,
            InputStream isVepFilteredVepJson,
            int startPosition
    ) throws IOException, ParseException, InvalidVcfFileException {

        List<JSONObject> vepFilteredVepJsons = new ArrayList<>();
        try (BufferedReader isBVepJson = new BufferedReader(new InputStreamReader(isVepFilteredVepJson))) {
            String line;
            while ((line = isBVepJson.readLine()) != null) {
                JSONObject json = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(line);
                vepFilteredVepJsons.add(json);
            }
        }

        VcfReader reader = new InputStreamVcfReader(isVepFilteredVcf);
        VcfParser parser = new VcfParserImpl();
        VcfExplorer vcfExplorer = new VcfExplorer(reader, parser);
        vcfExplorer.parse(FaultTolerance.FAIL_FAST);

        if (vepFilteredVepJsons.size() != vcfExplorer.getVcfData().getDataLines().size()) {
            throw new RuntimeException(
                    String.format("Not equal record size VepJsons(%s) and Vcf file(%s)",
                            vepFilteredVepJsons.size(), vcfExplorer.getVcfData().getDataLines().size()
                    )
            );
        }

        Map<String, Sample> samples = CaseUtils.parseFamFile(isFam);

        return annotateJson(
                String.format("%s_wgs", caseName),
                vepFilteredVepJsons,
                vcfExplorer, samples,
                startPosition
        );
    }

    private Observable<AnfisaResult> annotateJson(
            String caseSequence,
            List<JSONObject> vepFilteredVepJsons,
            VcfExplorer vcfExplorer, Map<String, Sample> samples,
            int startPosition
    ) {
        return Observable.create(o -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = startPosition; i < vepFilteredVepJsons.size(); i++) {
                            JSONObject json = vepFilteredVepJsons.get(i);
                            String chromosome = RequestParser.toChromosome(json.getAsString("seq_region_name"));
                            long start = json.getAsNumber("start").longValue();
                            long end = json.getAsNumber("end").longValue();

                            DataLine dataLine = (DataLine) vcfExplorer.getVcfData().getDataLines().get(i);

                            AnfisaResult anfisaResult = anfisaConnector.build(caseSequence, chromosome, start, end, json, dataLine, samples);
                            o.onNext(anfisaResult);
                        }
                        o.onComplete();
                    } catch (Throwable t) {
                        o.tryOnError(t);
                    }
                }
            }).start();
        });
    }

}
