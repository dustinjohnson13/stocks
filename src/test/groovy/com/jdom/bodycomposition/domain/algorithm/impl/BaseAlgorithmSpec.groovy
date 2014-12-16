package com.jdom.bodycomposition.domain.algorithm.impl

import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.bodycomposition.domain.broker.Broker
import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.MathUtil.toMoney
import static com.jdom.util.MathUtil.toPercentage

/**
 * Created by djohnson on 12/17/14.
 */
class BaseAlgorithmSpec extends Specification {

    @Unroll
    def 'should calculate correct number of shares for percentage of portfolio: #percentage'() {

        final Portfolio portfolio = new Portfolio(toMoney('$200'))
        final Date date = new Date()
        final Set<PositionValue> positions

        PortfolioValue portfolioValue = new PortfolioValue(portfolio, date, [
              new PositionValue(new Position(new Stock(id: 1L), 5), date, toMoney('$19.50'))] as Set)

        given: 'a portfolio is worth $297.50'
        assert portfolioValue.marketValue() == toMoney('$297.50')

        and: 'a broker that charges #commissionCost for commissions'
        Broker broker = Mock()
        broker.getCommissionCost() >> toMoney(commissionCost)

        when: 'number of shares for a stock with price #sharePrice is requested with a max value of percentage of portfolio '
        int shares = BaseAlgorithm.sharesForPortfolioPercentage(broker, portfolioValue, toMoney(sharePrice), toPercentage(percentage))

        then: 'the correct number of shares is returned'
        shares == expectedShares

        where:
        commissionCost | sharePrice | percentage | expectedShares
        '$4.95'        | '$13.17'   | '100%'     | 22
        '$4.95'        | '$13.17'   | '10%'      | 1
        '$4.95'        | '$13.17'   | '6.1%'     | 1
        '$4.95'        | '$13.17'   | '6%'       | 0
        '$4.95'        | '$13.17'   | '1%'       | 0
    }
}
