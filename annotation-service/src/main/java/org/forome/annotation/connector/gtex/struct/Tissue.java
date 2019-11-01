package org.forome.annotation.connector.gtex.struct;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

public class Tissue {

	public final String name;
	public final float expression;
	public final float relExp;

	public Tissue(String name, float expression, float relExp) {
		this.name = name;
		this.expression = expression;
		this.relExp = relExp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tissue tissue = (Tissue) o;
		return Float.compare(tissue.expression, expression) == 0 &&
				Float.compare(tissue.relExp, relExp) == 0 &&
				Objects.equals(name, tissue.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, expression, relExp);
	}

	public String toJSON() {
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
		return String.format("%s: %s TPM",
				name,
				(expression >= 10.0f) ?
						new DecimalFormat("0.0", formatSymbols).format(expression) :
						new DecimalFormat("0.00", formatSymbols).format(expression)
		);
	}
}
