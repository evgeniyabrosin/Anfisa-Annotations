package org.forome.annotation.annotator.input;

import org.forome.annotation.struct.Chromosome;
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
                    nextVariant = build(line);
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

            String line = bufferedReader.readLine();
            if (line != null) {
                nextVariant = build(line);
            } else {
                nextVariant = null;
                close();
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private VariantCNV build(String value) {
        String[] values = value.split("\t");

        Chromosome chromosome = new Chromosome(values[0]);
        int start = Integer.parseInt(values[1]);
        int end = Integer.parseInt(values[2]);

        String[] gts = values[6].split(":");
        String[] los = values[7].split(":");

        List<Genotype> genotypes = new ArrayList<>();
        for (int i = 0; i < samples.length; i++) {
            String sample = samples[i];
            String gt = gts[i].trim();
            float lo = Float.parseFloat(los[i].trim());
            genotypes.add(new Genotype(sample, gt, lo));
        }

        return new VariantCNV(chromosome, start, end, genotypes);
    }

    @Override
    public void close() throws IOException {
        this.bufferedReader.close();
        this.inputStream.close();
    }
}
