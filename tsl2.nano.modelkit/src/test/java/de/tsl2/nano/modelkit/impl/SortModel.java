package de.tsl2.nano.modelkit.impl;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SortModel {
    enum Type {
        TYPE1, TYPE2
    };

    public static ModelKit create() {
        ModelKit kit = new ModelKit("sorter", "sort", "* * * ? * MON-FRI *", null);
        kit.add(createDefinitions());
        kit.add(createFuncs());
        kit.add(createFacts());
        kit.add(createComparators());
        kit.add(createGroups());

        kit.register();
        return kit;
    }

    public static ModelKit create2() {
        ModelKit kit = new ModelKit("sorter2", "sort", "* * * ? * SAT-SUN *", null);
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
                new Def("groupItems", Arrays.asList(Type.class.getEnumConstants()))
        };
    }

    private static Func[] createFuncs() {
        return new Func[] {
                new Func<List<TestItem>, List<TestItem>>("sort", (owner, items) -> ((Group) owner).sort(items)),
                new Func<TestItem, String>("getType", (owner, item) -> item.type)
        };
    }

    private static Fact[] createFacts() {
        return new Fact[] {
                new Fact<Object>("isType1",
                        (config, item) -> config.getEnum("groupItems").get(0).toString()
                                .equals(((Func) config.get("getType", Func.class)).eval(item))),
                new Fact<Object>("isType2",
                        (config, item) -> config.getEnum("groupItems").get(1).toString()
                                .equals(((Func) config.get("getType", Func.class)).eval(item)))
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

    public static Object getTestItemFieldnames() {
        return new String[] { "name", "type", "value" };
    }
}
