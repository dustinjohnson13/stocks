package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario;
import com.jdom.bodycomposition.domain.algorithm.Portfolio;
import com.jdom.bodycomposition.domain.algorithm.TestMsftAlgorithm;
import com.jdom.bodycomposition.service.YahooStockTickerService;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Created by djohnson on 12/6/14.
 */
public class AlgorithmProfilePanel extends Panel {

   @SpringBean
   private YahooStockTickerService stockTickerService;

   public AlgorithmProfilePanel(final String id, final IModel<AlgorithmScenario> algorithmScenarioModel) {
      super(id, algorithmScenarioModel);

      Form<AlgorithmScenario> form = new Form<>("form", new CompoundPropertyModel<AlgorithmScenario>(algorithmScenarioModel));
      add(form);

      DateTextField startDate = new DateTextField("startDate");
      DateTextField endDate = new DateTextField("endDate");
      Component cash = new CurrencyTextField("portfolio.cash");
      Component commission = new CurrencyTextField("portfolio.commissionCost");

      form.add(startDate);
      form.add(endDate);
      form.add(cash);
      form.add(commission);

      final AjaxSubmitLink profileAlgorithm = new AjaxSubmitLink("profile", form) {
         @Override
         protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

            AlgorithmScenario scenario = algorithmScenarioModel.getObject();
            Portfolio result = stockTickerService.profileAlgorithm(new TestMsftAlgorithm(), scenario.getPortfolio(),
                  scenario.getStartDate(), scenario.getEndDate());
            scenario.setPortfolio(result);

            target.add(form);
         }
      };
      form.add(profileAlgorithm);
   }
}
