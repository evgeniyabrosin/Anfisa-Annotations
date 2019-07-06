package org.forome.annotation.annotator.executor;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.exception.ExceptionBuilder;

class Source {

    public final VariantContext variantContext;
    private final String strVepJson;

    private JSONObject _vepJson;

    public Source(VariantContext variantContext, String strVepJson) {
        this.variantContext = variantContext;
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
        }
        return _vepJson;
    }
}
