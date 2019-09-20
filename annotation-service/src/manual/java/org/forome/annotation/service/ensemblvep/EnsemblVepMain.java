package org.forome.annotation.service.ensemblvep;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import net.minidev.json.JSONObject;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.ref.RefConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.inline.EnsemblVepInlineService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class EnsemblVepMain {

    public static void main1(String[] args) throws Exception {
        VariantContextBuilder variantContextBuilder = new VariantContextBuilder();
        variantContextBuilder.loc("15", 89876828, 89876836);
        variantContextBuilder.alleles(new ArrayList<Allele>() {{
            add(Allele.create("TTGCTGC", false));
        }});
        VariantContext variantContext = variantContextBuilder.make();
        System.out.println("variantContext: " + variantContext);
//        variantContext.toStringDecodeGenotypes()
    }

    //chr1:881907-881906 C>C

    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = new ServiceConfig();
        SSHConnectService sshTunnelService = new SSHConnectService();
        DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService);

        try (EnsemblVepService ensemblVepService = new EnsemblVepInlineService(
                sshTunnelService,
                serviceConfig.ensemblVepConfigConnector,
                new RefConnector(databaseConnectService, serviceConfig.refConfigConnector)
        )) {
            for (int i=881906; i< 881916; i++) {
                CompletableFuture<JSONObject> futureVepJson = ensemblVepService.getVepJson(
                        new Variant(Chromosome.of("1"), i, i), "C"
                );
                JSONObject vepJson = futureVepJson.get();
                System.out.println("vepJson: " + vepJson);
            }
            System.out.println("complete");
        }

        databaseConnectService.close();
        sshTunnelService.close();
    }
}
