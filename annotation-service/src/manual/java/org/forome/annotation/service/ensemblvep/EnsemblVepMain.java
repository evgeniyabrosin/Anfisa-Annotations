package org.forome.annotation.service.ensemblvep;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import net.minidev.json.JSONObject;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.service.ensemblvep.inline.EnsemblVepInlineService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class EnsemblVepMain {

    public static void main(String[] args) throws Exception {
        VariantContextBuilder variantContextBuilder = new VariantContextBuilder();
        variantContextBuilder.loc("15", 89876828, 89876836);
        variantContextBuilder.alleles(new ArrayList<Allele>(){{ add(Allele.create("TTGCTGC", false)); }});
        VariantContext variantContext = variantContextBuilder.make();
        System.out.println("variantContext: " + variantContext);
//        variantContext.toStringDecodeGenotypes()
    }

    public static void main1(String[] args) throws Exception {
        ServiceConfig serviceConfig = new ServiceConfig();
        SSHConnectService sshTunnelService = new SSHConnectService();

        try(EnsemblVepService ensemblVepService = new EnsemblVepInlineService(sshTunnelService, serviceConfig.ensemblVepConfigConnector)) {
            CompletableFuture<JSONObject> futureVepJson1 = ensemblVepService.getVepJson(
                    new Variant(new Chromosome("1"), 881907, 881906), "C"
            );
            CompletableFuture<JSONObject> futureVepJson2 = ensemblVepService.getVepJson(
                    new Variant(new Chromosome("1"), 881907, 881906), "C"
            );
            JSONObject vepJson1 = futureVepJson1.get();
            JSONObject vepJson2 = futureVepJson2.get();
            System.out.println("vepJson1: " + vepJson1);
            System.out.println("vepJson2: " + vepJson2);
        }
    }
}
