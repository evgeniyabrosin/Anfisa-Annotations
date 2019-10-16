package org.forome.annotation.connector.anfisa.struct;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

public class ColorCode {

    public enum Shape {

        CROSS("-cross"),
        CIRCLE("");

        public final String shape;

        Shape(String s) {
            this.shape = s;
        }

        @Override
        public String toString() {
            return shape;
        }
    }

    public enum Color {

        GREY("grey"),
        GREEN("green"),
        RED("red"),
        YELLOW("yellow");

        public final String color;

        Color(String s) {
            this.color = s;
        }

        @Override
        public String toString() {
            return color;
        }
    }

    public enum Code {

        GREEN_CIRCLE(Shape.CIRCLE, Color.GREEN),

        GREY_CIRCLE(Shape.CIRCLE, Color.GREY),

        RED_CROSS(Shape.CROSS, Color.RED),

        RED_CIRCLE(Shape.CIRCLE, Color.RED),

        YELLOW_CROSS(Shape.CROSS, Color.YELLOW),

        YELLOW_CIRCLE(Shape.CIRCLE, Color.YELLOW);

        public final String code;

        Code(Shape shape, Color color) {
            this.code = stringValue(shape, color);
        }

        @Override
        public String toString() {
            return code;
        }
    }

    private static String stringValue(Shape shape, Color color) {
        return String.valueOf(color) + String.valueOf(shape);
    }

    public static Code code(Shape shape, Color color) {
        switch (shape) {
            case CIRCLE:
                switch (color) {
                    case RED:
                        return Code.RED_CIRCLE;
                    case YELLOW:
                        return Code.YELLOW_CIRCLE;
                    case GREEN:
                        return Code.GREEN_CIRCLE;
                }
            case CROSS:
                switch (color) {
                    case RED:
                        return Code.RED_CROSS;
                    case YELLOW:
                        return Code.YELLOW_CROSS;
                    case GREEN:
                        return Code.YELLOW_CROSS;
                }
        }
        throw new IllegalArgumentException(stringValue(shape, color));
    }

    private static final ImmutableMap<String, ImmutableMap<String, Integer>>
            inSilicoPredictions = new ImmutableMap.Builder()
            .put(
                    "polyphen_prediction", ImmutableMap.of(
                            "benign", 10,
                            "possibly_damaging", 20,
                            "probably_damaging", 20,
                            "damaging", 30
                    ))

            .put(
                    "polyphen2_hvar_pred", ImmutableMap.of(
                            "B", 10,
                            "P", 20,
                            "D", 30
                    ))

            .put(
                    "polyphen2_hdiv_pred", ImmutableMap.of(
                            "B", 10,
                            "P", 20,
                            "D", 30
                    ))

            .put(
                    "sift_prediction", ImmutableMap.of(
                            "tolerated", 10,
                            "deleterious", 30
                    ))

            .put(
                    "sift_pred", ImmutableMap.of(
                            "T", 10,
                            "D", 30
                    ))

            .put(
                    "mutationtaster_pred", ImmutableMap.of(
                            "P", 10,
                            "N", 10,
                            "D", 30,
                            "A", 30
                    ))

            .put(
                    "mutationassessor_pred", ImmutableMap.of(
                            "L", 10,
                            "N", 10,
                            "M", 30,
                            "H", 30
                    ))

            .put(
                    "fathmm_pred", ImmutableMap.of(
                            "T", 10,
                            "D", 30
                    )).build();

    public static int inSilicoPrediction(String tool, String value) {
        if (value == null)
            return 0;

        Map<String, Integer> translationTable = inSilicoPredictions.get(tool);
        Integer translatedValue = translationTable.get(value);
        if (translatedValue == null)
            return 0;
        return translatedValue;
    }

    public static Set<String> allInSilicoTools() {
        return inSilicoPredictions.keySet();
    }

}
