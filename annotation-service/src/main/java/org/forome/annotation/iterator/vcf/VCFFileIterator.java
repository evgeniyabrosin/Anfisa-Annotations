package org.forome.annotation.iterator.vcf;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.forome.annotation.iterator.cnv.CNVFileIterator;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.nio.file.Path;
import java.util.NoSuchElementException;

public class VCFFileIterator implements AutoCloseable {

    private final VCFFileReader vcfFileReader;
    private final CloseableIterator<VariantContext> vcfFileReaderIterator;

    private final CNVFileIterator cnvFileIterator;

    public VCFFileIterator(Path pathVcf) {
        this(pathVcf, null);
    }

    public VCFFileIterator(Path pathVcf, Path cnvFile) {
        this.vcfFileReader = new VCFFileReader(pathVcf, false);
        this.vcfFileReaderIterator = vcfFileReader.iterator();

        if (cnvFile != null) {
            cnvFileIterator = new CNVFileIterator(cnvFile);
        } else {
            cnvFileIterator = null;
        }
    }

    public VariantVep next() throws NoSuchElementException {
        while (true) {
            if (vcfFileReaderIterator.hasNext()) {
                VariantContext variantContext = vcfFileReaderIterator.next();
                if (Chromosome.CHR_M == Chromosome.of(variantContext.getContig())) {
                    continue;//Игнорируем митохондрии
                }
                return new VariantVCF(variantContext);
            } else if (cnvFileIterator != null && cnvFileIterator.hasNext()) {
                return cnvFileIterator.next();
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    @Override
    public void close() {
        this.vcfFileReaderIterator.close();
        this.vcfFileReader.close();
    }
}
