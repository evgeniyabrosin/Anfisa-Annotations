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
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.astorage.AStorageHttp;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.mysql.ClinvarConnectorMysql;
import org.forome.annotation.data.fasta.FastaSource;
import org.forome.annotation.data.gnomad.GnomadConnector;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtex.mysql.GTEXConnectorMysql;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.GTFConnectorImpl;
import org.forome.annotation.data.gtf.datasource.http.GTFDataSourceHttp;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.hgmd.mysql.HgmdConnectorMysql;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.pharmgkb.mysql.PharmGKBConnectorMysql;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnectorImpl;
import org.forome.annotation.data.spliceai.datasource.http.SpliceAIDataSourceHttp;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.TypeQuery;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.astorage.core.source.Source;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

public class CustomVariantMain {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	private final ServiceConfig serviceConfig;
	private final SSHConnectService sshTunnelService;
	private final DatabaseConnectService databaseConnectService;
	private final GnomadConnector gnomadConnector;
	private final SpliceAIConnector spliceAIConnector;
	private final HgmdConnector hgmdConnector;
	private final ClinvarConnector clinvarConnector;
	private final LiftoverConnector liftoverConnector;
	private final FastaSource fastaSource;
	private final GTFConnector gtfConnector;
	private final GTEXConnector gtexConnector;
	private final PharmGKBConnector pharmGKBConnector;
	private final AStorageHttp sourceHttp38;
	private final EnsemblVepService ensemblVepService;
	private final AnfisaConnector anfisaConnector;
	private final Processing processing;

	public CustomVariantMain() throws Exception {
		serviceConfig = new ServiceConfig();
		sshTunnelService = new SSHConnectService();
		databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);

		liftoverConnector = new LiftoverConnector();
		fastaSource = new FastaSource(databaseConnectService, serviceConfig.aStorageConfigConnector);

		FastaSource fastaSource = new FastaSource(databaseConnectService, serviceConfig.aStorageConfigConnector);

		gnomadConnector = null;
//		gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));
//      gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> crash(e));

		spliceAIConnector = new SpliceAIConnectorImpl(
				new SpliceAIDataSourceHttp(liftoverConnector)
		);
//		spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);

//		hgmdConnector = new HgmdConnectorHttp();
		hgmdConnector = new HgmdConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.hgmdConfigConnector);

//		clinvarConnector = new ClinvarConnectorHttp();
		clinvarConnector = new ClinvarConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.foromeConfigConnector);

		gtfConnector = new GTFConnectorImpl(
				new GTFDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector),
				liftoverConnector,
				(t, e) -> crash(e)
		);
//		gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> crash(e));

//		gtexConnector = new GTEXConnectorHttp();
		gtexConnector = new GTEXConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

//		pharmGKBConnector = new PharmGKBConnectorHttp();
		pharmGKBConnector = new PharmGKBConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

		sourceHttp38 = new AStorageHttp(
				databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector
		);

		ensemblVepService = new EnsemblVepExternalService((t, e) -> crash(e));
		anfisaConnector = new AnfisaConnector(
				gnomadConnector,
				spliceAIConnector,
				hgmdConnector,
				clinvarConnector,
				liftoverConnector,
				gtfConnector,
				gtexConnector,
				pharmGKBConnector,
				sourceHttp38,
				fastaSource
		);

		Source source = databaseConnectService.getSource(Assembly.GRCh37);

		processing = new Processing(source, anfisaConnector, TypeQuery.PATIENT_HG19);
	}

	//chr6:53140021 G>A
	public static void main(String[] args) {
		try {
			CustomVariantMain variantMain = new CustomVariantMain();

			ProcessingResult processingResult = variantMain.build(
					new VariantCustom(
							Chromosome.of("6"),
							53140021, 53140021,
							null, //TODO Ulitin V. не реализованно
							new Allele("A")
					)
			);
			log.debug("result: " + processingResult);

			System.exit(0);
		} catch (Throwable e) {
			crash(e);
		}
	}

	private ProcessingResult build(VariantCustom variant) throws ExecutionException, InterruptedException {
		MCase mCase = new MCase.Builder(Assembly.GRCh37, new LinkedHashMap<>(), Collections.emptyList()).build();

		JSONObject vepJson = ensemblVepService.getVepJson(variant).get();
		variant.setVepJson(vepJson);

		return processing.exec(mCase, variant);
	}

	public static void crash(Throwable e) {
		log.error("Application crashing", e);
		System.exit(1);
	}
}
