package com.jdom.bodycomposition.domain.market.orders
import com.jdom.bodycomposition.domain.BaseSecurity
/**
 * Created by djohnson on 12/10/14.
 */
final class Orders {

    private Orders() {
    }

    static BuyMarketOrder newBuyMarketOrder(final int shares, final BaseSecurity security) {
        return new BuyMarketOrderImpl(shares, security, Qualifier.NONE)
    }

    static SellMarketOrder newSellMarketOrder(final int shares, final BaseSecurity security) {
        return new SellMarketOrderImpl(shares, security, Qualifier.NONE)
    }

    static LimitOrder newBuyLimitOrder(final int shares,
                                       final BaseSecurity security,
                                       final long price,
                                       final Duration duration) {
        return new BuyLimitOrderImpl(shares, security, price, duration, Qualifier.NONE)
    }

    static LimitOrder newSellLimitOrder(
            final int shares, final BaseSecurity security, final long price, final Duration duration) {
        return new SellLimitOrderImpl(shares, security, price, duration, Qualifier.NONE)
    }

    static BuyStopOrder newBuyStopOrder(final int shares,
                                       final BaseSecurity security,
                                       final long stopPrice,
                                       final Duration duration) {
        return new BuyStopOrderImpl(shares, security, stopPrice, duration, Qualifier.NONE)
    }

    static SellStopOrder newSellStopOrder(
            final int shares, final BaseSecurity security, final long stopPrice, final Duration duration) {
        return new SellStopOrderImpl(shares, security, stopPrice, duration, Qualifier.NONE)
    }

    static BuyStopLimitOrder newBuyStopLimitOrder(final int shares,
                                     final BaseSecurity security,
                                     final long stopPrice, final long price,
                                     final Duration duration) {
        return new BuyStopLimitOrderImpl(shares, security, stopPrice, price, duration, Qualifier.NONE)
    }

    static SellStopLimitOrder newSellStopLimitOrder(
            final int shares, final BaseSecurity security, final long stopPrice, final long price, final Duration duration) {
        return new SellStopLimitOrderImpl(shares, security, stopPrice, price, duration, Qualifier.NONE)
    }

    static BuyMarketOnCloseOrder newBuyMarketOnCloseOrder(final int shares, final BaseSecurity security) {
        return new BuyMarketOnCloseOrderImpl(shares, security, Qualifier.NONE)
    }

    static SellMarketOnCloseOrder newSellMarketOnCloseOrder(final int shares, final BaseSecurity security) {
        return new SellMarketOnCloseOrderImpl(shares, security, Qualifier.NONE)
    }

    private static class BaseOrder implements Order {
        final int shares
        final BaseSecurity security
        // support ALL_OR_NONE?
        final Qualifier qualifier = Qualifier.NONE
        //    Advanced orders:
//    Trailing Stop,
//    One Cancels Other (Stock-Option),
//    One Cancels Other (Stock-Stock),
//    One Triggers Other (Stock-Option),
//    One Triggers Other (Stock-Stock)

        // Other order types:
//    Sell Short <- This involves fees which cant really be known, would have to estimate about 5% of the value
// of the trade annualized, charged pro-rated on a daily basis:
//        $11.00 x 10,000 shares = $110,000 (trade value)
//        $110,000 x 5% = $5,550 (annualized) / 360 = $15.27 daily charge
//    Buy to Cover

        protected BaseOrder(int shares, BaseSecurity security, Qualifier qualifier) {
            this.shares = shares
            this.security = security
            this.qualifier = qualifier
        }
    }

    private static class BaseOrderWithDuration extends BaseOrder {
        final Duration duration

        protected BaseOrderWithDuration(int shares, BaseSecurity security, Duration duration, Qualifier qualifier) {
            super(shares, security, qualifier)
            this.duration = duration
        }
    }


    private static class BaseMarketOrder extends BaseOrder implements MarketOrder {
        protected BaseMarketOrder(int shares, BaseSecurity security, Qualifier qualifier) {
            super(shares, security, qualifier)
        }
    }

    private static class BaseLimitOrder extends BaseOrderWithDuration implements LimitOrder {
        final long price

        protected BaseLimitOrder(int shares, BaseSecurity security, long price, Duration duration, Qualifier qualifier) {
            super(shares, security, duration, qualifier)
            this.price = price
        }
    }

    private static class BaseStopOrder extends BaseOrderWithDuration implements StopOrder {
        final long stopPrice

        protected BaseStopOrder(int shares, BaseSecurity security, long stopPrice, Duration duration, Qualifier qualifier) {
            super(shares, security, duration, qualifier)
            this.stopPrice = stopPrice
        }
    }

    private static class BaseStopLimitOrder extends BaseLimitOrder implements StopLimitOrder {
        final long stopPrice

        protected BaseStopLimitOrder(int shares, BaseSecurity security, long stopPrice, long price, Duration duration, Qualifier qualifier) {
            super(shares, security, price, duration, qualifier)
            this.stopPrice = stopPrice
        }
    }

    private static class BuyLimitOrderImpl extends BaseLimitOrder implements BuyLimitOrder {
        private BuyLimitOrderImpl(int shares, BaseSecurity security, long price, Duration duration, Qualifier qualifier) {
            super(shares, security, price, duration, qualifier)
        }
    }

    private static class SellLimitOrderImpl extends BaseLimitOrder implements SellLimitOrder {
        private SellLimitOrderImpl(int shares, BaseSecurity security, long price, Duration duration, Qualifier qualifier) {
            super(shares, security, price, duration, qualifier)
        }
    }

    private static class BuyMarketOrderImpl extends BaseMarketOrder implements BuyMarketOrder {
        private BuyMarketOrderImpl(int shares, BaseSecurity security, Qualifier qualifier) {
            super(shares, security, qualifier)
        }
    }

    private static class SellMarketOrderImpl extends BaseMarketOrder implements SellMarketOrder {
        private SellMarketOrderImpl(int shares, BaseSecurity security, Qualifier qualifier) {
            super(shares, security, qualifier)
        }
    }

    private static class BuyStopOrderImpl extends BaseStopOrder implements BuyStopOrder {
        private BuyStopOrderImpl(int shares, BaseSecurity security, long stopPrice, Duration duration, Qualifier qualifier) {
            super(shares, security, stopPrice, duration, qualifier)
        }
    }

    private static class SellStopOrderImpl extends BaseStopOrder implements SellStopOrder {
        private SellStopOrderImpl(int shares, BaseSecurity security, long stopPrice, Duration duration, Qualifier qualifier) {
            super(shares, security, stopPrice, duration, qualifier)
        }
    }

    private static class BuyStopLimitOrderImpl extends BaseStopLimitOrder implements BuyStopLimitOrder {
        private BuyStopLimitOrderImpl(int shares, BaseSecurity security, long stopPrice, long price, Duration duration, Qualifier qualifier) {
            super(shares, security, stopPrice, price, duration, qualifier)
        }
    }

    private static class SellStopLimitOrderImpl extends BaseStopLimitOrder implements SellStopLimitOrder {
        private SellStopLimitOrderImpl(int shares, BaseSecurity security, long stopPrice, long price, Duration duration, Qualifier qualifier) {
            super(shares, security, stopPrice, price, duration, qualifier)
        }
    }

    private static class BuyMarketOnCloseOrderImpl extends BaseMarketOrder implements BuyMarketOnCloseOrder {
        private BuyMarketOnCloseOrderImpl(int shares, BaseSecurity security, Qualifier qualifier) {
            super(shares, security, qualifier)
        }
    }

    private static class SellMarketOnCloseOrderImpl extends BaseMarketOrder implements SellMarketOnCloseOrder {
        private SellMarketOnCloseOrderImpl(int shares, BaseSecurity security, Qualifier qualifier) {
            super(shares, security, qualifier)
        }
    }
}
