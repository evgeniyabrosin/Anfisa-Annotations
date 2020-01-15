/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.service.ensemblvep;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import net.minidev.json.JSONObject;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.ref.RefConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.inline.EnsemblVepInlineService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Chromosome;

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
		DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);

		try (EnsemblVepService ensemblVepService = new EnsemblVepInlineService(
				sshTunnelService,
				serviceConfig.ensemblVepConfigConnector,
				new RefConnector(databaseConnectService, serviceConfig.refConfigConnector)
		)) {
			for (int i=881906; i< 881916; i++) {
				CompletableFuture<JSONObject> futureVepJson = ensemblVepService.getVepJson(
						Chromosome.of("1"), i, i, "C"
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
