package org.katacode.pipeline.engine;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Core factory generating stateless, parametric functional pipelines.
 * Emulates Apache Camel's core Enterprise Integration Patterns (EIP).
 */
public final class ComponentFactory {

    private ComponentFactory() {} // Pure utility container

    /**
     * Camel Pattern: Message Filter
     * Assesses a strict boolean Predicate against the message payload.
     * Halts downstream cascading immediately by returning null if the test fails.
     */
    public static <T> Function<Message<T>, Message<T>> createFilter(Predicate<T> criteria) {
        return message -> criteria.test(message.getBody()) ? message : null;
    }

    /**
     * Camel Pattern: Core Message Transformer
     * Mutates the inner payload body cleanly via a mapped function while retaining tracking headers.
     */
    public static <I, O> Function<Message<I>, Message<O>> createTransformer(Function<I, O> transformerLogic) {
        return message -> {
            O outputBody = transformerLogic.apply(message.getBody());
            return message.withBody(outputBody);
        };
    }

}
