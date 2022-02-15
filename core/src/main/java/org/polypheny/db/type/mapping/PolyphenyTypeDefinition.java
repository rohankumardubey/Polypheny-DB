/*
 * Copyright 2019-2022 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.type.mapping;

import java.math.BigDecimal;
import java.util.List;
import org.bson.BsonValue;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.mapping.TypeSpaceMapping.UnsupportedTypeException;
import org.polypheny.db.util.DateString;
import org.polypheny.db.util.TimeString;
import org.polypheny.db.util.TimestampString;

public enum PolyphenyTypeDefinition implements TypeDefinition<PolyphenyTypeDefinition> {
    INSTANCE;


    @Override
    public TypeSpaceMapping<PolyphenyTypeDefinition, PolyphenyTypeDefinition> getToPolyphenyMapping() {
        throw new RuntimeException( "PolyphenyTypeDefinition should not map onto itself." );
    }


    @Override
    public TypeSpaceMapping<PolyphenyTypeDefinition, PolyphenyTypeDefinition> getFromPolyphenyMapping() {
        throw new RuntimeException( "PolyphenyTypeDefinition should not map onto itself." );
    }


    @Override
    public Class<?> getMappingClass( PolyType type, boolean nullable ) {
        switch ( type ) {
            case BOOLEAN:
                return nullable ? Boolean.class : boolean.class;
            case TINYINT:
                return nullable ? Byte.class : byte.class;
            case SMALLINT:
                return nullable ? Short.class : short.class;
            case INTEGER:
                return nullable ? Integer.class : int.class;
            case BIGINT:
                return nullable ? Long.class : long.class;
            case DECIMAL:
                return BigDecimal.class;
            case FLOAT:
            case REAL:
                return nullable ? Float.class : float.class;
            case DOUBLE:
                return nullable ? Double.class : double.class;
            case DATE:
                return DateString.class;
            case TIME:
            case TIME_WITH_LOCAL_TIME_ZONE:
                return TimeString.class;
            case TIMESTAMP:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return TimestampString.class;
            case INTERVAL_YEAR:
            case INTERVAL_YEAR_MONTH:
            case INTERVAL_MONTH:
            case INTERVAL_DAY:
            case INTERVAL_DAY_HOUR:
            case INTERVAL_DAY_MINUTE:
            case INTERVAL_DAY_SECOND:
            case INTERVAL_HOUR:
            case INTERVAL_HOUR_MINUTE:
            case INTERVAL_HOUR_SECOND:
            case INTERVAL_MINUTE:
            case INTERVAL_MINUTE_SECOND:
            case INTERVAL_SECOND:
                return nullable ? Long.class : long.class;
            case CHAR:
            case VARCHAR:
                return String.class;
            case BINARY:
            case VARBINARY:
            case FILE:
            case IMAGE:
            case VIDEO:
            case SOUND:
                return Object.class;
            case NULL:
            case ANY:
            case SYMBOL:
            case DISTINCT:
            case STRUCTURED:
            case OTHER:
            case CURSOR:
            case COLUMN_LIST:
            case DYNAMIC_STAR:
            case GEOMETRY:
                return Object.class;
            case MULTISET:
            case ARRAY:
            case MAP:
            case ROW:
                return List.class;
            case JSON:
                return BsonValue.class;
        }

        throw new UnsupportedTypeException( "Mongo", type );
    }


    @Override
    public boolean needsPolyphenyMapping() {
        return TypeDefinition.super.needsPolyphenyMapping();
    }

}
