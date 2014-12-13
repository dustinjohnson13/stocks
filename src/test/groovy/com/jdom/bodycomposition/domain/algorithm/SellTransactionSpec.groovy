package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.Stock
import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.MathUtil.toMoney

/**
 * Created by djohnson on 12/12/14.
 */
class SellTransactionSpec extends Specification {

    static def stock = new Stock(id: 1L)

    @Unroll
    def 'should return correct portfolio after applying'() {
        SellTransaction sell = new SellTransaction(stock, new Date(), 10, toMoney('$10'), toMoney('$5'))

        when: 'the transaction is applied'
        def actual = sell.apply(portfolio)

        then: 'the result portfolio has the correct number of shares'
        actual == expectedPortfolio

        where:
        portfolio                                                        | expectedPortfolio
        new Portfolio(toMoney('$200'), [new Position(stock, 11)] as Set) | new Portfolio(toMoney('$295'), [new Position(stock, 1)] as Set)
        new Portfolio(toMoney('$200'), [new Position(stock, 10)] as Set) | new Portfolio(toMoney('$295'), [] as Set)
        new Portfolio(toMoney('$200'), [new Position(stock, 10)] as Set) | new Portfolio(toMoney('$295'), [] as Set)
    }

    @Unroll
    def 'should throw exception when the transaction is invalid'() {
        SellTransaction sell = new SellTransaction(stock, new Date(), 10, toMoney('$10'), toMoney('$5'))

        when: 'the transaction is applied'
        def actual = sell.apply(portfolio)

        then: 'an exception is thrown'
        thrown Exception

        where:
        portfolio << [
                new Portfolio(toMoney('$200'), [new Position(stock, 9)] as Set), // Too few positions
                new Portfolio(toMoney('$200'), [new Position(stock, 1)] as Set), // Too few positions
                new Portfolio(toMoney('$200'), [] as Set) // No positions
        ]
    }
}
