package ru.processtech.forome.annotation.connector.gnomad.struct;

import java.util.Objects;
import java.util.Set;

public class GnomadResult {

	public static final GnomadResult EMPTY = new GnomadResult(
			null, null, null,
			null, 0, 0,
			null
	);
	public final Sum exomes;
	public final Sum genomes;
	public final Sum overall;
	public final String popmax;
	public final double popmaxAf;
	public final long popmaxAn;
	public final Set<Url> urls;

	public GnomadResult(
			Sum exomes, Sum genomes, Sum overall,
			String popmax, double popmaxAf, long popmaxAn,
			Set<Url> urls
	) {
		this.exomes = exomes;
		this.genomes = genomes;
		this.overall = overall;

		this.popmax = popmax;
		this.popmaxAf = popmaxAf;
		this.popmaxAn = popmaxAn;

		this.urls = urls;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GnomadResult that = (GnomadResult) o;
		return Double.compare(that.popmaxAf, popmaxAf) == 0 &&
				popmaxAn == that.popmaxAn &&
				Objects.equals(exomes, that.exomes) &&
				Objects.equals(genomes, that.genomes) &&
				Objects.equals(overall, that.overall) &&
				Objects.equals(popmax, that.popmax) &&
				Objects.equals(urls, that.urls);
	}

	@Override
	public int hashCode() {
		return Objects.hash(exomes, genomes, overall, popmax, popmaxAf, popmaxAn, urls);
	}

	public static class Sum {

		public final long an;
		public final long ac;
		public final double af;

		public Sum(long an, long ac, double af) {
			this.an = an;
			this.ac = ac;
			this.af = af;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Sum sum = (Sum) o;
			return an == sum.an &&
					ac == sum.ac &&
					Double.compare(sum.af, af) == 0;
		}

		@Override
		public int hashCode() {
			return Objects.hash(an, ac, af);
		}
	}

	public static class Url {

		public final String chromosome;
		public final long position;
		public final String reference;
		public final String alternative;

		public Url(String chromosome, long position, String reference, String alternative) {
			this.chromosome = chromosome;
			this.position = position;
			this.reference = reference;
			this.alternative = alternative;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Url url = (Url) o;
			return position == url.position &&
					Objects.equals(chromosome, url.chromosome) &&
					Objects.equals(reference, url.reference) &&
					Objects.equals(alternative, url.alternative);
		}

		@Override
		public int hashCode() {
			return Objects.hash(chromosome, position, reference, alternative);
		}

		@Override
		public String toString() {
			return String.format(
					"http://gnomad.broadinstitute.org/variant/%s-%s-%s-%s",
					chromosome, position, reference, alternative
			);
		}


	}


}
