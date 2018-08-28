package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;

@SuppressWarnings({"rawtypes" })
public class CSheetAnnotationFactory implements AnnotationFactory<BeanDefinition, CSheet> {

    @Override
    public void build(BeanDefinition instance, CSheet a) {
        de.tsl2.nano.h5.CSheet sheet = new de.tsl2.nano.h5.CSheet(a.title(), a.cols(), a.rows());
        CCell c;
        for (int i = 0; i < a.cells().length; i++) {
            c = a.cells()[i];
            sheet.set(c.row(), c.col(), c.value());
        }
        sheet.save();
    }

}
