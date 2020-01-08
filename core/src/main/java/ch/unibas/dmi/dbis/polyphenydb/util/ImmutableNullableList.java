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

package ch.unibas.dmi.dbis.polyphenydb.util;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * An immutable list that may contain null values.
 *
 * If the list cannot contain null values, use {@link com.google.common.collect.ImmutableList}.
 *
 * @param <E> Element type
 */
public class ImmutableNullableList<E> extends AbstractList<E> {

    private static final List SINGLETON_NULL = Collections.singletonList( null );

    private final E[] elements;


    // private - does not copy array
    private ImmutableNullableList( E[] elements ) {
        this.elements = elements;
    }


    /**
     * Returns an immutable list containing the given elements, in order.
     *
     * Behavior as {@link com.google.common.collect.ImmutableList#copyOf(java.util.Collection)} except that this list allows nulls.
     */
    public static <E> List<E> copyOf( Collection<? extends E> elements ) {
        if ( elements instanceof ImmutableNullableList || elements instanceof ImmutableList || elements == SINGLETON_NULL ) {
            //noinspection unchecked
            return (List<E>) elements;
        }
        if ( elements == Collections.EMPTY_LIST ) {
            return ImmutableList.of();
        }
        // If there are no nulls, ImmutableList is better.
        for ( E object : elements ) {
            if ( object == null ) {
                final Object[] objects = elements.toArray();
                //noinspection unchecked
                return new ImmutableNullableList<>( (E[]) objects );
            }
        }
        return ImmutableList.copyOf( elements );
    }


    /**
     * Returns an immutable list containing the given elements, in order.
     *
     * Behavior as {@link com.google.common.collect.ImmutableList#copyOf(Iterable)} except that this list allows nulls.
     */
    public static <E> List<E> copyOf( Iterable<? extends E> elements ) {
        if ( elements instanceof ImmutableNullableList || elements instanceof ImmutableList || elements == SINGLETON_NULL ) {
            //noinspection unchecked
            return (List<E>) elements;
        }
        if ( elements instanceof Collection ) {
            //noinspection unchecked
            return copyOf( (Collection) elements );
        }
        // If there are no nulls, ImmutableList is better.
        final List<E> list = new ArrayList<>();
        Iterables.addAll( list, elements );
        if ( list.contains( null ) ) {
            return ImmutableNullableList.copyOf( list );
        }
        return ImmutableList.copyOf( elements );
    }


    /**
     * Returns an immutable list containing the given elements, in order.
     *
     * Behavior as {@link com.google.common.collect.ImmutableList#copyOf(Object[])} except that this list allows nulls.
     */
    public static <E> List<E> copyOf( E[] elements ) {
        // Check for nulls.
        for ( E object : elements ) {
            if ( object == null ) {
                return new ImmutableNullableList<>( elements.clone() );
            }
        }
        // There are no nulls. ImmutableList is better.
        return ImmutableList.copyOf( elements );
    }


    /**
     * Creates an immutable list of 1 element.
     */
    public static <E> List<E> of( E e1 ) {
        //noinspection unchecked
        return e1 == null ? (List<E>) SINGLETON_NULL : ImmutableList.of( e1 );
    }


    /**
     * Creates an immutable list of 2 elements.
     */
    public static <E> List<E> of( E e1, E e2 ) {
        // Only we can see the varargs array. Therefore the list is immutable.
        //noinspection unchecked
        return UnmodifiableArrayList.of( e1, e2 );
    }


    /**
     * Creates an immutable list of 3 elements.
     */
    public static <E> List<E> of( E e1, E e2, E e3 ) {
        // Only we can see the varargs array. Therefore the list is immutable.
        //noinspection unchecked
        return UnmodifiableArrayList.of( e1, e2, e3 );
    }


    /**
     * Creates an immutable list of 4 elements.
     */
    public static <E> List<E> of( E e1, E e2, E e3, E e4 ) {
        // Only we can see the varargs array. Therefore the list is immutable.
        //noinspection unchecked
        return UnmodifiableArrayList.of( e1, e2, e3, e4 );
    }


    /**
     * Creates an immutable list of 5 elements.
     */
    public static <E> List<E> of( E e1, E e2, E e3, E e4, E e5 ) {
        // Only we can see the varargs array. Therefore the list is immutable.
        //noinspection unchecked
        return UnmodifiableArrayList.of( e1, e2, e3, e4, e5 );
    }


    /**
     * Creates an immutable list of 6 elements.
     */
    public static <E> List<E> of( E e1, E e2, E e3, E e4, E e5, E e6 ) {
        // Only we can see the varargs array. Therefore the list is immutable.
        //noinspection unchecked
        return UnmodifiableArrayList.of( e1, e2, e3, e4, e5, e6 );
    }


    /**
     * Creates an immutable list of 7 elements.
     */
    public static <E> List<E> of( E e1, E e2, E e3, E e4, E e5, E e6, E e7 ) {
        // Only we can see the varargs array. Therefore the list is immutable.
        //noinspection unchecked
        return UnmodifiableArrayList.of( e1, e2, e3, e4, e5, e6, e7 );
    }


    /**
     * Creates an immutable list of 8 or more elements.
     */
    public static <E> List<E> of( E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E... others ) {
        Object[] array = new Object[8 + others.length];
        array[0] = e1;
        array[1] = e2;
        array[2] = e3;
        array[3] = e4;
        array[4] = e5;
        array[5] = e6;
        array[6] = e7;
        array[7] = e8;
        System.arraycopy( others, 0, array, 8, others.length );
        //noinspection unchecked
        return new ImmutableNullableList<>( (E[]) array );
    }


    @Override
    public E get( int index ) {
        return elements[index];
    }


    @Override
    public int size() {
        return elements.length;
    }


    /**
     * Returns a new builder. The generated builder is equivalent to the builder created by the {@link Builder} constructor.
     */
    public static <E> Builder<E> builder() {
        return new Builder<>();
    }


    /**
     * A builder for creating immutable nullable list instances.
     *
     * @param <E> element type
     */
    public static final class Builder<E> {

        private final List<E> contents = new ArrayList<>();


        /**
         * Creates a new builder. The returned builder is equivalent to the builder generated by
         * {@link ch.unibas.dmi.dbis.polyphenydb.util.ImmutableNullableList#builder}.
         */
        public Builder() {
        }


        /**
         * Adds {@code element} to the {@code ImmutableNullableList}.
         *
         * @param element the element to add
         * @return this {@code Builder} object
         */
        public Builder<E> add( E element ) {
            contents.add( element );
            return this;
        }


        /**
         * Adds each element of {@code elements} to the {@code ImmutableNullableList}.
         *
         * @param elements the {@code Iterable} to add to the {@code ImmutableNullableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null
         */
        public Builder<E> addAll( Iterable<? extends E> elements ) {
            Iterables.addAll( contents, elements );
            return this;
        }


        /**
         * Adds each element of {@code elements} to the {@code ImmutableNullableList}.
         *
         * @param elements the elements to add to the {@code ImmutableNullableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null
         */
        public Builder<E> add( E... elements ) {
            for ( E element : elements ) {
                add( element );
            }
            return this;
        }


        /**
         * Adds each element of {@code elements} to the {@code ImmutableNullableList}.
         *
         * @param elements the elements to add to the {@code ImmutableNullableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null
         */
        public Builder<E> addAll( Iterator<? extends E> elements ) {
            Iterators.addAll( contents, elements );
            return this;
        }


        /**
         * Returns a newly-created {@code ImmutableNullableList} based on the contents of the {@code Builder}.
         */
        public List<E> build() {
            return copyOf( contents );
        }
    }
}
