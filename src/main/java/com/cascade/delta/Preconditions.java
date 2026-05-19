package com.cascade.delta;

/**
 * Preconditions — Conditional logic for inventory delta application.
 *
 * <p>Prevents overselling by checking if the current stock satisfies
 * the minimum threshold before applying the delta. Same concept as
 * DynamoDB Conditional Writes.</p>
 *
 * <p>Example: An order for 5 PS5s sets {@code minStock = 5} to ensure
 * there are enough units before deducting.</p>
 */
public record Preconditions(int minStock) {

    /**
     * Checks if the current stock satisfies this precondition.
     *
     * @param currentStock the current inventory quantity
     * @return true if condition is met, false if order should be rejected
     */
    public boolean isSatisfied(int currentStock) {
        return currentStock >= minStock;
    }

    @Override
    public String toString() {
        return "Preconditions{minStock=" + minStock + "}";
    }
}
