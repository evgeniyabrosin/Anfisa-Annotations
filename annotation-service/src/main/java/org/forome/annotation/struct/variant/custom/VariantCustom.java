package org.forome.annotation.struct.variant.custom;

import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.vep.VariantVep;

public class VariantCustom extends VariantVep {

	private final int start;

	public VariantCustom(Chromosome chromosome, int start, int end) {
		super(chromosome, end);
		this.start = start;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public Genotype getGenotype(String sample) {
		return null;
	}
}
