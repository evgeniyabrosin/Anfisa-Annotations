package org.forome.annotation.connector.conservation.struct;

import org.junit.Assert;
import org.junit.Test;

public class ConservationTest {

    @Test
    public void test() {
        String expecteds[] = {"1.53166e-289", "1.00000e-29", "1.53166e-9", "0"};
        for (String expected: expecteds) {
            Double value = Conservation.convToL(expected);
            String actual = Conservation.convFromL(value);
            Assert.assertEquals(expected, actual);
        }
    }
}

