package org.forome.annotation.iterator.cnv;

import org.forome.annotation.struct.variant.cnv.Genotype;
import org.forome.annotation.struct.variant.cnv.VariantCNV;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CNVFileIterator implements AutoCloseable {

    private static Pattern PATTERN_SAMPLES = Pattern.compile(
            "^#(.*)Samples(.*):(.*)$"
    );

    private final InputStream inputStream;
    private final BufferedReader bufferedReader;

    private String[] samples;

    private VariantCNV nextVariant;

    private CNVFileRecord nextProcessedRecord;

    public CNVFileIterator(Path pathCnv) {
        try {
            this.inputStream = Files.newInputStream(pathCnv);
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#")) {
                    Matcher matcherSamples = PATTERN_SAMPLES.matcher(line);
                    if (matcherSamples.matches()) {
                        samples = Arrays.stream(
                                matcherSamples.group(3).trim().split(",")
                        ).map(s -> s.trim()).toArray(String[]::new);
                    }
                    continue;
                } else {
                    nextVariant = readVariant(new CNVFileRecord(line));
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasNext() {
        return (nextVariant != null);
    }

    public VariantCNV next() throws NoSuchElementException {
        if (nextVariant == null) {
            throw new NoSuchElementException();
        }

        try {
            VariantCNV result = nextVariant;
            if (nextProcessedRecord!=null) {
                nextVariant = readVariant(nextProcessedRecord);
            } else {
                nextVariant = null;
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private VariantCNV readVariant(CNVFileRecord record) throws IOException {
        List<CNVFileRecord> records = new ArrayList<>();
        records.add(record);

        while (true) {
            String line = bufferedReader.readLine();
            if (line != null) {
                CNVFileRecord iRecord = new CNVFileRecord(line);
                if (record.chromosome.equals(iRecord.chromosome) &&
                        record.start == iRecord.start &&
                        record.end == iRecord.end
                ) {
                    records.add(iRecord);
                } else {
                    nextProcessedRecord = iRecord;
                    break;
                }
            } else {
                nextProcessedRecord = null;
                close();
                break;
            }
        }

        List<Genotype> genotypes = new ArrayList<>();
        for (int i = 0; i < samples.length; i++) {
            String sample = samples[i];
            String gt = record.gts[i].trim();
            float lo = Float.parseFloat(record.los[i].trim());
            genotypes.add(new Genotype(sample, gt, lo));
        }

        return new VariantCNV(
                record.chromosome,
                record.start, record.end,
                genotypes
        );
    }

    @Override
    public void close() throws IOException {
        this.bufferedReader.close();
        this.inputStream.close();
    }
}
