package io.simplifier.plugin.contentrepo.pluginBaseRelated.db.migration

import java.sql.ResultSet

/**
  * Created by P005 on 17.05.2016.
  * Extended by K002 on 21.03.2018.
  */
object ResultSetHelper {

  type ResultMap = Map[String, Any]
  type ResultList = Vector[ResultMap]
  type ResultListCaseClass = Vector[Vector[Result]]

  case class Result(columnPosition: Int, columnName: String, columnType: Int, columnValue: Any)

  def apply(rs: ResultSet): ResultListCaseClass = convert(rs, asMap = false).right.get
  def apply(rs: ResultSet, asMap: Boolean): ResultList = convert(rs, asMap).left.get



  private[this] def convert(rs: ResultSet, asMap: Boolean): Either[ResultList, ResultListCaseClass] = {

    val rsMd = rs.getMetaData
    val numColumns = rsMd.getColumnCount
    val buildMap: () => IndexedSeq[Result] = () => for {
      i <- 1 to numColumns
      columnName: String = rsMd.getColumnLabel(i)
      columnType: Int = rsMd.getColumnType(i)
      columnValue: Option[Any] = Option(columnType match {
        //Characters
        case java.sql.Types.CHAR                    => rs.getString(i)      //JDBC Data Type Int:     1
        case java.sql.Types.VARCHAR                 => rs.getString(i)      //JDBC Data Type Int:    12
        case java.sql.Types.LONGVARCHAR             => rs.getString(i)      //JDBC Data Type Int: -   1
        case java.sql.Types.CLOB                    => rs.getClob(i)        //JDBC Data Type Int:  2005
        case java.sql.Types.NCHAR                   => rs.getNString(i)     //JDBC Data Type Int: -  15
        case java.sql.Types.NVARCHAR                => rs.getNString(i)     //JDBC Data Type Int: -   9
        case java.sql.Types.LONGNVARCHAR            => rs.getNString(i)     //JDBC Data Type Int: -  16
        case java.sql.Types.NCLOB                   => rs.getNClob(i)       //JDBC Data Type Int:  2011

        //Numbers
        case java.sql.Types.BOOLEAN                 => rs.getBoolean(i)     //JDBC Data Type Int:    16
        case java.sql.Types.BIT                     => rs.getBoolean(i)     //JDBC Data Type Int: -   7
        case java.sql.Types.TINYINT                 => rs.getByte(i).toInt  //JDBC Data Type Int  -   6
        case java.sql.Types.SMALLINT                => rs.getShort(i).toInt //JDBC Data Type Int      5
        case java.sql.Types.INTEGER                 => rs.getInt(i)         //JDBC Data Type Int:     4
        case java.sql.Types.BIGINT                  => rs.getLong(i)        //JDBC Data Type Int: -   5
        case java.sql.Types.REAL                    => rs.getFloat(i)       //JDBC Data Type Int:     7
        case java.sql.Types.DOUBLE                  => rs.getDouble(i)      //JDBC Data Type Int:     8
        case java.sql.Types.FLOAT                   => rs.getDouble(i)      //JDBC Data Type Int:     6
        case java.sql.Types.NUMERIC                 => rs.getBigDecimal(i)  //JDBC Data Type Int:     2
        case java.sql.Types.DECIMAL                 => rs.getBigDecimal(i)  //JDBC Data Type Int:     3

        //Date and Time
        case java.sql.Types.DATE                    => rs.getDate(i)        //JDBC Data Type Int:    91
        case java.sql.Types.TIME                    => rs.getTime(i)        //JDBC Data Type Int:    92
        case java.sql.Types.TIMESTAMP               => rs.getTimestamp(i)   //JDBC Data Type Int:    93
        case java.sql.Types.TIME_WITH_TIMEZONE      => rs.getTime(i)        //JDBC Data Type Int:  2013
        case java.sql.Types.TIMESTAMP_WITH_TIMEZONE => rs.getTimestamp(i)   //JDBC Data Type Int:  2014

        //Binaries
        case java.sql.Types.BLOB                    => rs.getBlob(i)        //JDBC Data Type Int:  2004
        case java.sql.Types.BINARY                  => rs.getBytes(i)       //JDBC Data Type Int: -   2
        case java.sql.Types.VARBINARY               => rs.getBytes(i)       //JDBC Data Type Int: -   3
        case java.sql.Types.LONGVARBINARY           => rs.getBytes(i)       //JDBC Data Type Int: -   4

        //Arrays
        case java.sql.Types.ARRAY                   => rs.getArray(i)       //JDBC Data Type Int:  2003

        //Structures
        case java.sql.Types.STRUCT                  => rs.getObject(i)      //JDBC Data Type Int:  2002
        case java.sql.Types.SQLXML                  => rs.getSQLXML(i)      //JDBC Data Type Int:  2009

        //References
        case java.sql.Types.REF                     => rs.getRef(i)         //JDBC Data Type Int:  2006
        case java.sql.Types.DATALINK                => rs.getURL(i)         //JDBC Data Type Int:    70
        case java.sql.Types.ROWID                   => rs.getRowId(i)       //JDBC Data Type Int: -   8
        case java.sql.Types.REF_CURSOR              => rs.getRef(i)         //JDBC Data Type Int:  2012

        //Objects
        case java.sql.Types.JAVA_OBJECT             => rs.getObject(i)      //JDBC Data Type Int:  2000
        case java.sql.Types.NULL                    => null                 //JDBC Data Type Int:     0

        //Other and Custom
        case java.sql.Types.OTHER                   => rs.getObject(i)      //JDBC Data Type Int:  1111
        case java.sql.Types.DISTINCT                => rs.getObject(i)      //JDBC Data Type Int:  2001

        //Non JDBC Types
        case other@_ => println(s"Non JDBC Data Type received! The value is: {$other and the data type is: {${other.getClass.getName}} !")
      })
    } yield Result(i, columnName, columnType, columnValue.orNull)

    if (asMap) Left(Iterator.continually(rs.next()).takeWhile(identity).map(_ => buildMap().map(rs => rs.columnName -> rs.columnValue).toMap).toVector) else
      Right(Iterator.continually(rs.next()).takeWhile(identity).map(_ => buildMap().toVector).toVector)
  }


}
