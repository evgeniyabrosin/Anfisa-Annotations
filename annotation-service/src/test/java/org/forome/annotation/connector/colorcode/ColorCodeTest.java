package org.forome.annotation.connector.colorcode;

import org.forome.annotation.connector.anfisa.struct.ColorCode;
import org.junit.Assert;
import org.junit.Test;

public class ColorCodeTest {

    @Test
    public void test() {
        Assert.assertEquals(ColorCode.Code.YELLOW_CROSS, ColorCode.code(ColorCode.Shape.CROSS, ColorCode.Color.YELLOW));
        Assert.assertEquals(ColorCode.Code.RED_CROSS, ColorCode.code(ColorCode.Shape.CROSS, ColorCode.Color.RED));
        Assert.assertEquals(ColorCode.Code.YELLOW_CIRCLE, ColorCode.code(ColorCode.Shape.CIRCLE, ColorCode.Color.YELLOW));
        Assert.assertEquals(ColorCode.Code.GREEN_CIRCLE, ColorCode.code(ColorCode.Shape.CIRCLE, ColorCode.Color.GREEN));
        Assert.assertEquals(ColorCode.Code.RED_CIRCLE, ColorCode.code(ColorCode.Shape.CIRCLE, ColorCode.Color.RED));
    }
}
