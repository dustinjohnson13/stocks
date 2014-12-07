package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction;
import com.jdom.util.MathUtil;
import com.jdom.util.TimeUtil;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by djohnson on 11/16/14.
 */
public class TransactionsPanel extends Panel {

   public TransactionsPanel(String id, String title, IModel<List<? extends PortfolioTransaction>> model) {
      super(id, model);

      add(new Label("title", title));

      final ListView<PortfolioTransaction> entries = new ListView<PortfolioTransaction>("transactions", model) {

         @Override
         protected void populateItem(ListItem<PortfolioTransaction> item) {
            PortfolioTransaction entry = item.getModelObject();
            item.add(new Label("symbol", entry.getSymbol()));
            item.add(new Label("type", entry.getAction()));
            item.add(new Label("date", TimeUtil.dashString(entry.getDate())));
            item.add(new Label("shares", entry.getShares()));
            item.add(new Label("price", MathUtil.formatMoney(entry.getPrice())));
            item.add(new Label("cashValue", MathUtil.formatMoney(entry.getCashValue())));
            item.add(new Label("commission", MathUtil.formatMoney(entry.getCommission())));
         }
      };
      add(entries);
   }
}
