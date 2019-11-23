/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.customvariant;

import net.minidev.json.JSONObject;
import org.forome.annotation.Main;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaInput;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.old.GnomadConnectorOld;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class VariantMain {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	private final ServiceConfig serviceConfig;
	private final SSHConnectService sshTunnelService;
	private final DatabaseConnectService databaseConnectService;
	private final GnomadConnector gnomadConnector;
	private final SpliceAIConnector spliceAIConnector;
	private final ConservationConnector conservationConnector;
	private final HgmdConnector hgmdConnector;
	private final ClinvarConnector clinvarConnector;
	private final LiftoverConnector liftoverConnector;
	private final GTFConnector gtfConnector;
	private final EnsemblVepService ensemblVepService;
	private final AnfisaConnector anfisaConnector;

	public VariantMain() throws Exception {
		serviceConfig = new ServiceConfig();
		sshTunnelService = new SSHConnectService();
		databaseConnectService = new DatabaseConnectService(sshTunnelService);

		gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
//      gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
		spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
		conservationConnector = new ConservationConnector(databaseConnectService, serviceConfig.conservationConfigConnector);
		hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);
		clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);
		liftoverConnector = new LiftoverConnector();
		gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> crash(e));
		ensemblVepService = new EnsemblVepExternalService((t, e) -> crash(e));
		anfisaConnector = new AnfisaConnector(
				gnomadConnector,
				spliceAIConnector,
				conservationConnector,
				hgmdConnector,
				clinvarConnector,
				liftoverConnector,
				gtfConnector
		);
	}

	private AnfisaResult build(VariantCustom variant, String alternative) throws ExecutionException, InterruptedException {
		JSONObject vepJson = ensemblVepService.getVepJson(variant, alternative).get();
		variant.setVepJson(vepJson);

		AnfisaInput anfisaInput = new AnfisaInput.Builder().build();
		return anfisaConnector.build(anfisaInput, variant);
	}

	public static void main(String[] args) throws Exception {
		VariantMain variantMain = new VariantMain();

		AnfisaResult anfisaResult = variantMain.build(
				new VariantCustom(Chromosome.of("11"), 2466481, 2466480), "AC"
		);
		log.debug("result: " + anfisaResult.toJSON());
	}



	public static void crash(Throwable e) {
		log.error("Application crashing ", e);
		System.exit(1);
	}
}
