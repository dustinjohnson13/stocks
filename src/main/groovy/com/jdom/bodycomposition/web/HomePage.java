package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.Stock;
import com.jdom.bodycomposition.domain.market.MarketReplay;
import com.jdom.bodycomposition.domain.algorithm.Portfolio;
import com.jdom.bodycomposition.domain.algorithm.impl.TestMsftAlgorithm;
import com.jdom.bodycomposition.service.DailySecurityDataDao;
import com.jdom.bodycomposition.service.SecurityService;
import com.jdom.util.TimeUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

public class HomePage extends WebPage {

   private static final Logger log = org.slf4j.LoggerFactory.getLogger(HomePage.class);

   private static final long serialVersionUID = 1L;

   @SpringBean
   private SecurityService bodyCompositionService;

   @SpringBean
   private DailySecurityDataDao dailySecurityDataDao;

   IModel<MarketReplay> scenarioModel = new Model<>();

   /**
    * Constructor that is invoked when page is invoked without a session.
    *
    * @param parameters Page parameters
    */
   public HomePage(final PageParameters parameters) {

      MarketReplay marketReplay = new MarketReplay();
      marketReplay.setInitialPortfolio(new Portfolio(500000L, 495L));
      marketReplay.setStartDate(new Date(TimeUtil.currentTimeMillis() - TimeUtil.MILLIS_PER_YEAR));
      marketReplay.setEndDate(TimeUtil.newDate());
      marketReplay.setAlgorithm(new TestMsftAlgorithm());
      scenarioModel.setObject(marketReplay);

      final AjaxLink<Void> updateSecurityDailyData = new AjaxLink<Void>("updateSecurityDailyData") {
         @Override
         public void onClick(AjaxRequestTarget target) {
            List<Stock> tickers = bodyCompositionService.getStocks();

            int totalTickersToUpdate = tickers.size();
            int numberUpdated = 0;
            for (Stock ticker : tickers) {
               try {
                  bodyCompositionService.updateHistoryData(ticker);
               } catch (Exception e) {
                  log.error("Unable to update ticker [" + ticker.getSymbol() + "]:", e);
               }

               numberUpdated++;
               log.info(String.format("Updated ticker: %s, %s/%s", ticker.getSymbol(), numberUpdated, totalTickersToUpdate));
            }
         }
      };
      add(updateSecurityDailyData);

      add(new AlgorithmProfilePanel("algorithmProfilePanel", scenarioModel));


   }
}
