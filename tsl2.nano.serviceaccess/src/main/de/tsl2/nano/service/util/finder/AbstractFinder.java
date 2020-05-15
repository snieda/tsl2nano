package de.tsl2.nano.service.util.finder;

import static de.tsl2.nano.service.util.ServiceUtil.CLAUSE_AND;
import static de.tsl2.nano.service.util.ServiceUtil.CLAUSE_NOT;
import static de.tsl2.nano.service.util.ServiceUtil.CLAUSE_OR;
import static de.tsl2.nano.service.util.ServiceUtil.CLAUSE_WHERE;
import static de.tsl2.nano.service.util.ServiceUtil.SUBST_RESULTBEAN;
import static de.tsl2.nano.service.util.ServiceUtil.createStatement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.tsl2.nano.service.util.GenericServiceBean;
import de.tsl2.nano.service.util.ServiceUtil;

/**
 * Basic Finder
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public abstract class AbstractFinder<T> implements Serializable {
    /** queryServiceBean */
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    Class<T> resultType;
    List<Object> par;
    String connectionType = CLAUSE_AND;
    Class<Object>[] relationsToLoad;
    int maxResult = 0;
    int index = 0;
    private boolean isSubSelect;
	private boolean hasWhereClause;

    public AbstractFinder(Class<T> resultType, Class<Object>... relationsToLoad) {
        this.resultType = resultType;
        this.relationsToLoad = relationsToLoad;
    }

    public <FINDER extends AbstractFinder<T>> FINDER setMaxResult(int maxResult) {
        this.maxResult = maxResult;
        return (FINDER) this;
    }

    public <FINDER extends AbstractFinder<T>> FINDER setOrConnection() {
        connectionType = CLAUSE_OR;
        return (FINDER) this;
    }

    public <FINDER extends AbstractFinder<T>> FINDER setAndConnection() {
        connectionType = CLAUSE_AND;
        return (FINDER) this;
    }

    public <FINDER extends AbstractFinder<T>> FINDER setNotConnection() {
        connectionType = CLAUSE_NOT;
        return (FINDER) this;
    }

    /**
     * creates a new statement or subselect
     * 
     * @param index number of subselect
     * @param currentQuery current query, created by previous Filters
     * @return the given StringBuilder instance
     */
    StringBuffer prepareQuery(int index,
            StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        this.index = index;
        if (par != null) {
            parameter.addAll(par);
        }
        lazyRelations.addAll(Arrays.asList(this.relationsToLoad));
        if (currentQuery.length() == 0) {
            currentQuery.append(createStatement(resultType));
        } else if (currentQuery.indexOf(CLAUSE_WHERE) != -1){
            isSubSelect = true;
            
            currentQuery.append(createSubSelect(resultType));
        }
        return currentQuery;
    }

    /**
     * creates a new statement or subselect
     * 
     * @param currentQuery current query, created by previous Filters
     * @return the given StringBuilder instance
     */
    abstract StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations);

    /**
     * adds own ejbql-statement to the given one.
     * 
     * @param index number of subselect
     * @param currentQuery current query, created by previous Filters
     * @return the given StringBuilder instance
     */
    public StringBuffer addToQuery(int index,
            StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        prepareQuery(index, currentQuery, parameter, lazyRelations);
        createQuery(currentQuery, parameter, lazyRelations);
        return postQuery(currentQuery, parameter, lazyRelations);
    }

    /**
     * perhaps closes a subselect, started by {@link #prepareQuery(StringBuffer, Collection, Collection)}.
     * 
     * @param currentQuery current query, created by previous Filters
     * @return the given StringBuilder instance
     */
    StringBuffer postQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        if (isSubSelect()) {
            currentQuery.append(")");
        }
        return currentQuery;
    }

    /**
     * creates a new subselect for the given beantype and the standard {@link GenericServiceBean#SUBST_RESULTBEAN}.
     * 
     * @param beanType this type has to be the same as the result bean type of the main select
     * @return subselect statement opening a brace - you have to close that before you finish.
     */
    Object createSubSelect(Class<T> beanType) {
        return "\n " + connectionType + "  " + SUBST_RESULTBEAN + " in (" + createStatement(beanType, getSubSelectSubst());
    }

    /**
     * isNewStatement
     * 
     * @return true, if this finder was first on creating the select statement
     */
    public boolean isSubSelect() {
        return isSubSelect;
    }

    /**
     * creates the result table substitution of sub-select. e.g. 't1.' of first sub-select in '...in (select t1 from
     * MyResultTable t1...'.
     * 
     * @return select result substitution type name.
     */
    protected String getSubSelectSubst() {
        return index > 0 && isSubSelect ? SUBST_RESULTBEAN + index : SUBST_RESULTBEAN;
    }

    /**
     * getAndClause
     * @return and-clause or where-clause
     */
    protected String getAndClause() {
        return  provideWhereClause() ? CLAUSE_AND : CLAUSE_WHERE;
    }
    
    boolean provideWhereClause() {
    	boolean hasWhereClause = this.hasWhereClause;
    	this.hasWhereClause = ! this.hasWhereClause;
		return hasWhereClause;
	}

	/**
     * getFinderIndex
     * 
     * @return {@link #index}
     */
    protected int getFinderIndex() {
        return index;
    }

    /**
     * getConnector
     * @param currentQuery current built query
     * @return {@link #connectionType} or {@link ServiceUtil#CLAUSE_WHERE}.
     */
    protected String getConnector(StringBuffer currentQuery) {
        return isSubSelect || currentQuery.indexOf(CLAUSE_WHERE) == -1 ? CLAUSE_WHERE : connectionType;
    }
    
    /**
     * getResultType
     * 
     * @return
     */
    public Class<?> getResultType() {
        return resultType;
    }

    /**
     * getMaxResult
     * 
     * @return
     */
    public int getMaxResult() {
        return maxResult;
    }
}