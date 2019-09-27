package org.forome.annotation.struct.variant.vcf;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
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
    public List<String> getAltAllele() {
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

    /*
    @Override
    public VariantType getVariantType() {
        Allele ref = variantContext.getReference();
        List<Allele> altAlleles = variantContext.getAlternateAlleles();

        VariantContext.Type vcfVariantType = variantContext.getType();
        StructuralVariantType structuralVariantType = variantContext.getStructuralVariantType();
        switch (vcfVariantType) {
            case SNP:
                if (isAllEqualsLength(ref, altAlleles)) {
                    return VariantType.SNV;
                }
                break;
            case INDEL:
                if (altAlleles.stream().filter(allele -> allele.length() < ref.length()).count() == 0) {
                    //Все альтернативные аллели длинее референса
                    return VariantType.INS;
                } else if (altAlleles.stream().filter(allele -> allele.length() > ref.length()).count() == 0) {
                    //Все альтернативные аллели короче референса
                    return VariantType.DEL;
                }
                break;
            default:
                throw new RuntimeException("Unknown vcf variantType: " + vcfVariantType);
        }

        return VariantType.SEQUENCE_ALTERATION;
    }
    */

    public static int getStart(VariantContext variantContext) {
        if (isAnyEqualsLength(variantContext.getReference(), variantContext.getAlternateAlleles())) {
            return variantContext.getStart();
        } else {
            return variantContext.getStart() + 1;
        }
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
    private static boolean isAnyEqualsLength(Allele reference, List<Allele> alleles) {
        for (Allele allele : alleles) {
            if (allele.length() == reference.length()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllEqualsLength(Allele reference, List<Allele> alleles) {
        for (Allele allele : alleles) {
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
