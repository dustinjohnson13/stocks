package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.BaseTicker;
import com.jdom.bodycomposition.service.YahooStockTickerService;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.List;

public class HomePage extends WebPage {

   private static final long serialVersionUID = 1L;

   @SpringBean
   private YahooStockTickerService bodyCompositionService;

   /**
    * Constructor that is invoked when page is invoked without a session.
    *
    * @param parameters Page parameters
    */
   public HomePage(final PageParameters parameters) {

      final IModel<List<? extends BaseTicker>> similarDaysEntries =  new LoadableDetachableModel<List<? extends BaseTicker>>() {
         protected List<? extends BaseTicker> load() {
            return bodyCompositionService.getTickers();
         }
      };

      final EntriesPanel similarDays = new EntriesPanel("yahooStockTickers", "Similar Days", similarDaysEntries);
      similarDays.setOutputMarkupId(true);
      add(similarDays);

   }
}
