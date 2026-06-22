package de.tronka.justsync.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Event<T> {
    List<Consumer<T>> subscribers = new ArrayList<>();
    List<Predicate<T>> filters = new ArrayList<>();

    Event() {}

    /**
     * Register any event listeners here.
     *
     * @param subscriber
     */
    public void subscribe(Consumer<T> subscriber) {
        this.subscribers.add(subscriber);
    }

    /** Clear all event listeners. */
    public void clear() {
        this.subscribers.clear();
    }

    /**
     * Invoke the event here. All registered event listeners will be run.
     *
     * @param payload
     */
    public void invoke(T payload) {
        if (!shouldBroadcast(payload)) {
            return;
        }
        this.subscribers.forEach(subscriber -> subscriber.accept(payload));
    }

    /**
     * Register any filters here.
     *
     * @param filter
     */
    public void addFilter(Predicate<T> filter) {
        this.filters.add(filter);
    }

    /** Clear all filters. */
    public void clearFilter() {
        this.filters.clear();
    }

    private boolean shouldBroadcast(T payload) {
        for (Predicate<T> filter : this.filters) {
            if (!filter.test(payload)) {
                return false;
            }
        }
        return true;
    }
}
