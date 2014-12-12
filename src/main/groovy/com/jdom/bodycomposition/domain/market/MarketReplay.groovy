package com.jdom.bodycomposition.domain.market
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.broker.Brokers
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.SecurityService
import com.jdom.util.TimeUtil
import groovy.transform.ToString
/**
 * Created by djohnson on 12/6/14.
 */
@ToString
class MarketReplay implements Serializable {

    Portfolio initialPortfolio

    Date startDate

    Date endDate

    long commissionCost

    Algorithm algorithm

    List<PortfolioTransaction> transactions = []

    List<PortfolioValue> dailyPortfolios = []

    long valueChangePercent

    long duration

    void replay(final DailySecurityDataDao dailySecurityDataDao, final SecurityService securityService) {

        long start = System.currentTimeMillis()

        List<Stock> securities = securityService.getStocks()
        for (Iterator<Stock> iter = securities.iterator(); iter.hasNext();) {
            if (!algorithm.includeSecurity(iter.next())) {
                iter.remove()
            }
        }

        MarketEngine marketEngine = createMarketEngine(dailySecurityDataDao)
        Broker broker = Brokers.create(marketEngine, initialPortfolio, commissionCost)

        Date currentDate = startDate

        while (!endDate.before(currentDate)) {

            marketEngine.processDay(currentDate)

            dailyPortfolios.add(securityService.portfolioValue(broker.portfolio, currentDate))

            def dailySecurityDatas = dailySecurityDataDao.findBySecurityInAndDate(securities, currentDate)

            algorithm.actionsForDay(broker, dailySecurityDatas, currentDate)

            currentDate = TimeUtil.oneDayLater(currentDate)
        }

        duration = System.currentTimeMillis() - start
        transactions = broker.transactions
        valueChangePercent = resultPortfolio.percentChangeFrom(securityService.portfolioValue(initialPortfolio, startDate))
    }

    protected MarketEngine createMarketEngine(final DailySecurityDataDao dailySecurityDataDao) {
        return MarketEngines.create(dailySecurityDataDao)
    }

    public PortfolioValue getResultPortfolio() {
        return dailyPortfolios.isEmpty() ? null : dailyPortfolios.get(dailyPortfolios.size() - 1)
    }
}
