package org.forome.annotation.connector.hgmd.struct;

public class HgmdPmidRow {

	public final String disease;
	public final String pmid;
	public final String tag;

	public HgmdPmidRow(String disease, String pmid, String tag) {
		this.disease = disease;
		this.pmid = pmid;
		this.tag = tag;
	}
}
