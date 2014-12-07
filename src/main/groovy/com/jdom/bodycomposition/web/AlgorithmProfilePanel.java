package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario;
import com.jdom.bodycomposition.domain.algorithm.Portfolio;
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction;
import com.jdom.bodycomposition.domain.algorithm.TestMsftAlgorithm;
import com.jdom.bodycomposition.service.YahooStockTickerService;
import com.jdom.util.MathUtil;
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
   private YahooStockTickerService stockTickerService;

   public AlgorithmProfilePanel(final String id, final IModel<AlgorithmScenario> algorithmScenarioModel) {
      super(id, algorithmScenarioModel);
      setOutputMarkupId(true);

      Form<AlgorithmScenario> form = new Form<>("form", new CompoundPropertyModel<AlgorithmScenario>(algorithmScenarioModel));
      add(form);

      DateTextField startDate = new DateTextField("startDate");
      DateTextField endDate = new DateTextField("endDate");
      Component cash = new CurrencyTextField("startPortfolio.cash");
      Component commission = new CurrencyTextField("startPortfolio.commissionCost");

      form.add(startDate);
      form.add(endDate);
      form.add(cash);
      form.add(commission);

      final AjaxSubmitLink profileAlgorithm = new AjaxSubmitLink("profile", form) {
         @Override
         protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

            AlgorithmScenario scenario = algorithmScenarioModel.getObject();
            scenario.getTransactions().clear();
            AlgorithmScenario result = stockTickerService.profileAlgorithm(new TestMsftAlgorithm(), scenario);

            target.add(AlgorithmProfilePanel.this);
         }
      };
      form.add(profileAlgorithm);

      final IModel<List<? extends PortfolioTransaction>> transactions = new LoadableDetachableModel<List<? extends PortfolioTransaction>>() {
         protected List<? extends PortfolioTransaction> load() {
            return algorithmScenarioModel.getObject().getTransactions();
         }
      };

      final TransactionsPanel transactionsPanel = new TransactionsPanel("transactions", "Transactions", transactions);
      transactionsPanel.setOutputMarkupId(true);
      add(transactionsPanel);

      final WebMarkupContainer result = new WebMarkupContainer("resultPortfolio") {
         @Override
         protected void onConfigure() {
            super.onConfigure();

            setVisible(algorithmScenarioModel.getObject().getResultPortfolio() != null);
         }
      };
      result.add(new Label("resultCash", new Model<String>() {
         @Override
         public String getObject() {
            final Portfolio resultPortfolio = algorithmScenarioModel.getObject().getResultPortfolio();
            return resultPortfolio == null ? "" : MathUtil.formatMoney(resultPortfolio.getCash());
         }

         @Override
         public void setObject(final String object) {
         }
      }));

      add(result);
   }
}
