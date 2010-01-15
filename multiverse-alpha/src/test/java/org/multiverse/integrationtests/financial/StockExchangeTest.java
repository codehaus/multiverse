package org.multiverse.integrationtests.financial;

import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.transactional.collections.TransactionalLinkedList;
import org.multiverse.utils.TodoException;

import java.util.LinkedList;
import java.util.List;

/**
 * Simulates a financial stock exchange.
 *
 * @author Peter Veentjer.
 */
public class StockExchangeTest {

    private final int traderCount = 1000;

    private StockExchange exchange;

    @Test
    public void test() {
        exchange = new StockExchange();
        exchange.register(new Stock());
        exchange.register(new Stock());
        exchange.register(new Stock());

        Trader trader;

        long totalPrice = exchange.totalSharePrice();

    }

    private class TraderThread extends TestThread {


        public TraderThread(int id) {
            super("TraderThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @TransactionalObject
    private class Trader {

        private final int initialCapapital;

        private List<Stock> trustedStocks = new LinkedList<Stock>();
        private int profit = 0;

        private Trader(int initialCapapital) {
            this.initialCapapital = initialCapapital;
        }

        public void once() {

        }
    }

    @TransactionalObject
    private class StockExchange {

        private List<Stock> listing = new TransactionalLinkedList<Stock>();
        private List<OrderQueue> orderQueues = new TransactionalLinkedList<OrderQueue>();

        //private

        public List<Stock> getListing() {
            return listing;
        }

        /**
         * Calculates the total stock price of all the stocks.
         *
         * @return the total.
         */
        public long totalSharePrice() {
            long total = 0;
            for (Stock stock : listing) {
                total += stock.shares * stock.parValue;
            }
            return total;
        }

        /**
         * @param stock
         * @return
         */
        public long getPrice(Stock stock) {
            throw new TodoException();
        }

        public long getSupply(Stock stock) {
            long result = 0;
            for (Order order : getOrderQueue(stock).unprocessedOrders) {
                if (order.isSellOrder()) {
                    result += order.amount;
                }
            }
            return result;
        }

        public long demand(Stock stock) {
            long result = 0;
            for (Order order : getOrderQueue(stock).unprocessedOrders) {
                if (order.isBuyOrder()) {
                    result += order.amount;
                }
            }
            return result;
        }

        public void placeBuyOrder(Trader trader, Stock stock, int amount, int price) {
            OrderQueue orderQueue = getOrderQueue(stock);


            Order order = new Order(trader, stock, amount, price, true);
            getOrderQueue(stock).unprocessedOrders.add(order);
        }

        public void placeSellOrder(Trader trader, Stock stock, int amount, int price) {
            Order order = new Order(trader, stock, amount, price, false);
            getOrderQueue(stock).unprocessedOrders.add(order);
        }

        private OrderQueue getOrderQueue(Stock stock) {
            for (OrderQueue orderQueue : orderQueues) {
                if (orderQueue.stock == stock) {
                    return orderQueue;
                }
            }

            return null;
        }

        /**
         * Registers a new stock on the StockExchange.
         * <p/>
         * No check is done on duplicates
         *
         * @param stock the stock to register.
         */
        public void register(Stock stock) {
            listing.add(stock);
            orderQueues.add(new OrderQueue(stock));
        }


        @TransactionalObject
        public class OrderQueue {

            private final Stock stock;
            private final List<Order> unprocessedOrders = new TransactionalLinkedList<Order>();

            public OrderQueue(Stock stock) {
                this.stock = stock;
            }
        }
    }

    @TransactionalObject
    public class Order {

        private final Trader trader;
        private final Stock stock;
        private final int amount;
        private final int price;
        private boolean buy;

        public Order(Trader trader, Stock stock, int amount, int price, boolean buy) {
            this.trader = trader;
            this.stock = stock;
            this.amount = amount;
            this.price = price;
            this.buy = buy;
        }

        public boolean isBuyOrder() {
            return buy;
        }

        public boolean isSellOrder() {
            return !buy;
        }

        public Trader getTrader() {
            return trader;
        }

        public Stock getStock() {
            return stock;
        }

        public int getAmount() {
            return amount;
        }

        public int getPrice() {
            return price;
        }

        public void execute() {

        }
    }

    @TransactionalObject
    private class Stock {

        private final Company company;

        private final long shares;

        private final long parValue;

        public Stock() {
            company = new Company();
            shares = 1000 * 1000;
            parValue = 10;
        }

        private Stock(Company company, long shares, long parValue) {
            this.company = company;
            this.shares = shares;
            this.parValue = parValue;
        }

        /**
         * The company that
         *
         * @return
         */
        public Company getCompany() {
            return company;
        }

        /**
         * Returns the total number of shares available.
         *
         * @return the number of shares available.
         */
        public long getShares() {
            return shares;
        }

        /**
         * Returns the par value (the initial price) of each share. It also is the minimal value possible for a share,
         * because each share is always backed up by the initial capital of the founders.
         *
         * @return the par value.
         */
        public long getParValue() {
            return parValue;
        }
    }

    /**
     * A Company; a Business Entity that is allowed to go the to Stock market to register {@link Stock}.
     */
    @TransactionalObject
    private class Company {

    }
}
