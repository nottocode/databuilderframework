package com.flipkart.cp.convert.europa.databuilderframework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.cp.convert.europa.databuilderframework.engine.DataBuilderMetadataManager;
import com.flipkart.cp.convert.europa.databuilderframework.engine.DataFlowBuilder;
import com.flipkart.cp.convert.europa.databuilderframework.engine.DataFlowExecutor;
import com.flipkart.cp.convert.europa.databuilderframework.engine.SimpleDataFlowExecutor;
import com.flipkart.cp.convert.europa.databuilderframework.engine.impl.DataBuilderFactoryImpl;
import com.flipkart.cp.convert.europa.databuilderframework.model.*;
import com.flipkart.cp.convert.europa.databuilderframework.util.DataSetAccessor;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DataFlowWithTransientDataTest {

    private DataBuilderMetadataManager dataBuilderMetadataManager = new DataBuilderMetadataManager();
    private DataFlowExecutor executor = new SimpleDataFlowExecutor(new DataBuilderFactoryImpl(dataBuilderMetadataManager));
    private DataFlowBuilder dataFlowBuilder = new DataFlowBuilder(dataBuilderMetadataManager);
    private DataFlow dataFlow = new DataFlow();
    private DataFlow dataFlowError = new DataFlow();

    @Before
    public void setup() throws Exception {
        dataBuilderMetadataManager.register(Lists.newArrayList("A", "B"), "C", "BuilderA", TestBuilderA.class );
        dataBuilderMetadataManager.register(Lists.newArrayList("C", "D"), "E", "BuilderB", TestBuilderB.class );
        dataBuilderMetadataManager.register(Lists.newArrayList("C", "E"), "F", "BuilderC", TestBuilderC.class );
        dataBuilderMetadataManager.register(Lists.newArrayList("X"), "Y", "BuilderX", TestBuilderError.class );
        //dataBuilderMetadataManager.register(Lists.newArrayList("F"),      "G", "BuilderD", TestBuilderD.class );
        //dataBuilderMetadataManager.register(Lists.newArrayList("E", "C"), "G", "BuilderE", TestBuilderE.class );

        dataFlow.setTargetData("F");
        dataFlow.setExecutionGraph(dataFlowBuilder.generateGraph(dataFlow).deepCopy());
        dataFlow.setTransients(Sets.newHashSet("C"));
        System.out.println(new ObjectMapper().writeValueAsString(dataFlow));
    }

    @Test
    public void testTransient() throws Exception {
        DataFlowInstance dataFlowInstance = new DataFlowInstance();
        dataFlowInstance.setId("testflow");
        dataFlowInstance.setDataFlow(dataFlow);
        {
            DataDelta dataDelta = new DataDelta(Lists.<Data>newArrayList(new TestDataA("Hello")));
            DataExecutionResponse response = executor.run(dataFlowInstance, dataDelta);
            Assert.assertTrue(response.getResponses().isEmpty());
        }
        Data dataC = null;
        {
            DataDelta dataDelta = new DataDelta(Lists.<Data>newArrayList(new TestDataB("World")));
            DataExecutionResponse response = executor.run(dataFlowInstance, dataDelta);
            Assert.assertFalse(response.getResponses().isEmpty());
            Assert.assertTrue(response.getResponses().containsKey("C"));
            DataSetAccessor accessor = new DataSetAccessor(dataFlowInstance.getDataSet());
            Assert.assertTrue(accessor.checkForData(Lists.newArrayList("A", "B")));
            Assert.assertFalse(accessor.checkForData("C"));
            dataC = response.getResponses().get("C");
        }
        {
            DataDelta dataDelta = new DataDelta(Lists.newArrayList(new TestDataD("this"), dataC));
            DataExecutionResponse response = executor.run(dataFlowInstance, dataDelta);
            Assert.assertFalse(response.getResponses().isEmpty());
            Assert.assertTrue(response.getResponses().containsKey("E"));
            Assert.assertTrue(response.getResponses().containsKey("F"));
        }
    }
}