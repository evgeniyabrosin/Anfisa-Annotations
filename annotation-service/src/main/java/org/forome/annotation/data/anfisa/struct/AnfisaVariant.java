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

package org.forome.annotation.data.anfisa.struct;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class AnfisaVariant {

	public static final ImmutableList<String> CSQ_DAMAGING = ImmutableList.of(
			"transcript_ablation",
			"splice_acceptor_variant",
			"splice_donor_variant",
			"stop_gained",
			"frameshift_variant",
			"stop_lost",
			"start_lost"
	);

	public static final ImmutableList<String> CSQ_MISSENSE = ImmutableList.of(
			"transcript_amplification",
			"inframe_insertion",
			"inframe_deletion",
			"missense_variant", //missense - когда замена нуклиотида(ов), меняет создаваемы белок
			"protein_altering_variant"
	);

	public static final ImmutableList<String> CSQ_BENIGN1 = ImmutableList.of(
			"splice_region_variant",
			"synonymous_variant", //Ситуация, когда замена нуклиотида(ов), не влияет на белок(когда одна и таже аминокислота кодируется разными способами)
			"stop_retained_variant",
			"coding_sequence_variant",
			"TF_binding_site_variant",
			"mature_miRNA_variant"
	);

	public static final ImmutableList<String> CSQ_BENIGN2 = ImmutableList.of(
			"5_prime_UTR_variant",
			"3_prime_UTR_variant",
			"non_coding_transcript_exon_variant",
			"non_coding_transcript_exon_variant",
			"upstream_gene_variant",
			"downstream_gene_variant",
			"regulatory_region_variant"
	);

	public static final ImmutableList<String> CSQ_BENIGN3 = ImmutableList.of(
			"intron_variant",
			"intergenic_variant"
	);

	public static final ImmutableList<List<String>> SEVERITY = ImmutableList.of(
			CSQ_DAMAGING, CSQ_MISSENSE, CSQ_BENIGN1, CSQ_BENIGN2, CSQ_BENIGN3
	);

	public static final ImmutableList<String> CONSEQUENCES = ImmutableList.copyOf(
			SEVERITY.stream().flatMap(strings -> strings.stream()).iterator()
	);

	public static final String BGM_BAYES_DE_NOVO = "BGM_BAYES_DE_NOVO";

	public static final String BGM_BAYES_DE_NOVO_S1 = "BGM_BAYES_DE_NOVO_S1";

	public static final ImmutableList<String> CALLERS = ImmutableList.of(
			"BGM_AUTO_DOM", "BGM_DE_NOVO", "BGM_HOM_REC", "BGM_CMPD_HET",
			BGM_BAYES_DE_NOVO, "BGM_BAYES_CMPD_HET", "BGM_BAYES_HOM_REC",
			"BGM_PIPELINE_A", "BGM_PIPELINE", "LMM", "SANGER", "RUFUS"
	);
}
