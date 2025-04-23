// src/main/java/com/example/OrderFetcher.java
package com.example;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.order.OrderPagedQueryResponse;
import com.commercetools.api.models.order.Order;
import io.vrap.rmf.base.client.ApiHttpResponse;

import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OrderFetcher {

    private final ProjectApiRoot apiRoot;
    private static final int PAGE_SIZE = 500;
    private static final int MAX_TOTAL = 10_000;
    private static final Duration MIN_DURATION = Duration.ofSeconds(1);

    public OrderFetcher(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    public List<Order> fetchAllOrders(ZonedDateTime from, ZonedDateTime to) {
        List<Order> allOrders = new ArrayList<>();
        recursiveFetch(from, to, allOrders);
        return allOrders;
    }

    private void recursiveFetch(ZonedDateTime start, ZonedDateTime end, List<Order> results) {
        long count = countOrders(start, end);

        if (count < MAX_TOTAL) {
            if (Duration.between(start, end).compareTo(MIN_DURATION) <= 0) {
                results.addAll(fetchByIdPagination(start));
            } else {
                results.addAll(fetchByOffsetPagination(start, end, count));
            }
        } else {
            ZonedDateTime mid = start.plus(Duration.between(start, end).dividedBy(2));
            recursiveFetch(start, mid, results);
            recursiveFetch(mid.plusNanos(1), end, results); // avoid overlap
        }
    }

    private long countOrders(ZonedDateTime from, ZonedDateTime to) {
        ApiHttpResponse<OrderPagedQueryResponse> response = apiRoot.orders()
            .get()
            .withWhere("createdAt >= :from AND createdAt <= :to")
            .withPredicateVar("from", from.toString())
            .withPredicateVar("to", to.toString())
            .withLimit(1)
            .executeBlocking();

        return response.getBody().getTotal();
    }

    private List<Order> fetchByOffsetPagination(ZonedDateTime from, ZonedDateTime to, long total) {
        List<Order> orders = new ArrayList<>();
        long pages = (long) Math.ceil((double) total / PAGE_SIZE);

        for (int i = 0; i < pages; i++) {
            var response = apiRoot.orders()
                .get()
                .withWhere("createdAt >= :from AND createdAt <= :to")
                .withPredicateVar("from", from.toString())
                .withPredicateVar("to", to.toString())
                .withLimit(PAGE_SIZE)
                .withOffset(i * PAGE_SIZE)
                .withSort("createdAt asc")
                .executeBlocking()
                .getBody();

            orders.addAll(response.getResults());
        }

        return orders;
    }

    private List<Order> fetchByCreatedAtPagination(ZonedDateTime from, ZonedDateTime to) {
        List<Order> orders = new ArrayList<>();
        ZonedDateTime lastCreatedAt = null;
        String lastId = null;

        while (true) {
            StringBuilder where = new StringBuilder("createdAt >= :from AND createdAt <= :to");
            if (lastCreatedAt != null) {
                where.append(" AND (createdAt > :lastCreatedAt OR (createdAt = :lastCreatedAt AND id > :lastId))");
            }

            var request = apiRoot.orders().get()
                .withWhere(where.toString())
                .withPredicateVar("from", from.toString())
                .withPredicateVar("to", to.toString())
                .withSort("createdAt asc")
                .addSort("id asc")
                .withLimit(PAGE_SIZE);

            if (lastCreatedAt != null) {
                request = request
                    .withPredicateVar("lastCreatedAt", lastCreatedAt.toString())
                    .withPredicateVar("lastId", lastId);
            }

            var response = request.executeBlocking().getBody();
            if (response.getResults().isEmpty()) break;

            orders.addAll(response.getResults());

            Order last = response.getResults().get(response.getResults().size() - 1);
            lastCreatedAt = last.getCreatedAt();
            lastId = last.getId();
        }

        return orders;
    }

    private List<Order> fetchByIdPagination(ZonedDateTime timestamp) {
        List<Order> orders = new ArrayList<>();
        String lastId = null;

        while (true) {
            String where = "createdAt = :timestamp";
            if (lastId != null) {
                where += " AND id > :lastId";
            }

            var request = apiRoot.orders().get()
                .withWhere(where)
                .withPredicateVar("timestamp", timestamp.toString())
                .withLimit(PAGE_SIZE)
                .withSort("id asc");

            if (lastId != null) {
                request = request.withPredicateVar("lastId", lastId);
            }

            var response = request.executeBlocking().getBody();
            if (response.getResults().isEmpty()) break;

            orders.addAll(response.getResults());
            lastId = response.getResults().get(response.getResults().size() - 1).getId();
        }

        return orders;
    }
}
