package org.forome.annotation.annotator.executor;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantVCF;

import java.util.Objects;

class Source {

    public final Variant variant;
    private final String strVepJson;

    private JSONObject _vepJson;

    public Source(Variant variant, String strVepJson) {
        this.variant = variant;
        this.strVepJson = strVepJson;
    }

    public JSONObject getVepJson() {
        if (strVepJson == null) return null;
        if (_vepJson == null) {
            try {
                _vepJson = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(strVepJson);
            } catch (ParseException e) {
                throw ExceptionBuilder.buildInvalidVepJsonException(e);
            }

            //Для оптимизации валидируем тут - конечно было бы правильнее сделать это выше,
            // но тогда будет проседание по производительности
            if (variant instanceof VariantVCF) {
                VariantContext variantContext = ((VariantVCF) variant).variantContext;

                String[] vepJsonInput = _vepJson.getAsString("input").split("\t");

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

                int vcfStart = variantContext.getStart();
                int vcfEnd = variantContext.getEnd();
                int vepJsonPosition = Integer.parseInt(vepJsonInput[1]);
                if (!(
                        Math.min(vcfStart, vcfEnd) <= vepJsonPosition && vepJsonPosition <= Math.max(vcfStart, vcfEnd)
                )) {
                    throw new RuntimeException(
                            String.format("Not equals: vcf start: %s, vcf end: %s, vep.json position: %s",
                                    vcfStart, vcfEnd, vepJsonPosition
                            )
                    );
                }

                //Дополнительная, валидация(с другой стороны)
                if (!Objects.equals(
                        new Chromosome(variantContext.getContig()),
                        new Chromosome(_vepJson.getAsString("seq_region_name"))
                )) {
                    throw new RuntimeException(
                            String.format("Not equals chromosome, vcf %s and vep.json %s", variantContext.getContig(), _vepJson)
                    );
                }
//            if (VariantVCF.getStart(variantContext) != _vepJson.getAsNumber("start").intValue()) {
//                throw new RuntimeException(
//                        String.format("Not equals start, vcf: %s, vep.json: %s",
//                                variantContext.getStart(), _vepJson.getAsNumber("start")
//                        )
//                );
//            }
//            if (variantContext.getEnd() != _vepJson.getAsNumber("end").intValue()) {
//                throw new RuntimeException(
//                        String.format("Not equals end, vcf: %s, vep.json: %s",
//                                variantContext.getEnd(), _vepJson.getAsNumber("end")
//                        )
//                );
//            }
            }
        }
        return _vepJson;
    }
}
