package org.forome.annotation.connector.anfisa.struct;

import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.clinvar.struct.ClinvarResult;

import java.util.List;

public class Record {

	public List<ClinvarResult> clinvarResults;
	public HgmdConnector.Data hgmdData;

}
