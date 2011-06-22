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

	public JSONResultSetWrapper(ResultSet rs) {

		this.resultSet = rs;
	}

	/**
	 * Returns true if this iterator has more values.  Also advances the
	 * ResultSet, so always use this!
	 */
	@Override
	public boolean hasNext() {

		boolean hasMore = false;
		try {
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
	        ResultSetMetaData metadata = resultSet.getMetaData();

	        obj.put("row_num", new Integer(resultSet.getRow()));

	        JSONObject data = new JSONObject();
	        obj.put("row_data", data);

	        int num_columns = metadata.getColumnCount();
	        for (int i = 1; i <= num_columns; i++) {

	            String col_name = metadata.getColumnName(i);

	            switch (metadata.getColumnType(i)) {
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
	                case Types.REAL :
	                    data.put(col_name, new Float(resultSet.getFloat(i)));
	                    break;
	                case Types.FLOAT  :
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
