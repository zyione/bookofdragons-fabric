package net.magister.liberanimalium.anim.v2;

import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public record Ambit(long now, IntUnaryOperator random, String clipPrefix, Predicate<String> clipExists, boolean layerIdle) {
}
