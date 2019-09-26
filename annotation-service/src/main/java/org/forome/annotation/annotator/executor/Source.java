package org.forome.annotation.annotator.executor;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

class Source {

    private final static Logger log = LoggerFactory.getLogger(Source.class);

    public final VariantVep variant;
    public final JSONObject vepJson;

    public Source(VariantVep variant, JSONObject vepJson) {
        this.variant = variant;
        this.vepJson = vepJson;

        variant.setVepJson(vepJson);

        //Валидация на соотвествие строк
        if (variant instanceof VariantVCF) {
            VariantContext variantContext = ((VariantVCF) variant).variantContext;

            String[] vepJsonInput = vepJson.getAsString("input").split("\t");

            String vcfChromosome = RequestParser.toChromosome(variantContext.getContig());
            String vepJsonChromosome = RequestParser.toChromosome(vepJsonInput[0]);
            if (!vcfChromosome.equals(vepJsonChromosome)) {
                throw new RuntimeException(
                        String.format("Not equals chromosome, vcf %s and vep.json %s", vcfChromosome, vepJsonChromosome)
                );
            }

            String vcfId = variantContext.getID();
            String vepJsonId = vepJsonInput[2];
            if (!vcfId.equals(vepJsonId)) {
                throw new RuntimeException(
                        String.format("Not equals id, vcf %s and vep.json %s", vcfId, vepJsonId)
                );
            }

            //Валидация позиций
            if (!Objects.equals(
                    Chromosome.of(variantContext.getContig()),
                    Chromosome.of(vepJson.getAsString("seq_region_name"))
            )) {
                throw new RuntimeException(
                        String.format("Not equals chromosome, vcf %s and vep.json %s", variantContext.getContig(), vepJson)
                );
            }
            if (VariantVCF.getStart(variantContext) != vepJson.getAsNumber("start").intValue()) {
                throw new RuntimeException(
                        String.format("Not equals start, vcf: %s, vep.json: %s, input: %s",
                                VariantVCF.getStart(variantContext), vepJson.getAsNumber("start"),
                                vepJson.getAsString("input")
                        )
                );
            }
            if (VariantVCF.getEnd(variantContext) != vepJson.getAsNumber("end").intValue()) {
                throw new RuntimeException(
                        String.format("Not equals end, vcf: %s, vep.json: %s, input: %s",
                                VariantVCF.getEnd(variantContext), vepJson.getAsNumber("end"),
                                vepJson.getAsString("input")
                        )
                );
            }

            //Валидация VariantType
//            String vepVariantType = vepJson.getAsString("variant_class");
//            VariantType vcfVariantType = variant.getVariantType();
//            log.debug("{} {}", vepVariantType, vcfVariantType);
//            if (!vepVariantType.equals(vcfVariantType.toJSON())) {
//                throw new RuntimeException(
//                        String.format("Not equals VariantType, vcf: %s, vep.json: %s, input: %s",
//                                vcfVariantType, vepVariantType,
//                                vepJson.getAsString("input")
//                        )
//                );
//            }

        }
    }
}
