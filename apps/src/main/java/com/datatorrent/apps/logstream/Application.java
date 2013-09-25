/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.apps.logstream;

import com.datatorrent.api.Context.OperatorContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import com.datatorrent.api.DAG;
import com.datatorrent.api.StreamingApplication;
import com.datatorrent.lib.algo.TopNUnique;
import com.datatorrent.lib.io.ConsoleOutputOperator;
import com.datatorrent.lib.logs.DimensionObject;
import com.datatorrent.lib.logs.MultiWindowDimensionAggregation;
import com.datatorrent.lib.util.DimensionTimeBucketOperator;
import com.datatorrent.lib.util.DimensionTimeBucketSumOperator;

/**
 * Log stream processing application based on DataTorrent platform.<br>
 * This application consumes log data generated by running systems and services
 * in near real-time, and processes it to produce actionable data.  This in turn
 * can be used to produce alerts, take corrective actions, or predict system
 * behavior.
 * <p>
 * Running Java Test or Main app in IDE:
 *
 * <pre>
 * LocalMode.runApp(new Application(), 600000); // 10 min run
 * </pre>
 *
 * Output : <br>
 * During successful deployment and run, user should see following output:
 * TODO
 * <pre>
 * </pre>
 *
 * Application DAG : <br>
 * TODO
 * <img src="doc-files/Application.gif" width=600px > <br>
 * <br>
 *
 * Streaming Window Size : 1000 ms(1 Sec) <br>
 * Operator Details : <br>
 * <ul>
 * <li><b>The operator Console: </b> This operator just outputs the input tuples
 * to the console (or stdout). You can use other output adapters if needed.<br>
 * </li>
 * </ul>
 *
 * @since 0.3.5
 */
public class Application implements StreamingApplication
{
  public DimensionTimeBucketSumOperator getPageDimensionTimeBucketSumOperator(String name, DAG dag)
  {

    DimensionTimeBucketSumOperator oper = dag.addOperator(name, DimensionTimeBucketSumOperator.class);
    oper.addDimensionKeyName("host");
    oper.addDimensionKeyName("clientip");
    oper.addDimensionKeyName("request");
    oper.addDimensionKeyName("agent");
    oper.addDimensionKeyName("response");

    oper.addValueKeyName("bytes");
    Set<String> dimensionKey = new HashSet<String>();

    dimensionKey.add("response");
    dimensionKey.add("clientip");
    try {
      oper.addCombination(dimensionKey);
    } catch (NoSuchFieldException e) {
    }

    oper.setTimeBucketFlags(DimensionTimeBucketOperator.TIMEBUCKET_MINUTE);
    return oper;
  }

  private MultiWindowDimensionAggregation getAggregationOper(String name, DAG dag)
  {
    MultiWindowDimensionAggregation oper = dag.addOperator("sliding_window", MultiWindowDimensionAggregation.class);
    oper.setWindowSize(3);
    List<int[]> dimensionArrayList = new ArrayList<int[]>();
    int[] dimensionArray = { 4, 1 };
    int[] dimensionArray_2 = { 0 };
//    dimensionArrayList.add(dimensionArray_2);
    dimensionArrayList.add(dimensionArray);
    //dimensionArrayList.add(dimensionArray_2);
    oper.setDimensionArray(dimensionArrayList);

    oper.setTimeBucket("m");
    oper.setDimensionKeyVal("1");

   // oper.setOperationType(AggregateOperation.AVERAGE);

    return oper;
  }

  @Override
  public void populateDAG(DAG dag, Configuration conf)
  {
    // set app name
    dag.setAttribute(DAG.APPLICATION_NAME, "SiteOperationsApplication");

    /*
     * Read log file messages from a messaging system (Redis, RabbitMQ, etc)
     * Typically one message equates to a single line in a log file, but in
     * some cases may be multiple lines such as java stack trace, etc.
     */

    // Get logs from RabbitMQ
    RabbitMQLogsInputOperator apacheLogInput = dag.addOperator("ApacheLogInput", RabbitMQLogsInputOperator.class);

    // dynamically partition based on number of incoming tuples from the queue
    dag.setAttribute(apacheLogInput, OperatorContext.INITIAL_PARTITION_COUNT, 2);
    dag.setAttribute(apacheLogInput, OperatorContext.PARTITION_TPS_MIN, 1000);
    dag.setAttribute(apacheLogInput, OperatorContext.PARTITION_TPS_MAX, 3000);

    /*
     * Convert incoming JSON structures to flattened map objects
     */
    // TODO

    /*
     * Explode dimensions based on log types ( apache, mysql, syslog, etc)
     */
    DimensionTimeBucketSumOperator dimensionOperator = getPageDimensionTimeBucketSumOperator("Dimension", dag);
    dag.addStream("dimension_in", apacheLogInput.outputPort, dimensionOperator.in);

    /*
     * Calculate average, min, max, etc from dimensions ( based on log types )
     */
    // aggregating over sliding window
    MultiWindowDimensionAggregation multiWindowAggOpr = getAggregationOper("sliding_window", dag);
    dag.addStream("dimension_out", dimensionOperator.out, multiWindowAggOpr.data);

    // adding top N operator
    TopNUnique<String, DimensionObject<String>> topNOpr = dag.addOperator("topN", new TopNUnique<String, DimensionObject<String>>());
    topNOpr.setN(5);
    dag.addStream("aggregation_topn", multiWindowAggOpr.output, topNOpr.data);
    /*
     * Websocket output to UI from calculated aggregations
     */
    //TODO

    /*
     * Pattern recognition
     */
    //TODO

    /*
     * Alerts
     */
    //TODO

    /*
     * Console output for debugging purposes
     */
    ConsoleOutputOperator console = dag.addOperator("console", ConsoleOutputOperator.class);
    dag.addStream("topn_output", topNOpr.top, console.input);

  }

}
