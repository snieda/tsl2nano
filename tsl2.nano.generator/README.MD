
    #foreach( $att in $class.Attributes)    
                            <span class="th">${att.Name}</span>
    #end

## using the $util as implementation of GeneratorUtiltiy

### util implmentation classes

there are three implmentations of Utitly classes. Each Generator implementation/algorithm may use an own utility implementation.

* *ClassGenerator*  : GeneratorBeanUtility
* *PackageGenerator*: GeneratorBeanUtility
* *XmlGenerator*    : GeneratorXmlUtility
* PropertiesAsConstantsGenerator*: GeneratorBeanUtility

### GeneratorUtility

Base implementation. Provides string transformations, and transfomrations to classes, instances or beans (providing all properties of a class per reflection).

#### method reference with examples

### GeneratorBeanUtility

### GeneratorXmlUtility



