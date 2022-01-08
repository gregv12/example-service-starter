package com.fluxtion.example.servicestater.graph;

import com.fluxtion.example.servicestater.Service.Status;
import com.fluxtion.example.servicestater.ServiceModels;
import com.fluxtion.example.servicestater.ServiceStatusRecord;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fluxtion.example.servicestater.ServiceModels.mapWithStatus;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
class FluxtionServiceManagerModelATest {

    /**
     * <pre>
     *
     * Tree view of model A
     *
     * +-------------+                      +------------+              +-----------+      |
     * |             |                      |            |              |           |      |
     * |  handler_c  |                      | handler_a  |              | handler_b |      |
     * +---+---------+                      +----+-------+              +-----+-----+      |
     *     |                                     |                            |            |
     *     |    +---------+                      |        +-------+           |            |   DIRECTION OF
     *     |    |         |                      |        |       |           |            |   EVENT FLOW
     *     +----+ calc_c  |                      +--------+agg_AB +-----------+            |
     *          +----+----+                               +---+---+                        |
     *               |                                        |                            |
     *               |                                        |                            |
     *               |                +-----------+           |                            |
     *               |                |           |           |                            |
     *               +----------------+ persister +-----------+                            |
     *                                |           |                                        |
     *                                +-----------+                                        v
     *
     *
     * </pre>
     * drawn with - https://asciiflow.com/#/
     */


    protected boolean ADD_AUDIT_LOG = false;
    protected boolean COMPILED = false;
    protected final List<ServiceStatusRecord> statusList = new ArrayList<>();

    @BeforeEach
    public void beforeTest() {
        ADD_AUDIT_LOG = false;
        COMPILED = false;
    }

    @Test
    void buildSystemController() {
        var serviceManager = ServiceModels.buildModelA(ADD_AUDIT_LOG, COMPILED);
        serviceManager.registerStatusListener(this::recordServiceStatus);
        assertEquals(6, statusList.size());
        checkStatusMatch(ServiceModels.allUnknownStatus());
    }

    @Test
    void startingAggAB() {
        var serviceManager = ServiceModels.buildModelA(ADD_AUDIT_LOG, COMPILED);
        serviceManager.registerStatusListener(this::recordServiceStatus);
        serviceManager.startService(ServiceModels.AGG_AB);
        var statusMap = mapWithStatus(Status.STATUS_UNKNOWN);
        updateStatus(statusMap, ServiceModels.AGG_AB, Status.WAITING_FOR_PARENTS_TO_START);
        updateStatus(statusMap, ServiceModels.PERSISTER, Status.STARTING);
        checkStatusMatch(statusMap);
    }

    @Test
    void startingAggABThenNotifyPersisterStart() {
//        ADD_AUDIT_LOG = true;
//        COMPILED = true;
        var serviceManager = startAService(ServiceModels.AGG_AB);
        var statusMap = mapWithStatus(Status.STATUS_UNKNOWN);
        serviceManager.serviceStarted(ServiceModels.PERSISTER);
        updateStatus(statusMap, ServiceModels.AGG_AB, Status.STARTING);
        updateStatus(statusMap, ServiceModels.PERSISTER, Status.STARTED);
        checkStatusMatch(statusMap);
    }

    @Test
    void startingAggABThenNotifyPersisterStartCompiled() {
//        ADD_AUDIT_LOG = true;
        COMPILED = true;
        var serviceManager = startAService(ServiceModels.AGG_AB);
        var statusMap = mapWithStatus(Status.STATUS_UNKNOWN);
        serviceManager.serviceStarted(ServiceModels.PERSISTER);
        updateStatus(statusMap, ServiceModels.AGG_AB, Status.STARTING);
        updateStatus(statusMap, ServiceModels.PERSISTER, Status.STARTED);
        checkStatusMatch(statusMap);
    }

    @Test
    void stopAll(){
        ADD_AUDIT_LOG = true;
        FluxtionServiceManager fluxtionServiceManager = ServiceModels.buildModelA(ADD_AUDIT_LOG, COMPILED);
        fluxtionServiceManager.registerStatusListener(this::recordServiceStatus);
        fluxtionServiceManager.stopAllServices();
        Map<String, ServiceStatusRecord> statusMap = mapWithStatus(Status.WAITING_FOR_PARENTS_TO_STOP);
        updateStatus(statusMap, ServiceModels.HANDLER_A, Status.STOPPING);
        updateStatus(statusMap, ServiceModels.HANDLER_B, Status.STOPPING);
        updateStatus(statusMap, ServiceModels.HANDLER_C, Status.STOPPING);
        checkStatusMatch(statusMap);
    }

    @Test
    void startAll(){
        ADD_AUDIT_LOG = true;
        FluxtionServiceManager fluxtionServiceManager = ServiceModels.buildModelA(ADD_AUDIT_LOG, COMPILED);
        fluxtionServiceManager.registerStatusListener(this::recordServiceStatus);
        fluxtionServiceManager.stopAllServices();
        fluxtionServiceManager.startAllServices();
        Map<String, ServiceStatusRecord> statusMap = mapWithStatus(Status.WAITING_FOR_PARENTS_TO_START);
        updateStatus(statusMap, ServiceModels.PERSISTER, Status.STARTING);
        checkStatusMatch(statusMap);
    }

    private FluxtionServiceManager startAService(String serviceName) {
        FluxtionServiceManager fluxtionServiceManager = ServiceModels.buildModelA(ADD_AUDIT_LOG, COMPILED);
        fluxtionServiceManager.registerStatusListener(this::recordServiceStatus);
        fluxtionServiceManager.startService(serviceName);
        return fluxtionServiceManager;
    }

    protected void checkStatusMatch(List<ServiceStatusRecord> statusMap) {
        assertThat(statusList, Matchers.containsInAnyOrder(statusMap.toArray()));
    }

    protected void checkStatusMatch(Map<String, ServiceStatusRecord> statusMap) {
        assertThat(statusList, Matchers.containsInAnyOrder(statusMap.values().toArray()));
    }

    static void updateStatus(Map<String, ServiceStatusRecord> statusMap, String serviceName, Status status) {
        statusMap.put(serviceName, new ServiceStatusRecord(serviceName, status));
    }

    public void recordServiceStatus(List<ServiceStatusRecord> statusUpdate) {
        statusList.clear();
        statusList.addAll(statusUpdate);
        if (ADD_AUDIT_LOG) {
            BaseServiceStarterTest.logStatus(statusUpdate);
        }
    }


}
