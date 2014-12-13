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
        new Portfolio(toMoney('$200'), [new Position(stock, 10)] as Set) | new Portfolio(toMoney('$95'), [new Position(stock, 20)] as Set)
        new Portfolio(toMoney('$105'))                                   | new Portfolio(toMoney('$0'), [new Position(stock, 10)] as Set)
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
                new Portfolio(toMoney('$104'), [new Position(stock, 9)] as Set), // Not enough money
                new Portfolio(toMoney('$50'), [new Position(stock, 1)] as Set), // Not enough money
                new Portfolio(toMoney('$0'), [new Position(stock, 1)] as Set), // No money
        ]
    }
}
