package org.katacode.pipeline.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The immutable data envelope traveling through the pipeline execution chain.
 * Tracks payload bodies and transactional metadata headers.
 *
 * @param <B> The explicit type of the payload body.
 */
public final class Message<B> {
    private final B body;
    private final Map<String, Object> headers;

    public Message(B body) {
        this(body, Collections.emptyMap());
    }

    public Message(B body, Map<String, Object> headers) {
        this.body = body;
        // Ensure absolute immutability to adhere to strict functional durability
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    }

    public B getBody() {
        return body;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getHeader(String name, Class<T> type) {
        Object value = headers.get(name);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Fluent factory method to derive a new message window with a mutated body 
     * while flawlessly preserving existing tracking headers.
     */
    public <N> Message<N> withBody(N newBody) {
        return new Message<>(newBody, this.headers);
    }

}
