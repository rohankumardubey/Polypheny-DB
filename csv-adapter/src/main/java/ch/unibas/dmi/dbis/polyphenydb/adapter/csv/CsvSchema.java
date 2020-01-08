/*
 * This file is based on code taken from the Apache Calcite project, which was released under the Apache License.
 * The changes are released under the MIT license.
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Databases and Information Systems Research Group, University of Basel, Switzerland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.unibas.dmi.dbis.polyphenydb.adapter.csv;


import ch.unibas.dmi.dbis.polyphenydb.catalog.entity.CatalogColumn;
import ch.unibas.dmi.dbis.polyphenydb.catalog.entity.combined.CatalogCombinedTable;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataType;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeFactory;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeImpl;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeSystem;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelProtoDataType;
import ch.unibas.dmi.dbis.polyphenydb.schema.Table;
import ch.unibas.dmi.dbis.polyphenydb.schema.impl.AbstractSchema;
import ch.unibas.dmi.dbis.polyphenydb.sql.type.SqlTypeFactoryImpl;
import ch.unibas.dmi.dbis.polyphenydb.sql.type.SqlTypeName;
import ch.unibas.dmi.dbis.polyphenydb.util.Source;
import ch.unibas.dmi.dbis.polyphenydb.util.Sources;
import ch.unibas.dmi.dbis.polyphenydb.util.Util;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Schema mapped onto a directory of CSV files. Each table in the schema is a CSV file in that directory.
 */
public class CsvSchema extends AbstractSchema {

    private final File directoryFile;
    private final CsvTable.Flavor flavor;
    private Map<String, CsvTable> tableMap = new HashMap<>();


    /**
     * Creates a CSV schema.
     *
     * @param directoryFile Directory that holds {@code .csv} files
     * @param flavor Whether to instantiate flavor tables that undergo query optimization
     */
    public CsvSchema( File directoryFile, CsvTable.Flavor flavor ) {
        super();
        this.directoryFile = directoryFile;
        this.flavor = flavor;
    }


    public Table createCsvTable( CatalogCombinedTable combinedTable ) {
        final RelDataTypeFactory typeFactory = new SqlTypeFactoryImpl( RelDataTypeSystem.DEFAULT );
        final RelDataTypeFactory.Builder fieldInfo = typeFactory.builder();
        List<CsvFieldType> fieldTypes = new LinkedList<>();
        for ( CatalogColumn catalogColumn : combinedTable.getColumns() ) {
            SqlTypeName dataTypeName = SqlTypeName.get( catalogColumn.type.name() ); // TODO Replace PolySqlType with native
            RelDataType sqlType = sqlType( typeFactory, dataTypeName, catalogColumn.length, catalogColumn.scale, null );
            fieldInfo.add( catalogColumn.name, sqlType ).nullable( catalogColumn.nullable );

            fieldTypes.add( CsvFieldType.getCsvFieldType( catalogColumn.type ) );
        }

        Source source = Sources.of( new File( directoryFile, combinedTable.getTable().name + ".csv" ) );
        CsvTable table = createTable( source, RelDataTypeImpl.proto( fieldInfo.build() ), fieldTypes );
        tableMap.put( combinedTable.getTable().name, table );
        return table;
    }


    @Override
    public Map<String, Table> getTableMap() {
        return new HashMap<>( tableMap );
    }

    /**
     * Creates different sub-type of table based on the "flavor" attribute.
     */
    private CsvTable createTable( Source source, RelProtoDataType protoRowType, List<CsvFieldType> fieldTypes ) {
        switch ( flavor ) {
            case TRANSLATABLE:
                return new CsvTranslatableTable( source, protoRowType, fieldTypes );
            case SCANNABLE:
                return new CsvScannableTable( source, protoRowType, fieldTypes );
            case FILTERABLE:
                return new CsvFilterableTable( source, protoRowType, fieldTypes );
            default:
                throw new AssertionError( "Unknown flavor " + this.flavor );
        }
    }


    private RelDataType sqlType( RelDataTypeFactory typeFactory, SqlTypeName dataTypeName, Integer length, Integer scale, String typeString ) {
        // Fall back to ANY if type is unknown
        final SqlTypeName sqlTypeName = Util.first( dataTypeName, SqlTypeName.ANY );
        switch ( sqlTypeName ) {
            case ARRAY:
                RelDataType component = null;
                if ( typeString != null && typeString.endsWith( " ARRAY" ) ) {
                    // E.g. hsqldb gives "INTEGER ARRAY", so we deduce the component type "INTEGER".
                    final String remaining = typeString.substring( 0, typeString.length() - " ARRAY".length() );
                    component = parseTypeString( typeFactory, remaining );
                }
                if ( component == null ) {
                    component = typeFactory.createTypeWithNullability( typeFactory.createSqlType( SqlTypeName.ANY ), true );
                }
                return typeFactory.createArrayType( component, -1 );
        }
        if ( scale != null && length != null && length >= 0 && scale >= 0 && sqlTypeName.allowsPrecScale( true, true ) ) {
            return typeFactory.createSqlType( sqlTypeName, length, scale );
        } else if ( length != null && length >= 0 && sqlTypeName.allowsPrecNoScale() ) {
            return typeFactory.createSqlType( sqlTypeName, length );
        } else {
            assert sqlTypeName.allowsNoPrecNoScale();
            return typeFactory.createSqlType( sqlTypeName );
        }
    }


    /**
     * Given "INTEGER", returns BasicSqlType(INTEGER).
     * Given "VARCHAR(10)", returns BasicSqlType(VARCHAR, 10).
     * Given "NUMERIC(10, 2)", returns BasicSqlType(NUMERIC, 10, 2).
     */
    private RelDataType parseTypeString( RelDataTypeFactory typeFactory, String typeString ) {
        int precision = -1;
        int scale = -1;
        int open = typeString.indexOf( "(" );
        if ( open >= 0 ) {
            int close = typeString.indexOf( ")", open );
            if ( close >= 0 ) {
                String rest = typeString.substring( open + 1, close );
                typeString = typeString.substring( 0, open );
                int comma = rest.indexOf( "," );
                if ( comma >= 0 ) {
                    precision = Integer.parseInt( rest.substring( 0, comma ) );
                    scale = Integer.parseInt( rest.substring( comma ) );
                } else {
                    precision = Integer.parseInt( rest );
                }
            }
        }
        try {
            final SqlTypeName typeName = SqlTypeName.valueOf( typeString );
            return typeName.allowsPrecScale( true, true )
                    ? typeFactory.createSqlType( typeName, precision, scale )
                    : typeName.allowsPrecScale( true, false )
                            ? typeFactory.createSqlType( typeName, precision )
                            : typeFactory.createSqlType( typeName );
        } catch ( IllegalArgumentException e ) {
            return typeFactory.createTypeWithNullability( typeFactory.createSqlType( SqlTypeName.ANY ), true );
        }
    }

//    /**
//     * Looks for a suffix on a string and returns either the string with the suffix removed or the original string.
//     */
//    private static String trim( String s, String suffix ) {
//        String trimmed = trimOrNull( s, suffix );
//        return trimmed != null ? trimmed : s;
//    }
//
//
//    /**
//     * Looks for a suffix on a string and returns either the string with the suffix removed or null.
//     */
//    private static String trimOrNull( String s, String suffix ) {
//        return s.endsWith( suffix )
//                ? s.substring( 0, s.length() - suffix.length() )
//                : null;
//    }
//
//
//    @Override
//    public Map<String, Table> getTableMap() {
//        if ( tableMap == null ) {
//            tableMap = createTableMap();
//        }
//        return tableMap;
//    }
//
//
//    private Map<String, Table> createTableMap() {
//        // Look for files in the directory ending in ".csv", ".csv.gz", ".json", ".json.gz".
//        final Source baseSource = Sources.of( directoryFile );
//        File[] files = directoryFile.listFiles( ( dir, name ) -> {
//            final String nameSansGz = trim( name, ".gz" );
//            return nameSansGz.endsWith( ".csv" ) || nameSansGz.endsWith( ".json" );
//        } );
//        if ( files == null ) {
//            System.out.println( "directory " + directoryFile + " not found" );
//            files = new File[0];
//        }
//        // Build a map from table name to table; each file becomes a table.
//        final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();
//        for ( File file : files ) {
//            Source source = Sources.of( file );
//            Source sourceSansGz = source.trim( ".gz" );
//            final Source sourceSansJson = sourceSansGz.trimOrNull( ".json" );
//            if ( sourceSansJson != null ) {
//                JsonTable table = new JsonTable( source );
//                builder.put( sourceSansJson.relative( baseSource ).path(), table );
//                continue;
//            }
//            final Source sourceSansCsv = sourceSansGz.trim( ".csv" );
//
//            final Table table = createTable( source );
//            builder.put( sourceSansCsv.relative( baseSource ).path(), table );
//        }
//        return builder.build();
//    }
//
//
//

}
