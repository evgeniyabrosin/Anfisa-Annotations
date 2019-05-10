package org.forome.annotation.annotator.executor;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
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
        }
        return _vepJson;
    }
}
