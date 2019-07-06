package org.forome.annotation.annotator.input;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.forome.annotation.controller.utils.RequestParser;

import java.nio.file.Path;
import java.util.NoSuchElementException;

public class VCFFileIterator implements AutoCloseable {

    private final Path pathVcf;

    private final VCFFileReader vcfFileReader;
    private final CloseableIterator<VariantContext> vcfFileReaderIterator;

    public VCFFileIterator(Path pathVcf) {
        this.pathVcf = pathVcf;

        this.vcfFileReader = new VCFFileReader(pathVcf, false);
        this.vcfFileReaderIterator = vcfFileReader.iterator();
    }

    public VariantContext next() throws NoSuchElementException {
        VariantContext variantContext;
        while (true) {
            if (!vcfFileReaderIterator.hasNext()) {
                throw new NoSuchElementException();
            }
            variantContext = vcfFileReaderIterator.next();
            if ("M".equals(RequestParser.toChromosome(variantContext.getContig()))) {
                continue;//Игнорируем митохондрии
            }
            break;
        }
        return variantContext;
    }


    @Override
    public void close() {
        this.vcfFileReaderIterator.close();
        this.vcfFileReader.close();
    }
}
