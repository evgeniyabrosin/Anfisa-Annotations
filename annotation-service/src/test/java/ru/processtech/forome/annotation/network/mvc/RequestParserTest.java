package ru.processtech.forome.annotation.network.mvc;

import org.junit.Assert;
import org.junit.Test;
import ru.processtech.forome.annotation.controller.utils.RequestParser;
import ru.processtech.forome.annotation.exception.ServiceException;

public class RequestParserTest {

	@Test
	public void test() throws Exception {
		Assert.assertEquals("1", RequestParser.toChromosome("1"));
		Assert.assertEquals("1", RequestParser.toChromosome("chr1"));
		Assert.assertEquals("5", RequestParser.toChromosome("5"));
		Assert.assertEquals("5", RequestParser.toChromosome("chr5"));
		Assert.assertEquals("23", RequestParser.toChromosome("23"));
		Assert.assertEquals("23", RequestParser.toChromosome("chr23"));
		Assert.assertEquals("M", RequestParser.toChromosome("M"));
		Assert.assertEquals("X", RequestParser.toChromosome("X"));
		Assert.assertEquals("Y", RequestParser.toChromosome("Y"));

		try {
			RequestParser.toChromosome("");
			Assert.fail();
		} catch (ServiceException ignore) {
		}

		try {
			RequestParser.toChromosome("0");
			Assert.fail();
		} catch (ServiceException ignore) {
		}

		try {
			RequestParser.toChromosome("24");
			Assert.fail();
		} catch (ServiceException ignore) {
		}

		try {
			RequestParser.toChromosome("chr0");
			Assert.fail();
		} catch (ServiceException ignore) {
		}

		try {
			RequestParser.toChromosome("chr24");
			Assert.fail();
		} catch (ServiceException ignore) {
		}

		try {
			RequestParser.toChromosome("m");
			Assert.fail();
		} catch (ServiceException ignore) {
		}

		try {
			RequestParser.toChromosome("A");
			Assert.fail();
		} catch (ServiceException ignore) {
		}
	}
}
