package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.BaseTicker;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by djohnson on 11/16/14.
 */
public class EntriesPanel extends Panel {

   public EntriesPanel(String id, String title, IModel<List<? extends BaseTicker>> model) {
      super(id, model);

      add(new Label("title", title));

      final ListView<BaseTicker> entries = new ListView<BaseTicker>("entries", model) {

         @Override
         protected void populateItem(ListItem<BaseTicker> item) {
            BaseTicker entry = item.getModelObject();
            item.add(new Label("ticker", entry.getTicker()));
            item.add(new Label("name", entry.getName()));
            item.add(new Label("category", entry.getCategory()));
            item.add(new Label("country", entry.getCountry()));
         }
      };
      add(entries);
   }
}
