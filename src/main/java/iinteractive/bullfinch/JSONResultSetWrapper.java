package iinteractive.bullfinch;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONResultSetWrapper implements Iterator<String> {

	static Logger logger = LoggerFactory.getLogger(JSONResultSetWrapper.class);
	private ResultSet resultSet;
	private ResultSetMetaData metadata;
	private String[] columnNames;
	private int[] columnTypes;
	private int columnCount;


	public JSONResultSetWrapper(ResultSet rs) throws SQLException {

		this.resultSet = rs;
		this.metadata = rs.getMetaData();
		this.columnCount = this.metadata.getColumnCount();

		// Setup an array of names and types, using the rowCount size
		this.columnNames = new String[this.columnCount];
		this.columnTypes = new int[this.columnCount];
		for (int i = 0; i < this.columnCount; i++) {
			columnNames[i] = metadata.getColumnName(i + 1);
			columnTypes[i] = metadata.getColumnType(i + 1);
		}
	}

	/**
	 * Returns true if this iterator has more values.  Also advances the
	 * ResultSet, so always use this!  This is horrible, but it works. - CGW
	 */
	@Override
	public boolean hasNext() {

		boolean hasMore = false;
		try {
			// If this isn't the last one, then we are ok. When isLast is true,
			// this will return false.
			hasMore = this.resultSet.next();
		} catch(SQLException e) {
			// We'll complain, but otherwise we'll return a false, can't do
			// much about it here.
			e.printStackTrace();
		}

		return hasMore;
	}

	/**
	 * Returns the next item in the iterator.  Assumes you have called hasNext()
	 * already, since that calls next() on the ResultSet. :)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String next() {

		JSONObject obj = new JSONObject();

		try {


			obj.put("row_num", new Integer(resultSet.getRow()));

	        JSONObject data = new JSONObject();
	        obj.put("row_data", data);

	        for (int i = 1; i <= this.columnCount; i++) {

	            String col_name = this.columnNames[i - 1];

	            switch (this.columnTypes[i - 1]) {
	                case Types.CHAR        :
	                case Types.VARCHAR     :
	                case Types.LONGVARCHAR :
	                    data.put(col_name, resultSet.getString(i));
	                    break;
	                case Types.NUMERIC :
	                case Types.DECIMAL :
	                    data.put(col_name, resultSet.getBigDecimal(i));
	                    break;
	                case Types.BIT     :
	                case Types.BOOLEAN :
	                    data.put(col_name, resultSet.getBoolean(i));
	                    break;
	                case Types.TINYINT  :
	                case Types.SMALLINT :
	                case Types.INTEGER  :
	                    data.put(col_name, new Integer(resultSet.getInt(i)));
	                    break;
	                case Types.BIGINT :
	                    data.put(col_name, new Long(resultSet.getLong(i)));
	                    break;
	                case Types.REAL	:
	                case Types.FLOAT:
	                	data.put(col_name, new Float(resultSet.getFloat(i)));
	                	break;
	                case Types.DOUBLE :
	                    data.put(col_name, new Double(resultSet.getDouble(i)));
	                    break;
	                case Types.DATE:
	                    Date d = resultSet.getDate(i);
	                    data.put(col_name, d != null ? d.toString() : null);
	                    break;
	                case Types.TIME :
	                    Time t = resultSet.getTime( i );
	                    data.put(col_name, t != null ? t.toString() : null);
	                    break;
	                case Types.TIMESTAMP :
	                    data.put(col_name, resultSet.getString(i));
	                    break;
	                default :
	                    throw new SQLException("I don't recognize this type for column (" + col_name + ")");
	            }
	        }
		} catch(Exception e) {
			logger.error("Failed to JSON-ify resultset", e);
		}

        return obj.toString();
	}

	@Override
	public void remove() {
		// AINT DOING SHOT, ROFLCOPTER
	}

}
