package ru.processtech.forome.annotation.struct;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class Variant {

	public static final ImmutableList<String> CSQ_DAMAGING = ImmutableList.of(
			"transcript_ablation",
			"splice_acceptor_variant",
			"splice_donor_variant",
			"stop_gained",
			"frameshift_variant",
			"stop_lost",
			"start_lost",
			"transcript_amplification",
			"inframe_insertion",
			"inframe_deletion"
	);

	public static final ImmutableList<String> CSQ_MISSENSE = ImmutableList.of(
			"missense_variant"
	);

	public static final ImmutableList<String> CSQ_BENIGN1 = ImmutableList.of(
			"splice_region_variant",
			"synonymous_variant"
			);

	public static final ImmutableList<String> CSQ_BENIGN2 = ImmutableList.of(
			"5_prime_UTR_variant",
			"3_prime_UTR_variant",
			"non_coding_transcript_exon_variant",
			"non_coding_transcript_exon_variant",
			"intron_variant",
			"upstream_gene_variant",
			"downstream_gene_variant",
			"regulatory_region_variant"
	);

	public static final ImmutableList<List<String>> SEVERITY = ImmutableList.of(
			CSQ_DAMAGING, CSQ_MISSENSE, CSQ_BENIGN1, CSQ_BENIGN2
	);

}
