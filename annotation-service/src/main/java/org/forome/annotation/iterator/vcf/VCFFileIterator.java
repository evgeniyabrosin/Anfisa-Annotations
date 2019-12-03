package org.forome.annotation.iterator.vcf;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.iterator.cnv.CNVFileIterator;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

public class VCFFileIterator implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(VCFFileIterator.class);

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

            //Validation equals samples
            List<String> vcfSamples = vcfFileReader.getFileHeader().getGenotypeSamples();
            List<String> cnvSamples = cnvFileIterator.getSamples();
            if (vcfSamples.size() != cnvSamples.size() || !vcfSamples.containsAll(cnvSamples)) {
                throw ExceptionBuilder.buildNotEqualSamplesVcfAndCnvFile();
            }
        } else {
            cnvFileIterator = null;
        }
    }

    public VariantVep next() throws NoSuchElementException {
        while (true) {
            if (vcfFileReaderIterator.hasNext()) {
                VariantContext variantContext;
                try {
                    variantContext = vcfFileReaderIterator.next();
                } catch (Throwable e) {
                    log.warn("Invalid variant, ignored", e);
                    continue;
                }
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
