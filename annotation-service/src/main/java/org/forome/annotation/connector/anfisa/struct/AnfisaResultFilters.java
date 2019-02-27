package org.forome.annotation.connector.anfisa.struct;

import java.util.Optional;

public class AnfisaResultFilters {

	public Double gnomadPopmaxAf;
	public float fs;
	public Long severity;
	public String[] has_variant;
	public Long minGq;
	public Long probandGq;
	public int distFromExon;
	public String gnomadPopmax;
	public Long gnomadPopmaxAn;
	public Double gnomadAfFam;
	public Double gnomadAfPb;
	public Double gnomadDbGenomesAf;
	public Object gnomadDbExomesAf;
	public float qd;

	public Boolean clinvarBenign;
	public Boolean hgmdBenign;

	public Optional<Boolean> clinvarTrustedBenign;

	public AnfisaResultFilters() {
	}
}
