/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

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
				VariantContext variantContext = vcfFileReaderIterator.next();
				if (!Chromosome.isChromosome(variantContext.getContig())) {
					continue;//Игнорируем непонятные хромосомы
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
