package de.tsl2.nano.modelkit.sort;

import java.util.Arrays;
import java.util.List;

import de.tsl2.nano.modelkit.impl.AttributeComparator;
import de.tsl2.nano.modelkit.impl.Comp;
import de.tsl2.nano.modelkit.impl.Def;
import de.tsl2.nano.modelkit.impl.Fact;
import de.tsl2.nano.modelkit.impl.Func;
import de.tsl2.nano.modelkit.impl.Group;
import de.tsl2.nano.modelkit.impl.ModelKit;

public class SortModel {
    enum Type {
        TYPE1, TYPE2, TYPE3
    };

    public static ModelKit create() {
        ModelKit kit = new ModelKit("sorter", "sort", "* * * ? * FRI *", null);
        kit.add(createDefinitions());
        kit.add(createFuncs());
        kit.add(createFacts());
        kit.add(createComparators());
        kit.add(createGroups());

        kit.register();
        return kit;
    }

    private static Def[] createDefinitions() {
        return new Def[] {
                new Def("group1Items", Arrays.asList(Type.class.getEnumConstants()))
        };
    }

    private static Func[] createFuncs() {
        return new Func[] {
                new Func<List<Object>, List<Object>>("sort", (owner, items) -> ((Group) owner).sort(items))
        };
    }

    private static Fact[] createFacts() {
        return new Fact[] {
                new Fact<Object>("isType1",
                        (config, item) -> config.getEnum("groupItems").get(0)
                                .equals(config.get("getType", Func.class))),
                new Fact<Object>("isType2",
                        (config, item) -> config.getEnum("groupItems").get(1).equals(config.get("getType", Func.class)))
        };
    }

    private static Comp[] createComparators() {
        return new Comp[] {
                new AttributeComparator("byType", "type", null)
        };
    }

    private static Group[] createGroups() {
        return new Group[] {
                new Group("group1", "isType1", "sort").setComparators("byType"),
                new Group("group2", "isType2", "sort").setComparators("byType")
        };
    }

}
