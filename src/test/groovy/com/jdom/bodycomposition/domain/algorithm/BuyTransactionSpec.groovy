package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.Stock
import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.MathUtil.toMoney

/**
 * Created by djohnson on 12/12/14.
 */
class BuyTransactionSpec extends Specification {

    static def stock = new Stock(id: 1L)

    @Unroll
    def 'should return correct portfolio after applying'() {
        BuyTransaction buy = new BuyTransaction(stock, new Date(), 10, toMoney('$10'), toMoney('$5'))

        when: 'the transaction is applied'
        def actual = buy.apply(portfolio)

        then: 'the result portfolio has the correct number of shares'
        actual == expectedPortfolio

        where:
        portfolio                                                        | expectedPortfolio
        Portfolio.newPortfolio(toMoney('$200'), [Position.newPosition(stock, 10)] as Set) | Portfolio.newPortfolio(toMoney('$95'), [Position.newPosition(stock, 20)] as Set)
        Portfolio.newPortfolio(toMoney('$105'))                                   | Portfolio.newPortfolio(toMoney('$0'), [Position.newPosition(stock, 10)] as Set)
    }

    @Unroll
    def 'should throw exception when the transaction is invalid'() {
        BuyTransaction buy = new BuyTransaction(stock, new Date(), 10, toMoney('$10'), toMoney('$5'))

        when: 'the transaction is applied'
        def actual = buy.apply(portfolio)

        then: 'an exception is thrown'
        thrown Exception

        where:
        portfolio << [
                Portfolio.newPortfolio(toMoney('$104'), [Position.newPosition(stock, 9)] as Set), // Not enough money
                Portfolio.newPortfolio(toMoney('$50'), [Position.newPosition(stock, 1)] as Set), // Not enough money
                Portfolio.newPortfolio(toMoney('$0'), [Position.newPosition(stock, 1)] as Set), // No money
        ]
    }
}
