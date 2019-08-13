package org.forome.annotation.connector.anfisa.struct;

import org.forome.annotation.connector.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class AnfisaResultFilters {

	public String chromosome;
	public double fs;
	public Long severity;
	public List<String> has_variant = new ArrayList<>();
	public Integer minGq;
	public Integer probandGq;
	public long distFromExon;
	public GnomadResult.Popmax gnomadWidePopmax;
	public Double gnomadAfFam;
	public Double gnomadAfPb;
	public Double gnomadDbGenomesAf;
	public Object gnomadDbExomesAf;
	public Long gnomadHom;
	public Long gnomadHem;
	public double qd;
	public Double mq;
	public List<String> filters;

	public Boolean clinvarBenign;
	public Boolean hgmdBenign;

	public Optional<Boolean> clinvarTrustedBenign;
	public HashMap<String, Integer> altZygosity;

	public String spliceAltering;
	public Float spliceAiDsmax;

	public List<String> alts;

	public Integer numClinvarSubmitters;

	public ClinvarVariantSummary.ReviewStatus clinvarReviewStatus;
	public String[] clinvarAcmgGuidelines;

	public AnfisaResultFilters() {
	}
}
