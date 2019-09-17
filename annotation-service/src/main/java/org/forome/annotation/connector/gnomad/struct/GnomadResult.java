package org.forome.annotation.connector.gnomad.struct;

import java.util.Objects;
import java.util.Set;

public class GnomadResult {

    public static class Popmax {

        public final GnamadGroup group;
        public final double af;
        public final long an;

        public Popmax(GnamadGroup group, double af, long an) {
            this.group = group;
            this.af = af;
            this.an = an;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Popmax popmax = (Popmax) o;
            return Double.compare(popmax.af, af) == 0 &&
                    an == popmax.an &&
                    Objects.equals(group, popmax.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, af, an);
        }
    }

    public final Sum exomes;
    public final Sum genomes;
    public final Sum overall;
    public final Popmax popmax;
    public final Popmax rawPopmax;
    public final Set<Url> urls;

    public GnomadResult(
            Sum exomes, Sum genomes, Sum overall,
            GnomadResult.Popmax popmax, Popmax rawPopmax,
            Set<Url> urls
    ) {
        this.exomes = exomes;
        this.genomes = genomes;
        this.overall = overall;

        this.popmax = popmax;
        this.rawPopmax = rawPopmax;

        this.urls = urls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GnomadResult that = (GnomadResult) o;
        return Objects.equals(rawPopmax, that.rawPopmax) &&
                Objects.equals(exomes, that.exomes) &&
                Objects.equals(genomes, that.genomes) &&
                Objects.equals(overall, that.overall) &&
                Objects.equals(popmax, that.popmax) &&
                Objects.equals(rawPopmax, that.rawPopmax) &&
                Objects.equals(urls, that.urls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exomes, genomes, overall, rawPopmax, urls);
    }

    public static class Sum {

        public final long an;
        public final long ac;
        public final double af;
        public final long hom;
        public final Long hem;

        public Sum(long an, long ac, double af, long hom, Long hem) {
            this.an = an;
            this.ac = ac;
            this.af = af;
            this.hom = hom;
            this.hem = hem;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sum sum = (Sum) o;
            return an == sum.an &&
                    ac == sum.ac &&
                    Double.compare(sum.af, af) == 0 &&
                    hom == sum.hom;
        }

        @Override
        public int hashCode() {
            return Objects.hash(an, ac, af, hom);
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
