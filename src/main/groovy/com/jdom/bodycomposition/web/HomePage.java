package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.BaseTicker;
import com.jdom.bodycomposition.domain.YahooStockTicker;
import com.jdom.bodycomposition.domain.algorithm.Portfolio;
import com.jdom.bodycomposition.domain.algorithm.TestMsftAlgorithm;
import com.jdom.bodycomposition.service.YahooStockTickerDataDao;
import com.jdom.bodycomposition.service.YahooStockTickerService;
import com.jdom.util.TimeUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HomePage extends WebPage {

   private static final Logger log = org.slf4j.LoggerFactory.getLogger(HomePage.class);

   private static final long serialVersionUID = 1L;

   @SpringBean
   private YahooStockTickerService bodyCompositionService;

   @SpringBean
   private YahooStockTickerDataDao yahooStockTickerDataDao;

   IModel<Portfolio> portfolioModel = new Model<>(new Portfolio(0l));

   /**
    * Constructor that is invoked when page is invoked without a session.
    *
    * @param parameters Page parameters
    */
   public HomePage(final PageParameters parameters) {

      final AjaxLink<Void> updateTickerData = new AjaxLink<Void>("updateTickerData") {
         @Override
         public void onClick(AjaxRequestTarget target) {
            List<YahooStockTicker> tickers = bodyCompositionService.getTickers();

            int totalTickersToUpdate = tickers.size();
            int numberUpdated = 0;
            for (YahooStockTicker ticker : tickers) {
               try {
                  bodyCompositionService.updateHistoryData(ticker);
               } catch (Exception e) {
                  log.error("Unable to update ticker [" + ticker.getTicker() + "]:", e);
               }

               numberUpdated++;
               log.info(String.format("Updated ticker: %s, %s/%s", ticker.getTicker(), numberUpdated, totalTickersToUpdate));
            }
         }
      };
      add(updateTickerData);

      final AjaxLink<Void> profileAlgorithm = new AjaxLink<Void>("profileAlgorithm") {
         @Override
         public void onClick(AjaxRequestTarget target) {
            final Date startDate = TimeUtil.dateFromDashString("2003-11-28");
            final Date endDate = TimeUtil.dateFromDashString("2010-07-16");

            Portfolio result = bodyCompositionService.profileAlgorithm(new TestMsftAlgorithm(), portfolioModel.getObject(), startDate, endDate);
            portfolioModel.setObject(result);
         }
      };
      add(profileAlgorithm);


      final IModel<List<? extends BaseTicker>> similarDaysEntries = new LoadableDetachableModel<List<? extends BaseTicker>>() {
         protected List<? extends BaseTicker> load() {
//            return bodyCompositionService.getTickers();
            return Collections.emptyList();
         }
      };

      final EntriesPanel similarDays = new EntriesPanel("yahooStockTickers", "Similar Days", similarDaysEntries);
      similarDays.setOutputMarkupId(true);
      add(similarDays);

   }
}
