package com.jdom.bodycomposition.domain.market
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.impl.BuyAt52WeekLowSellAtGivenPercentUpOrDown
import com.jdom.bodycomposition.domain.algorithm.impl.BuyRandomSellHigher
import com.jdom.bodycomposition.domain.algorithm.impl.TradingMarketsAlgorithm
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.broker.Brokers
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.DailySecurityMetricsDao
import com.jdom.bodycomposition.service.SecurityService
import com.jdom.util.TimeUtil
import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.jdom.util.MathUtil.formatPercentage
/**
 * Created by djohnson on 12/6/14.
 */
@ToString
class MarketReplay implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(MarketReplay)

    Portfolio initialPortfolio

    Date startDate

    Date endDate

    long commissionCost

    Algorithm algorithm

    List<PortfolioTransaction> transactions = []

    List<PortfolioValue> dailyPortfolios = []

    long valueChangePercent

    long duration

    void replay(final DailySecurityDataDao dailySecurityDataDao,
                final DailySecurityMetricsDao dailySecurityMetricsDao,
                final SecurityService securityService) {

        Map<String, Class<? extends Algorithm>> algorithms = [:]

        50.times {
            algorithms.put("tradingMarkets_${it}", TradingMarketsAlgorithm.class)
            algorithms.put("buy52WeekLow_${it}", BuyAt52WeekLowSellAtGivenPercentUpOrDown.class)
            algorithms.put("buyRandom_${it}", BuyRandomSellHigher.class)
        }

        List<AlgorithmConfiguration> configurations = []
        algorithms.each { name, algorithmClass ->
            def configuration = new AlgorithmConfiguration(
                  outputFile: new File("/home/djohnson/Desktop/${name}_results_${System.currentTimeMillis()}.txt"),
                  algorithm: algorithmClass.newInstance())
            configurations.add(configuration)
        }

        def bearDates = [TimeUtil.dateFromDashString('2008-03-06'), TimeUtil.dateFromDashString('2009-03-06')]
        def bullDates = [TimeUtil.dateFromDashString('2012-06-01'), TimeUtil.dateFromDashString('2013-06-01')]
        def neitherDates = [TimeUtil.dateFromDashString('2004-12-17'), TimeUtil.dateFromDashString('2005-12-17')]

        def marketTypes = ['Bear': bearDates, 'Bull': bullDates, 'Neither': neitherDates]

        marketTypes.each { type, dates ->

//            final int numberOfRuns = 150
//            final int numberOfRuns = 10
            final int numberOfRuns = 1
            this.startDate = dates[0]
            this.endDate = dates[1]

            configurations.each {
                it.outputFile.append("\n\n${type} Market:\nResults\n")
            }

            for (int i = 0; i < numberOfRuns; i++) {

                log.info "Running scenario type ${type} Market, run number ${i + 1}/${numberOfRuns}"

                long start = System.currentTimeMillis()

                MarketEngine marketEngine = createMarketEngine(dailySecurityDataDao)

                configurations.each {
                    it.broker = Brokers.create(marketEngine, initialPortfolio, commissionCost)
                }

                Date currentDate = startDate

                while (!endDate.before(currentDate)) {

                    marketEngine.processDay(currentDate)

                    def dailySecurityMetrics = dailySecurityMetricsDao.findByDate(currentDate)
                    def dailySecurityDatas = dailySecurityDataDao.findByDate(currentDate)

                    configurations.each {
                        def portfolioValue = securityService.portfolioValue(it.broker.portfolio, currentDate)
                        it.dailyPortfolios.add(portfolioValue)
                        it.algorithm.actionsForDay(it.broker, portfolioValue, new ArrayList<DailySecurityData>(dailySecurityDatas), new ArrayList<DailySecurityData>(dailySecurityMetrics), new Date(currentDate.getTime()))
                    }

                    currentDate = TimeUtil.oneDayLater(currentDate)
                }

                duration = System.currentTimeMillis() - start
                def initialPortfolioValue = securityService.portfolioValue(initialPortfolio, startDate)
                configurations.each {
                    it.transactions = it.broker.transactions
                    def resultPortfolio = securityService.portfolioValue(it.broker.portfolio, endDate)
                    long valueChangePercent = resultPortfolio.percentChangeFrom(initialPortfolioValue)
                    it.percentChanges += valueChangePercent

                    if (valueChangePercent > it.maxPercentChange) {
                        it.maxPercentChange = valueChangePercent
                    } else if (valueChangePercent < it.minPercentChange) {
                        it.minPercentChange = valueChangePercent
                    }
                    it.outputFile.append("Run [${i}]\n")
                    it.outputFile.append("${initialPortfolioValue}\n")
                    it.outputFile.append("${resultPortfolio}\n")
                    it.outputFile.append("Percent change: ${formatPercentage(valueChangePercent)}")
                    it.outputFile.append('\n\n')
                }
            }

            configurations.each {
                long averagePercentChange = (long) (it.percentChanges / numberOfRuns)
                it.outputFile.append("Average percent change: ${formatPercentage(averagePercentChange)}")
                it.outputFile.append("\nMaximum percent change: ${formatPercentage(it.maxPercentChange)}")
                it.outputFile.append("\nMinimum percent change: ${formatPercentage(it.minPercentChange)}")
            }
        }


    }

    protected MarketEngine createMarketEngine(final DailySecurityDataDao dailySecurityDataDao) {
        return MarketEngines.create(dailySecurityDataDao)
    }

    public PortfolioValue getResultPortfolio() {
        return dailyPortfolios.isEmpty() ? null : dailyPortfolios.get(dailyPortfolios.size() - 1)
    }

    static class AlgorithmConfiguration {
        Broker broker
        Algorithm algorithm
        File outputFile
        List<PortfolioTransaction> transactions = []
        List<PortfolioValue> dailyPortfolios = []
        long percentChanges = 0l
        long maxPercentChange = Long.MIN_VALUE
        long minPercentChange = Long.MAX_VALUE
    }
}
