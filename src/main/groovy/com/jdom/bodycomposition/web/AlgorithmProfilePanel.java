package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.market.MarketReplay;
import com.jdom.bodycomposition.service.MarketReplayService;
import com.jdom.bodycomposition.service.SecurityService;
import com.jdom.util.MathUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

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

      Component cash = new CurrencyTextField("initialPortfolio.cash");
      Component commission = new CurrencyTextField("commissionCost");

      form.add(cash);
      form.add(commission);

      final AjaxSubmitLink profileAlgorithm = new AjaxSubmitLink("profile", form) {
         @Override
         protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

            MarketReplay scenario = algorithmScenarioModel.getObject();
            MarketReplay result = marketReplayService.profileAlgorithm(scenario);

            target.add(AlgorithmProfilePanel.this);
         }
      };
      form.add(profileAlgorithm);

      WebMarkupContainer results = new WebMarkupContainer("results") {
         @Override
         protected void onConfigure() {
            super.onConfigure();

            setVisible(algorithmScenarioModel.getObject().getDuration() != 0);
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

       results.add(new Label("averagePercentChange", new Model<String>() {
           @Override
           public String getObject() {
               return MathUtil.formatPercentage(algorithmScenarioModel.getObject().getAveragePercentChange());
           }
       }));

       results.add(new Label("maximumPercentChange", new Model<String>() {
           @Override
           public String getObject() {
               return MathUtil.formatPercentage(algorithmScenarioModel.getObject().getMaxPercentChange());
           }
       }));

       results.add(new Label("minimumPercentChange", new Model<String>() {
           @Override
           public String getObject() {
               return MathUtil.formatPercentage(algorithmScenarioModel.getObject().getMinPercentChange());
           }
       }));
   }
}
