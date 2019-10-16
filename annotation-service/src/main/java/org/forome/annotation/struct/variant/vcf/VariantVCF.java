package org.forome.annotation.struct.variant.vcf;

import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariantVCF extends VariantVep {

    public final VariantContext variantContext;

    public VariantVCF(VariantContext variantContext) {
        super(
                Chromosome.of(variantContext.getContig()),
                getStart(variantContext),
                getEnd(variantContext)
        );
        this.variantContext = variantContext;
    }

    @Override
    public Genotype getGenotype(String sample) {
        return new GenotypeVCF(variantContext, sample);
    }

    @Override
    public String getRef() {
        return variantContext.getReference().getBaseString();
    }

    @Override
    public List<Allele> getAltAllele() {
        return variantContext.getAlternateAlleles().stream().map(allele -> new Allele(allele.getBaseString())).collect(Collectors.toList());
    }

    @Override
    public List<String> getStrAltAllele() {
        List<String> alleles = variantContext.getAlleles()
                .stream().map(allele -> allele.getBaseString()).collect(Collectors.toList());
        List<String> alt_allels = variantContext.getAlternateAlleles()
                .stream().map(allele -> allele.getBaseString()).collect(Collectors.toList());
        Map<String, Long> counts = new HashMap<>();
        for (htsjdk.variant.variantcontext.Genotype genotype : variantContext.getGenotypes()) {
            int[] ad = genotype.getAD();
            if (ad == null || ad.length == 0) {
                return alt_allels;
            }
            for (int i = 0; i < alleles.size(); i++) {
                String al = alleles.get(i);
                long n = ad[i];
                counts.put(al, counts.getOrDefault(al, 0L) + n);
            }
        }
        List<String> tmp_alt_allels = alt_allels.stream()
                .filter(s -> counts.containsKey(s) && counts.get(s) > 0)
                .collect(Collectors.toList());
        if (!tmp_alt_allels.isEmpty()) {
            return tmp_alt_allels;
        } else {
            return alt_allels;
        }
    }

    public static int getStart(VariantContext variantContext) {
        if (isAnyEqualsLength(variantContext.getReference(), variantContext.getAlternateAlleles())) {
            return variantContext.getStart();
        } else {
            char fRef = variantContext.getReference().getBaseString().charAt(0);
            boolean increment = false;
            for (htsjdk.variant.variantcontext.Allele allele : variantContext.getAlternateAlleles()) {
                char fAlt = allele.getBaseString().charAt(0);
                if (fRef == fAlt) {
                    increment = true;
                    break;
                }
            }

            if (increment) {
                return variantContext.getStart() + 1;
            } else {
                return variantContext.getStart();
            }
        }

    /*
    if (isAnyEqualsLength(variantContext.getReference(), variantContext.getAlternateAlleles())) {
        return variantContext.getStart();
    } else {
        return variantContext.getStart() + 1;
    }
    */
    }

    public static int getEnd(VariantContext variantContext) {
        return variantContext.getEnd();
    }

    /**
     * Проверяем, что длина одного из аллелей равна длине реверенса
     *
     * @param reference
     * @param alleles
     * @return
     */
    private static boolean isAnyEqualsLength(htsjdk.variant.variantcontext.Allele reference, List<htsjdk.variant.variantcontext.Allele> alleles) {
        for (htsjdk.variant.variantcontext.Allele allele : alleles) {
            if (allele.length() == reference.length()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllEqualsLength(htsjdk.variant.variantcontext.Allele reference, List<htsjdk.variant.variantcontext.Allele> alleles) {
        for (htsjdk.variant.variantcontext.Allele allele : alleles) {
            if ("*".equals(allele.getBaseString())) {
                return false;
            }
            if (allele.length() != reference.length()) {
                return false;
            }
        }
        return true;
    }

}
