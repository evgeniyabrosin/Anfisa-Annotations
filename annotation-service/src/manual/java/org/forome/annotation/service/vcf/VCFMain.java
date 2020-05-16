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

package org.forome.annotation.service.vcf;

import org.forome.annotation.Main;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.http.ClinvarConnectorHttp;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.gnomad.GnomadConnector;
import org.forome.annotation.data.gnomad.mysql.GnomadConnectorImpl;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtex.http.GTEXConnectorHttp;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.hgmd.http.HgmdConnectorHttp;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.pharmgkb.http.PharmGKBConnectorHttp;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.http.SpliceAIConnectorHttp;
import org.forome.annotation.iterator.vcf.VCFFileIterator;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.TypeQuery;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class VCFMain {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
		DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);

//        GnomadConnector gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
		GnomadConnector gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));

		SpliceAIConnector spliceAIConnector = new SpliceAIConnectorHttp();
//		SpliceAIConnector spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);

		ConservationData conservationConnector = new ConservationData(databaseConnectService);

		HgmdConnector hgmdConnector = new HgmdConnectorHttp();
//		HgmdConnector hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);

		ClinvarConnector clinvarConnector = new ClinvarConnectorHttp();
//		ClinvarConnector clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);

		LiftoverConnector liftoverConnector = new LiftoverConnector();
		GTFConnector gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> crash(e));

		GTEXConnector gtexConnector = new GTEXConnectorHttp();
//		GTEXConnector gtexConnector = new GTEXConnectorMysql(databaseConnectService, serviceConfig.gtexConfigConnector);

		PharmGKBConnector pharmGKBConnector = new PharmGKBConnectorHttp();
//		PharmGKBConnector pharmGKBConnector = new PharmGKBConnector(databaseConnectService, serviceConfig.pharmGKBConfigConnector);

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
		Processing processing = new Processing(anfisaConnector, TypeQuery.PATIENT_HG19);

		Path pathVcf = Paths.get("/home/kris/processtech/tmp/newvcf/HG002_GRCh37_GIAB_highconf_CG-IllFB-IllGATKHC-Ion-10X-SOLID_CHROM1-22_v.3.3.2_highconf_triophased.vcf");
		VCFFileIterator vcfFileIterator = new VCFFileIterator(pathVcf);

//		while (true) {
//			try {
//				MAVariantVep variant = vcfFileIterator.next();
//				JSONObject vepJson = ensemblVepService.getVepJson(variant).get();
//				variant.setVepJson(vepJson);
//
//				List<ProcessingResult> processingResults = processing.exec(null, variant);
//				for (ProcessingResult processingResult: processingResults) {
//					log.debug("processingResult: " + processingResult);
//				}
//			} catch (NoSuchElementException e) {
//				break;
//			}
//		}

		log.debug("end");
	}

	public static void crash(Throwable e) {
		log.error("Application crashing ", e);
		System.exit(1);
	}
}
