package org.forome.annotation.annotator;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.forome.annotation.utils.JSONEquals;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Подготовка тестовых данных
 * cd /home/vulitin/tmp/anfisa
 * git pull
 *
 * cd /home/vulitin/tmp/111/bgm9001/
 * PYTHONPATH=/home/vulitin/tmp/anfisa python -m annotations.annotator -i /home/vulitin/tmp/111/bgm9001/bgm9001_wgs_xbrowse.vep.filtered.vep.json -o /home/vulitin/tmp/111/bgm9001/output_file
 *
 * =================================
 * cd /home/vulitin/tmp/111/bgm9001/
 * java -cp /home/vulitin/deploy/annotationservice/exec/annotation.jar org.forome.annotation.annotator.main.AnnotatorMain -config /home/vulitin/deploy/annotationservice/exec/config.json -vcf bgm9001_wgs_xbrowse.vep.filtered.vcf -vepjson bgm9001_wgs_xbrowse.vep.filtered.vep.json -output /tmp/bgm9001_output_java.json
 *
 */
public class AnnotatorTest extends AnfisaBaseTest {

    private final static Logger log = LoggerFactory.getLogger(AnnotatorTest.class);

    @Test
    public void test() throws Exception {
        Annotator annotator = new Annotator(anfisaConnector);
        int start = 52;//
        //Ошибочные варианты: 543, 763

        Path fileExpected = Paths.get("/home/kris/processtech/tmp/bgm9001/output_file");
        List<JSONObject> expecteds =
                Files.readAllLines(fileExpected)
                        .stream()
                        .map(s -> {
                            try {
                                return new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(s, JSONObject.class);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());


        AnnotatorResult annotatorResult = annotator.exec(
                "bgm9001",
                Paths.get("/home/kris/processtech/tmp/bgm9001/bgm9001.fam"),
                Paths.get("/home/kris/processtech/tmp/bgm9001/bgm9001_wgs_xbrowse.vep.filtered.vcf"),
                Paths.get("/home/kris/processtech/tmp/bgm9001/bgm9001_wgs_xbrowse.vep.filtered.vep.json"),
                start
        );

//        VCFFileReader vcfFileReader = new VCFFileReader(
//                Paths.get("/home/kris/processtech/tmp/bch0004_wgs_1.vcf")
//                , false);
//
//        AnnotatorResult annotatorResult = annotator.annotateJson(
//                String.format("%s_wgs", "noname"),
//                null,
//                vcfFileReader,
//                null,
//                0
//        );

        //Игнорим загаловок
        expecteds.remove(0);

        if (start > 1) {
            for (int i = 1; i < start; i++) {
                expecteds.remove(0);
            }
        }

        AtomicInteger line = new AtomicInteger(start);
        annotatorResult.observableAnfisaResult.blockingSubscribe(
                anfisaResult -> {
                    JSONObject actual = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(
                            //Сделано специально, что бы потерять всю информацию о типах и работать с чистым json
                            GetAnfisaJSONController.build(anfisaResult).toJSONString()
                    );

                    JSONObject expected = expecteds.remove(0);

                    try {
                        JSONEquals.equals(expected, actual);
                        log.debug("{}, equals: {}", line.get(), anfisaResult);
                        line.incrementAndGet();
                    } catch (Throwable e) {
                        log.error("Fail", e);
                        Assert.fail(e.getMessage());
                    }
                },
                e -> {
                    log.error("Fail", e);
                    Assert.fail(e.getMessage());
                }
        );


    }
}
