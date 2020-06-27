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
import org.forome.annotation.data.astorage.AStorageHttp;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.mysql.ClinvarConnectorMysql;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gnomad.datasource.http.GnomadDataSourceHttp;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtex.mysql.GTEXConnectorMysql;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.GTFConnectorImpl;
import org.forome.annotation.data.gtf.datasource.http.GTFDataSourceHttp;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.hgmd.mysql.HgmdConnectorMysql;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.pharmgkb.mysql.PharmGKBConnectorMysql;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnectorImpl;
import org.forome.annotation.data.spliceai.datasource.http.SpliceAIDataSourceHttp;
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

		LiftoverConnector liftoverConnector = new LiftoverConnector();

		GnomadConnectorImpl gnomadConnector = new GnomadConnectorImpl(new GnomadDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector), (t, e) -> crash(e));
//		GnomadConnector gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
//        GnomadConnector gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));

		SpliceAIConnector spliceAIConnector = new SpliceAIConnectorImpl(
				new SpliceAIDataSourceHttp(liftoverConnector)
		);
//		SpliceAIConnector spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);

		ConservationData conservationConnector = new ConservationData(databaseConnectService);

//		HgmdConnector hgmdConnector = new HgmdConnectorHttp();
		HgmdConnector hgmdConnector = new HgmdConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.hgmdConfigConnector);

//		ClinvarConnector clinvarConnector = new ClinvarConnectorHttp();
		ClinvarConnector clinvarConnector = new ClinvarConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.foromeConfigConnector);

		GTFConnector gtfConnector = new GTFConnectorImpl(
				new GTFDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector),
				(t, e) -> crash(e)
		);
//		GTFConnector gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> crash(e));

//		GTEXConnector gtexConnector = new GTEXConnectorHttp();
		GTEXConnector gtexConnector = new GTEXConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

//		PharmGKBConnector pharmGKBConnector = new PharmGKBConnectorHttp();
		PharmGKBConnector pharmGKBConnector = new PharmGKBConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

		AStorageHttp sourceHttp38 = new AStorageHttp(
				databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector
		);

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
				pharmGKBConnector,
				sourceHttp38
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
