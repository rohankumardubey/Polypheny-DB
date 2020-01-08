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

package ch.unibas.dmi.dbis.polyphenydb.plan;


import ch.unibas.dmi.dbis.polyphenydb.rel.RelNode;
import ch.unibas.dmi.dbis.polyphenydb.rel.convert.ConverterRule;
import ch.unibas.dmi.dbis.polyphenydb.rel.metadata.RelMetadataQuery;
import ch.unibas.dmi.dbis.polyphenydb.util.Pair;
import ch.unibas.dmi.dbis.polyphenydb.util.graph.DefaultDirectedGraph;
import ch.unibas.dmi.dbis.polyphenydb.util.graph.DefaultEdge;
import ch.unibas.dmi.dbis.polyphenydb.util.graph.DirectedGraph;
import ch.unibas.dmi.dbis.polyphenydb.util.graph.Graphs;
import ch.unibas.dmi.dbis.polyphenydb.util.graph.Graphs.FrozenGraph;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;


/**
 * Definition of the the convention trait.
 * A new set of conversion information is created for each planner that registers at least one {@link ConverterRule} instance.
 *
 * Conversion data is held in a {@link LoadingCache} with weak keys so that the JVM's garbage collector may reclaim the conversion data after the planner itself has been
 * garbage collected. The conversion information consists of a graph of conversions (from one calling convention to another) and a map of graph arcs to {@link ConverterRule}s.
 */
public class ConventionTraitDef extends RelTraitDef<Convention> {

    public static final ConventionTraitDef INSTANCE = new ConventionTraitDef();


    /**
     * Weak-key cache of RelOptPlanner to ConversionData. The idea is that when
     * the planner goes away, so does the cache entry.
     */
    private final LoadingCache<RelOptPlanner, ConversionData> conversionCache = CacheBuilder.newBuilder().weakKeys().build( CacheLoader.from( ConversionData::new ) );

    //~ Constructors -----------------------------------------------------------


    private ConventionTraitDef() {
        super();
    }


    // implement RelTraitDef
    @Override
    public Class<Convention> getTraitClass() {
        return Convention.class;
    }


    @Override
    public String getSimpleName() {
        return "convention";
    }


    @Override
    public Convention getDefault() {
        return Convention.NONE;
    }


    @Override
    public void registerConverterRule( RelOptPlanner planner, ConverterRule converterRule ) {
        if ( converterRule.isGuaranteed() ) {
            ConversionData conversionData = getConversionData( planner );

            final Convention inConvention = (Convention) converterRule.getInTrait();
            final Convention outConvention = (Convention) converterRule.getOutTrait();
            conversionData.conversionGraph.addVertex( inConvention );
            conversionData.conversionGraph.addVertex( outConvention );
            conversionData.conversionGraph.addEdge( inConvention, outConvention );

            conversionData.mapArcToConverterRule.put( Pair.of( inConvention, outConvention ), converterRule );
        }
    }


    @Override
    public void deregisterConverterRule( RelOptPlanner planner, ConverterRule converterRule ) {
        if ( converterRule.isGuaranteed() ) {
            ConversionData conversionData = getConversionData( planner );

            final Convention inConvention = (Convention) converterRule.getInTrait();
            final Convention outConvention = (Convention) converterRule.getOutTrait();

            final boolean removed = conversionData.conversionGraph.removeEdge( inConvention, outConvention );
            assert removed;
            conversionData.mapArcToConverterRule.remove( Pair.of( inConvention, outConvention ), converterRule );
        }
    }


    // implement RelTraitDef
    @Override
    public RelNode convert( RelOptPlanner planner, RelNode rel, Convention toConvention, boolean allowInfiniteCostConverters ) {
        final RelMetadataQuery mq = rel.getCluster().getMetadataQuery();
        final ConversionData conversionData = getConversionData( planner );

        final Convention fromConvention = rel.getConvention();

        List<List<Convention>> conversionPaths = conversionData.getPaths( fromConvention, toConvention );

        loop:
        for ( List<Convention> conversionPath : conversionPaths ) {
            assert conversionPath.get( 0 ) == fromConvention;
            assert conversionPath.get( conversionPath.size() - 1 ) == toConvention;
            RelNode converted = rel;
            Convention previous = null;
            for ( Convention arc : conversionPath ) {
                if ( planner.getCost( converted, mq ).isInfinite() && !allowInfiniteCostConverters ) {
                    continue loop;
                }
                if ( previous != null ) {
                    converted = changeConvention( converted, previous, arc, conversionData.mapArcToConverterRule );
                    if ( converted == null ) {
                        throw new AssertionError( "Converter from " + previous + " to " + arc + " guaranteed that it could convert any relexp" );
                    }
                }
                previous = arc;
            }
            return converted;
        }

        return null;
    }


    /**
     * Tries to convert a relational expression to the target convention of an arc.
     */
    private RelNode changeConvention( RelNode rel, Convention source, Convention target, final Multimap<Pair<Convention, Convention>, ConverterRule> mapArcToConverterRule ) {
        assert source == rel.getConvention();

        // Try to apply each converter rule for this arc's source/target calling conventions.
        final Pair<Convention, Convention> key = Pair.of( source, target );
        for ( ConverterRule rule : mapArcToConverterRule.get( key ) ) {
            assert rule.getInTrait() == source;
            assert rule.getOutTrait() == target;
            RelNode converted = rule.convert( rel );
            if ( converted != null ) {
                return converted;
            }
        }
        return null;
    }


    @Override
    public boolean canConvert( RelOptPlanner planner, Convention fromConvention, Convention toConvention ) {
        ConversionData conversionData = getConversionData( planner );
        return fromConvention.canConvertConvention( toConvention ) || conversionData.getShortestPath( fromConvention, toConvention ) != null;
    }


    private ConversionData getConversionData( RelOptPlanner planner ) {
        return conversionCache.getUnchecked( planner );
    }


    /**
     * Workspace for converting from one convention to another.
     */
    private static final class ConversionData {

        final DirectedGraph<Convention, DefaultEdge> conversionGraph = DefaultDirectedGraph.create();

        /**
         * For a given source/target convention, there may be several possible conversion rules. Maps {@link DefaultEdge} to a collection of {@link ConverterRule} objects.
         */
        final Multimap<Pair<Convention, Convention>, ConverterRule> mapArcToConverterRule = HashMultimap.create();

        private FrozenGraph<Convention, DefaultEdge> pathMap;


        public List<List<Convention>> getPaths( Convention fromConvention, Convention toConvention ) {
            return getPathMap().getPaths( fromConvention, toConvention );
        }


        private FrozenGraph<Convention, DefaultEdge> getPathMap() {
            if ( pathMap == null ) {
                pathMap = Graphs.makeImmutable( conversionGraph );
            }
            return pathMap;
        }


        public List<Convention> getShortestPath( Convention fromConvention, Convention toConvention ) {
            return getPathMap().getShortestPath( fromConvention, toConvention );
        }
    }
}