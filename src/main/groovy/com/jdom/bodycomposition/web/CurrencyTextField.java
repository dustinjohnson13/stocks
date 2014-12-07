package com.jdom.bodycomposition.web;

import com.jdom.util.MathUtil;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import java.util.Locale;

/**
 * Created by djohnson on 12/6/14.
 */
public class CurrencyTextField extends TextField<Long> {

   public CurrencyTextField(String id) {
      this(id, null);
   }

   public CurrencyTextField(String id, IModel<Long> model) {
      super(id, model, Long.class);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <C> IConverter<C> getConverter(final Class<C> type) {
      if (Long.class.isAssignableFrom(type)) {
         return (IConverter<C>) new CurrencyConverter();
      } else {
         return super.getConverter(type);
      }
   }

   public static class CurrencyConverter implements IConverter<Long> {

      @Override
      public Long convertToObject(String value, Locale locale) throws ConversionException {
         return MathUtil.toMoney(value);
      }

      @Override
      public String convertToString(Long value, Locale locale) {
         return MathUtil.formatMoney(value);
      }

   }
}
