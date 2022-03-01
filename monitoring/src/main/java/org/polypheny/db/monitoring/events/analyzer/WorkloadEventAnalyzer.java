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

package org.polypheny.db.monitoring.events.analyzer;

import org.polypheny.db.monitoring.events.WorkloadEvent;
import org.polypheny.db.monitoring.events.metrics.WorkloadDataPoint;

public class WorkloadEventAnalyzer {

    public static WorkloadDataPoint analyze( WorkloadEvent workloadEvent ) {
        WorkloadDataPoint metric = WorkloadDataPoint
                .builder()
                .Id( workloadEvent.getId() )
                .recordedTimestamp( workloadEvent.getRecordedTimestamp() )
                .isCommitted( workloadEvent.isCommitted() )
                .monitoringType( workloadEvent.getMonitoringType() )
                .algNode( workloadEvent.getAlgNode() )
                .build();

        return metric;
    }

}
