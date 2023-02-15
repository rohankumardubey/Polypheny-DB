/*
 * Copyright 2019-2023 The Polypheny Project
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

package org.polypheny.db.catalog;

import io.activej.serializer.BinarySerializer;

public interface SerializableCatalog {

    <T extends SerializableCatalog> BinarySerializer<T> getSerializer();

    default <T extends SerializableCatalog> byte[] serialize( Class<T> clazz ) {
        byte[] buffer = new byte[200];
        getSerializer().encode( buffer, 0, this );
        return buffer;
    }

    default <T extends SerializableCatalog> SerializableCatalog deserialize( byte[] serialized, Class<T> clazz ) {
        return getSerializer().decode( serialized, 0 );
    }

}