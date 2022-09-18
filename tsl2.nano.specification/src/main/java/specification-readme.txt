##############################################################################
# Tsl2Nano H5 Specification Properties (Thomas Schneider / 2022)
# 
# Syntax:
# <create-property>|<create-user><create-rule><bean-change>
#
# with:
#   create-property   : <property-name>=<property-value>
#   create-user       : createuser=<user-name>:<password>:<db-user-name>:<db-password>
#   create-rule       : <<rule-type-character><rule-simple-name>=<rule-expression>
#   bean-change       : <bean-name>[.<bean-attribute>.[<prop-change>|<attr-change>]] | [bean-change-ex]
#     with:  
#       bean-name     : <simple-bean-class-name>
#       bean-attribute: <simple-bean-attribute-name>
#       prop-change   : <<presentable>|<columnDefinition>|<constraint>|type|id|unique|temporalType|description|doValidation>*=<new-value>
#	    attr-change   :
#			  enabler=<rule>
#			| listener=<rule>:<list-of-observables>
#			| rulecover=<rule>:<attribute-property>
#       bean-change-ex:
#			  <valueexpression=<{attribute-name}[[any-seperator-characters]{attribute-name}...]>
#			| addattribute=<rule-name>
#			| addaction=<rule-name>
#			| attributefilter=<list-of-attribute-names-of-this-bean>
#			| icon=<relative-path-to-image-file>
#			| createcompositor=<basetype>,<baseattribute>,<attribte-of-this-bean-as-target><icon-attribte>
#			| createcontroller=<basetype>,<baseattribute>,<attribte-of-this-bean-as-target><icon-attribte><attribute-to-be-increased-by-clicks>
#			| createquery=<sql-query>
#			| createstatistics
#			| createsheet=<name>,<rows>,<cols>
#
#      with:
#        rule       : <rule-type-character><rule-simple-name>
#        constraint : constraint.<type|format|scale|precision|nullable|length|min|max|defaultValue|allowedValues>
#        presentable: presentable.<type|style|label|description|layout|layoutConstraints|visible|searchable|icon|nesting>
#        columndef  : columndefinition.<name|format|columnIndex|sortIndex|isSortUpDirection|width|<presentable>|minsearch|maxsearch|standardSummary>
#
# The character ':' can be replaced by one of ';:,\s'. The character '=' can be
# replaced by a tab character.
##############################################################################


