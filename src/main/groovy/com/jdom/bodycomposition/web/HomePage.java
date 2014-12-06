package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.BaseTicker;
import com.jdom.bodycomposition.domain.YahooStockTicker;
import com.jdom.bodycomposition.domain.YahooStockTickerData;
import com.jdom.bodycomposition.service.YahooStockTickerDataDao;
import com.jdom.bodycomposition.service.YahooStockTickerService;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HomePage extends WebPage {

   private static final long serialVersionUID = 1L;

   @SpringBean
   private YahooStockTickerService bodyCompositionService;

   @SpringBean
   YahooStockTickerDataDao yahooStockTickerDataDao;

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

            for (Iterator<YahooStockTicker> iter = tickers.iterator(); iter.hasNext(); ) {
               YahooStockTicker ticker = iter.next();

               List<YahooStockTickerData> existing = yahooStockTickerDataDao.findByTicker(ticker);
               if (!existing.isEmpty()) {
                  YahooStockTickerData earliestEntry = existing.get(0);
                  YahooStockTickerData latestEntry = existing.get(existing.size() - 1);
//                  System.out.println(String.format("Skipping ticker: %s, already have from %s to %s", ticker.getTicker(),
//                        TimeUtil.dashString(earliestEntry.getDate()),
//                        TimeUtil.dashString(latestEntry.getDate())));
//
//                  iter.remove();
               }
            }

            System.gc();
            int totalTickersToUpdate = tickers.size();
            int numberUpdated = 0;
            for (YahooStockTicker ticker : tickers) {
               try {
                  bodyCompositionService.updateHistoryData(ticker);
                  System.gc();

                  Thread.sleep(500l);
               } catch (FileNotFoundException fnfe) {
                  System.out.println(String.format("Ticker: %s does not have history data", ticker.getTicker()));
               } catch (Exception e) {
                  e.printStackTrace();
               }

               numberUpdated++;
               System.out.println(String.format("Updated ticker: %s, %s/%s", ticker.getTicker(), numberUpdated, totalTickersToUpdate));
            }
         }
      };
      add(updateTickerData);

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
