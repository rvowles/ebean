package com.avaje.ebean;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.util.CamelCaseHelper;

/**
 * Used to build object graphs based on a raw SQL statement (rather than
 * generated by Ebean).
 * <p>
 * If you don't want to build object graphs you can use {@link SqlQuery} instead
 * which returns {@link SqlRow} objects rather than entity beans.
 * </p>
 * <p>
 * <b>Unparsed RawSql:</b>
 * </p>
 * <p>
 * When RawSql is created via RawSqlBuilder.unparsed(sql) then Ebean can not
 * modify the SQL at all. It can't add any extra expressions into the SQL.
 * </p>
 * <p>
 * <b>Parsed RawSql:</b>
 * </p>
 * <p>
 * When RawSql is created via RawSqlBuilder.parse(sql) then Ebean will parse the
 * SQL and find places in the SQL where it can add extra where expressions, add
 * extra having expressions or replace the order by clause. If you want to
 * explicitly tell Ebean where these insertion points are you can place special
 * strings into your SQL (${where} or ${andWhere} and ${having} or
 * ${andHaving}).
 * </p>
 * <p>
 * If the SQL already includes a WHERE clause put in ${andWhere} in the location
 * you want Ebean to add any extra where expressions. If the SQL doesn't have a
 * WHERE clause put ${where} in instead. Similarly you can put in ${having} or
 * ${andHaving} where you want Ebean put add extra having expressions.
 * </p>
 * <p>
 * <b>Aggregates:</b>
 * </p>
 * <p>
 * Often RawSql will be used with Aggregate functions (sum, avg, max etc). The
 * follow example shows an example based on Total Order Amount -
 * sum(d.order_qty*d.unit_price).
 * </p>
 * <p>
 * We can use a OrderAggregate bean that has a &#064;Sql to indicate it is based
 * on RawSql and not based on a real DB Table or DB View. It has some properties
 * to hold the values for the aggregate functions (sum etc) and a &#064;OneToOne
 * to Order.
 * </p>
 * <p>
 * &nbsp;
 * </p>
 * <p>
 * <b>Example OrderAggregate</b>
 * </p>
 * 
 * <pre class="code">
 *  ...
 *  // &#064;Sql indicates to that this bean
 *  // is based on RawSql rather than a table
 * 
 * &#064;Entity
 * &#064;Sql    
 * public class OrderAggregate {
 * 
 *  &#064;OneToOne
 *  Order order;
 *      
 *  Double totalAmount;
 *  
 *  Double totalItems;
 *  
 *  // getters and setters
 *  ...
 * </pre>
 * <p>
 * <b>Example 1:</b>
 * </p>
 * 
 * <pre class="code">
 * String sql = &quot; select order_id, o.status, c.id, c.name, sum(d.order_qty*d.unit_price) as totalAmount&quot;
 *     + &quot; from o_order o&quot;
 *     + &quot; join o_customer c on c.id = o.kcustomer_id &quot;
 *     + &quot; join o_order_detail d on d.order_id = o.id &quot; + &quot; group by order_id, o.status &quot;;
 * 
 * RawSql rawSql = RawSqlBuilder.parse(sql)
 *     // map the sql result columns to bean properties
 *     .columnMapping(&quot;order_id&quot;, &quot;order.id&quot;).columnMapping(&quot;o.status&quot;, &quot;order.status&quot;)
 *     .columnMapping(&quot;c.id&quot;, &quot;order.customer.id&quot;)
 *     .columnMapping(&quot;c.name&quot;, &quot;order.customer.name&quot;)
 *     // we don't need to map this one due to the sql column alias
 *     // .columnMapping(&quot;sum(d.order_qty*d.unit_price)&quot;, &quot;totalAmount&quot;)
 *     .create();
 * 
 * Query&lt;OrderAggregate&gt; query = Ebean.find(OrderAggregate.class);
 * query.setRawSql(rawSql).where().gt(&quot;order.id&quot;, 0).having().gt(&quot;totalAmount&quot;, 20);
 * 
 * List&lt;OrderAggregate&gt; list = query.findList();
 * </pre>
 * 
 * <p>
 * <b>Example 2:</b>
 * </p>
 * 
 * <p>
 * The following example uses a FetchConfig().query() so that after the initial
 * RawSql query is executed Ebean executes a secondary query to fetch the
 * associated order status, orderDate along with the customer name.
 * </p>
 * 
 * <pre class="code">
 * String sql = &quot; select order_id, 'ignoreMe', sum(d.order_qty*d.unit_price) as totalAmount &quot;
 *     + &quot; from o_order_detail d&quot;
 *     + &quot; group by order_id &quot;;
 * 
 * RawSql rawSql = RawSqlBuilder.parse(sql).columnMapping(&quot;order_id&quot;, &quot;order.id&quot;)
 *     .columnMappingIgnore(&quot;'ignoreMe'&quot;).create();
 * 
 * Query&lt;OrderAggregate&gt; query = Ebean.find(OrderAggregate.class);
 * query.setRawSql(rawSql).fetch(&quot;order&quot;, &quot;status,orderDate&quot;, new FetchConfig().query())
 *     .fetch(&quot;order.customer&quot;, &quot;name&quot;).where()
 *     .gt(&quot;order.id&quot;, 0).having().gt(&quot;totalAmount&quot;, 20).order().desc(&quot;totalAmount&quot;).setMaxRows(10);
 * 
 * </pre>
 * 
 * <p>
 * Note that lazy loading also works with object graphs built with RawSql.
 * </p>
 * 
 */
public final class RawSql implements Serializable {

  private static final long serialVersionUID = 1L;

  private final ResultSet resultSet;
  
  private final Sql sql;

  private final ColumnMapping columnMapping;

  /**
   * Construct with a ResultSet and properties that the columns map to.
   * <p>
   * The properties listed in the propertyNames must be in the same order as the columns in the
   * resultSet.
   * <p>
   * When a query executes this RawSql object then it will close the resultSet.
   */
  public RawSql(ResultSet resultSet, String... propertyNames) {
    this.resultSet = resultSet;
    this.sql = null;
    this.columnMapping = new ColumnMapping(propertyNames);
  }
  
  protected RawSql(ResultSet resultSet, Sql sql, ColumnMapping columnMapping) {
    this.resultSet = resultSet;
    this.sql = sql;
    this.columnMapping = columnMapping;
  }

  /**
   * Return the Sql either unparsed or in parsed (broken up) form.
   */
  public Sql getSql() {
    return sql;
  }

  
  /**
   * Return the resultSet if this is a ResultSet based RawSql.
   */
  public ResultSet getResultSet() {
    return resultSet;
  }

  /**
   * Return the column mapping for the SQL columns to bean properties.
   */
  public ColumnMapping getColumnMapping() {
    return columnMapping;
  }

  /**
   * Return the hash for this query.
   */
  public int queryHash() {
    if (resultSet != null) {
      return 31 * columnMapping.queryHash();
    }
    return 31 * sql.queryHash() + columnMapping.queryHash();
  }

  /**
   * Represents the sql part of the query. For parsed RawSql the sql is broken
   * up so that Ebean can insert extra WHERE and HAVING expressions into the
   * SQL.
   */
  public static final class Sql implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean parsed;

    private final String unparsedSql;

    private final String preFrom;

    private final String preWhere;

    private final boolean andWhereExpr;

    private final String preHaving;

    private final boolean andHavingExpr;

    private final String orderBy;

    private final boolean distinct;

    private final int queryHashCode;

    /**
     * Construct for unparsed SQL.
     */
    protected Sql(String unparsedSql) {
      this.queryHashCode = unparsedSql.hashCode();
      this.parsed = false;
      this.unparsedSql = unparsedSql;
      this.preFrom = null;
      this.preHaving = null;
      this.preWhere = null;
      this.andHavingExpr = false;
      this.andWhereExpr = false;
      this.orderBy = null;
      this.distinct = false;
    }

    /**
     * Construct for parsed SQL.
     */
    protected Sql(int queryHashCode, String preFrom, String preWhere, boolean andWhereExpr,
        String preHaving, boolean andHavingExpr,
        String orderBy, boolean distinct) {

      this.queryHashCode = queryHashCode;
      this.parsed = true;
      this.unparsedSql = null;
      this.preFrom = preFrom;
      this.preHaving = preHaving;
      this.preWhere = preWhere;
      this.andHavingExpr = andHavingExpr;
      this.andWhereExpr = andWhereExpr;
      this.orderBy = orderBy;
      this.distinct = distinct;
    }

    /**
     * Return a hash for this query.
     */
    public int queryHash() {
      return queryHashCode;
    }

    public String toString() {
      if (!parsed) {
        return "unparsed[" + unparsedSql + "]";
      }
      return "select[" + preFrom + "] preWhere[" + preWhere + "] preHaving[" + preHaving
          + "] orderBy[" + orderBy + "]";
    }

    public boolean isDistinct() {
      return distinct;
    }

    /**
     * Return true if the SQL is left completely unmodified.
     * <p>
     * This means Ebean can't add WHERE or HAVING expressions into the query -
     * it will be left completely unmodified.
     * </p>
     */
    public boolean isParsed() {
      return parsed;
    }

    /**
     * Return the SQL when it is unparsed.
     */
    public String getUnparsedSql() {
      return unparsedSql;
    }

    /**
     * Return the SQL prior to FROM clause.
     */
    public String getPreFrom() {
      return preFrom;
    }

    /**
     * Return the SQL prior to WHERE clause.
     */
    public String getPreWhere() {
      return preWhere;
    }

    /**
     * Return true if there is already a WHERE clause and any extra where
     * expressions start with AND.
     */
    public boolean isAndWhereExpr() {
      return andWhereExpr;
    }

    /**
     * Return the SQL prior to HAVING clause.
     */
    public String getPreHaving() {
      return preHaving;
    }

    /**
     * Return true if there is already a HAVING clause and any extra having
     * expressions start with AND.
     */
    public boolean isAndHavingExpr() {
      return andHavingExpr;
    }

    /**
     * Return the SQL ORDER BY clause.
     */
    public String getOrderBy() {
      return orderBy;
    }

  }

  /**
   * Defines the column mapping for raw sql DB columns to bean properties.
   */
  public static final class ColumnMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<String, Column> dbColumnMap;

    private final Map<String, String> propertyMap;
    
    private final Map<String, Column> propertyColumnMap;

    private final boolean parsed;

    private final boolean immutable;

    private final int queryHashCode;

    /**
     * Construct from parsed sql where the columns have been identified.
     */
    protected ColumnMapping(List<Column> columns) {
      this.queryHashCode = 0;
      this.immutable = false;
      this.parsed = true;
      this.propertyMap = null;
      this.propertyColumnMap = null;
      this.dbColumnMap = new LinkedHashMap<String, Column>();
      for (int i = 0; i < columns.size(); i++) {
        Column c = columns.get(i);
        dbColumnMap.put(c.getDbColumn(), c);
      }
    }

    /**
     * Construct for unparsed sql.
     */
    protected ColumnMapping() {
      this.queryHashCode = 0;
      this.immutable = false;
      this.parsed = false;
      this.propertyMap = null;
      this.propertyColumnMap = null;
      this.dbColumnMap = new LinkedHashMap<String, Column>();
    }
    
    /**
     * Construct for ResultSet use.
     */
    protected ColumnMapping(String... propertyNames) {
      this.immutable = false;
      this.parsed = false;
      this.propertyMap = null;
      //this.propertyColumnMap = null;
      this.dbColumnMap = new LinkedHashMap<String, Column>();
      
      int hc = 31;
      int pos = 0;
      for (String prop : propertyNames) {
        hc = 31 * hc + prop.hashCode();
        dbColumnMap.put(prop, new Column(pos++, prop, null, prop));
      }
      propertyColumnMap = dbColumnMap;
      this.queryHashCode = hc;
    }

    /**
     * Construct an immutable ColumnMapping based on collected information.
     */
    protected ColumnMapping(boolean parsed, LinkedHashMap<String, Column> dbColumnMap) {
      this.immutable = true;
      this.parsed = parsed;
      this.dbColumnMap = dbColumnMap;

      int hc = ColumnMapping.class.getName().hashCode();

      HashMap<String, Column> pcMap = new HashMap<String, Column>();
      HashMap<String, String> pMap = new HashMap<String, String>();

      for (Column c : dbColumnMap.values()) {
        pMap.put(c.getPropertyName(), c.getDbColumn());
        pcMap.put(c.getPropertyName(), c);

        hc = 31 * hc + c.getPropertyName() == null ? 0 : c.getPropertyName().hashCode();
        hc = 31 * hc + c.getDbColumn() == null ? 0 : c.getDbColumn().hashCode();
      }
      this.propertyMap = Collections.unmodifiableMap(pMap);
      this.propertyColumnMap = Collections.unmodifiableMap(pcMap);
      this.queryHashCode = hc;
    }

    /**
     * Creates an immutable copy of this ColumnMapping.
     * 
     * @throws IllegalStateException
     *           when a propertyName has not been defined for a column.
     */
    protected ColumnMapping createImmutableCopy() {

      for (Column c : dbColumnMap.values()) {
        c.checkMapping();
      }

      return new ColumnMapping(parsed, dbColumnMap);
    }

    protected void columnMapping(String dbColumn, String propertyName) {

      if (immutable) {
        throw new IllegalStateException("Should never happen");
      }
      if (!parsed) {
        int pos = dbColumnMap.size();
        dbColumnMap.put(dbColumn, new Column(pos, dbColumn, null, propertyName));
      } else {
        Column column = dbColumnMap.get(dbColumn);
        if (column == null) {
          String msg = "DB Column [" + dbColumn + "] not found in mapping. Expecting one of ["
              + dbColumnMap.keySet() + "]";
          throw new IllegalArgumentException(msg);
        }
        column.setPropertyName(propertyName);
      }
    }

    /**
     * Return the query hash for this column mapping.
     */
    public int queryHash() {
      if (queryHashCode == 0) {
        throw new RuntimeException("Bug: queryHashCode == 0");
      }
      return queryHashCode;
    }

    /**
     * Returns true if the Columns where supplied by parsing the sql select
     * clause.
     * <p>
     * In the case where the columns where parsed then we can do extra checks on
     * the column mapping such as, is the column a valid one in the sql and
     * whether all the columns in the sql have been mapped.
     * </p>
     */
    public boolean isParsed() {
      return parsed;
    }

    /**
     * Return the number of columns in this column mapping.
     */
    public int size() {
      return dbColumnMap.size();
    }

    /**
     * Return the column mapping.
     */
    protected Map<String, Column> mapping() {
      return dbColumnMap;
    }

    /**
     * Return the mapping by DB column.
     */
    public Map<String, String> getMapping() {
      return propertyMap;
    }

    /**
     * Return the index position by bean property name.
     */
    public int getIndexPosition(String property) {
      Column c = propertyColumnMap.get(property);
      return c == null ? -1 : c.getIndexPos();
    }

    /**
     * Return an iterator of the Columns.
     */
    public Iterator<Column> getColumns() {
      return dbColumnMap.values().iterator();
    }

    /**
     * A Column of the RawSql that is mapped to a bean property (or ignored).
     */
    public static class Column implements Serializable {

      private static final long serialVersionUID = 1L;
      private final int indexPos;
      private final String dbColumn;

      private final String dbAlias;

      private String propertyName;

      /**
       * Construct a Column.
       */
      public Column(int indexPos, String dbColumn, String dbAlias) {
        this(indexPos, dbColumn, dbAlias, derivePropertyName(dbAlias, dbColumn));
      }

      private Column(int indexPos, String dbColumn, String dbAlias, String propertyName) {
        this.indexPos = indexPos;
        this.dbColumn = dbColumn;
        this.dbAlias = dbAlias;
        if (propertyName == null && dbAlias != null) {
          this.propertyName = dbAlias;
        } else {
          this.propertyName = propertyName;
        }
      }

      private static String derivePropertyName(String dbAlias, String dbColumn) {
        if (dbAlias != null) {
          return dbAlias;
        }
        int dotPos = dbColumn.indexOf('.');
        if (dotPos > -1) {
          dbColumn = dbColumn.substring(dotPos + 1);
        }
        return CamelCaseHelper.toCamelFromUnderscore(dbColumn);
      }

      private void checkMapping() {
        if (propertyName == null) {
          String msg = "No propertyName defined (Column mapping) for dbColumn [" + dbColumn + "]";
          throw new IllegalStateException(msg);
        }
      }

      public String toString() {
        return dbColumn + "->" + propertyName;
      }

      /**
       * Return the index position of this column.
       */
      public int getIndexPos() {
        return indexPos;
      }

      /**
       * Return the DB column name including table alias (if it has one).
       */
      public String getDbColumn() {
        return dbColumn;
      }

      /**
       * Return the DB column alias (if it has one).
       */
      public String getDbAlias() {
        return dbAlias;
      }

      /**
       * Return the bean property this column is mapped to.
       */
      public String getPropertyName() {
        return propertyName;
      }

      private void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
      }

    }
  }
}
