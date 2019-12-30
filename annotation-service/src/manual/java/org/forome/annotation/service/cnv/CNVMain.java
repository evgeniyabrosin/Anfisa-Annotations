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

package org.forome.annotation.service.cnv;

import net.minidev.json.JSONObject;
import org.forome.annotation.Main;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaInput;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.GnomadConnectorImpl;
import org.forome.annotation.connector.gtex.GTEXConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.pharmgkb.PharmGKBConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.iterator.cnv.CNVFileIterator;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CNVMain {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
		DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
//        GnomadConnector gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
		GnomadConnector gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
		SpliceAIConnector spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
		ConservationConnector conservationConnector = new ConservationConnector(databaseConnectService, serviceConfig.conservationConfigConnector);
		HgmdConnector hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);
		ClinvarConnector clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);
		LiftoverConnector liftoverConnector = new LiftoverConnector();
		GTFConnector gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> crash(e));
		GTEXConnector gtexConnector = new GTEXConnector(databaseConnectService, serviceConfig.gtexConfigConnector);
		PharmGKBConnector pharmGKBConnector = new PharmGKBConnector(databaseConnectService, serviceConfig.pharmGKBConfigConnector);
		EnsemblVepService ensemblVepService = new EnsemblVepExternalService((t, e) -> crash(e));
		AnfisaConnector anfisaConnector = new AnfisaConnector(
				gnomadConnector,
				spliceAIConnector,
				conservationConnector,
				hgmdConnector,
				clinvarConnector,
				liftoverConnector,
				gtfConnector,
				gtexConnector,
				pharmGKBConnector
		);
		Processing processing = new Processing(anfisaConnector);

		Path pathVcf = Paths.get("/home/kris/processtech/tmp/_3/cnv.vcf");
		CNVFileIterator cnvFileIterator = new CNVFileIterator(pathVcf);

		while (cnvFileIterator.hasNext()) {
			VariantCNV variant = cnvFileIterator.next();
			JSONObject vepJson = ensemblVepService.getVepJson(variant, "-").get();
			variant.setVepJson(vepJson);

			AnfisaInput anfisaInput = new AnfisaInput.Builder().build();
			List<ProcessingResult> processingResults = processing.exec(null, variant);
			for (ProcessingResult processingResult: processingResults) {
				log.debug("processingResult: " + processingResult);
			}
		}

		log.debug("end");
	}

	public static void crash(Throwable e) {
		log.error("Application crashing ", e);
		System.exit(1);
	}
}
