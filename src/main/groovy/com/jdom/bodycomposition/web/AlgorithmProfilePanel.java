package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction;
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue;
import com.jdom.bodycomposition.domain.market.MarketReplay;
import com.jdom.bodycomposition.service.MarketReplayService;
import com.jdom.bodycomposition.service.SecurityService;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.List;

/**
 * Created by djohnson on 12/6/14.
 */
public class AlgorithmProfilePanel extends Panel {

   @SpringBean
   private SecurityService securityService;

   @SpringBean
   private MarketReplayService marketReplayService;

   public AlgorithmProfilePanel(final String id, final IModel<MarketReplay> algorithmScenarioModel) {
      super(id, algorithmScenarioModel);
      setOutputMarkupId(true);

      Form<MarketReplay> form = new Form<>("form", new CompoundPropertyModel<MarketReplay>(algorithmScenarioModel));
      add(form);

      DateTextField startDate = new DateTextField("startDate");
      DateTextField endDate = new DateTextField("endDate");
      Component cash = new CurrencyTextField("initialPortfolio.cash");
      Component commission = new CurrencyTextField("commissionCost");

      form.add(startDate);
      form.add(endDate);
      form.add(cash);
      form.add(commission);

      final AjaxSubmitLink profileAlgorithm = new AjaxSubmitLink("profile", form) {
         @Override
         protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

            MarketReplay scenario = algorithmScenarioModel.getObject();
            scenario.getTransactions().clear();
            MarketReplay result = marketReplayService.profileAlgorithm(scenario);

            target.add(AlgorithmProfilePanel.this);
         }
      };
      form.add(profileAlgorithm);

      final IModel<List<? extends PortfolioTransaction>> transactions = new LoadableDetachableModel<List<? extends PortfolioTransaction>>() {
         protected List<? extends PortfolioTransaction> load() {
            return algorithmScenarioModel.getObject().getTransactions();
         }
      };

      WebMarkupContainer results = new WebMarkupContainer("results") {
         @Override
         protected void onConfigure() {
            super.onConfigure();

            setVisible(algorithmScenarioModel.getObject().getResultPortfolio() != null);
         }
      };
      results.setOutputMarkupId(true);
      add(results);

      results.add(new Label("duration", new Model<String>() {
         @Override
         public String getObject() {
            return algorithmScenarioModel.getObject().getDuration() + " ms";
         }
      }));

      final TransactionsPanel transactionsPanel = new TransactionsPanel("transactions", "Transactions", transactions);
      transactionsPanel.setOutputMarkupId(true);
      results.add(transactionsPanel);

      final IModel<PortfolioValue> resultPortfolioModel = new LoadableDetachableModel<PortfolioValue>() {
         protected PortfolioValue load() {
            return algorithmScenarioModel.getObject().getResultPortfolio();
         }
      };
      final IModel<PortfolioValue> initialPortfolioValueModel = new LoadableDetachableModel<PortfolioValue>() {
         protected PortfolioValue load() {
            final MarketReplay marketReplay = algorithmScenarioModel.getObject();
            return marketReplay == null ? null : securityService.portfolioValue(marketReplay.getInitialPortfolio(),
                    marketReplay.getStartDate());
         }
      };
      final PortfolioValuePanel resultPortfolioPanel = new PortfolioValuePanel("resultPortfolio", resultPortfolioModel, initialPortfolioValueModel);
      results.add(resultPortfolioPanel);

//      final IModel<List<? extends PortfolioValue>> portfolioCheckpoints = new LoadableDetachableModel<List<? extends PortfolioValue>>() {
//         protected List<? extends PortfolioValue> load() {
//            MarketReplay scenario = algorithmScenarioModel.getObject();
//            final PortfolioValue resultPortfolio = scenario.getResultPortfolio();
//
//            if (resultPortfolio == null) {
//               return Collections.emptyList();
//            } else {
//               return securityService.portfolioValueCheckpoints(scenario);
//            }
//         }
//      };
   }
}
