package org.forome.annotation.service.ensemblvep.inline;

import net.minidev.json.JSONObject;

@FunctionalInterface
public interface EnsemblVepCallback {

    void apply(String request, JSONObject response);
}
