package ru.processtech.forome.annotation.connector.anfisa.struct;

import ru.processtech.forome.annotation.connector.clinvar.struct.ClinvarResult;
import ru.processtech.forome.annotation.connector.hgmd.HgmdConnector;

import java.util.List;

public class Record {

	public List<ClinvarResult> clinvarResults;
	public HgmdConnector.Data hgmdData;

}
