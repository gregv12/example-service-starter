# Fluxtion service starter
A server for managing the deterministic execution of start and stop tasks for a set of interdependent services. Implemented
with [Fluxtion](https://github.com/v12technology/fluxtion) to manage the underlying directed acyclic graph of services.

### Overview
In many systems services execute independently but need to co-ordinate their lifecycle with each other. A service
may require all downstream service to be started before starting itself and becoming available to accept upstream requests.
Similarly, if a downstream service becomes unavailable all upstream services will need to be notified and take appropriate 
actions. As systems grow a complex graph of interdependent services quickly arises, the difficulty in correctly managing 
lifecycle overwhelms a handwritten manual solution. 

Service starter provides an automated utility for managing the lifecycle of independent services, executing
start and stop tasks associated with a particular service at the correct time.

### Main components
- **ServiceManagerServer** manages the execution of lifecycle tasks associated with a service. The server places all 
 clients requests on a queue and executes them on its own thread. 
- **Service** - To manage a user service a proxy Service is instantiated and registered with the ServiceManagerServer. 
 An external service has a one to one mapping with a Service. Data held by the Service:
  - name - a unique service name that can be identified globally
  - dependents - Upstream services that require this service to be started for validity
  - start task - an optional task that is executed when the service enters the STARTING lifecycle phase
  - stop task - an optional task that is executed when the service enters the STOPPING lifecycle phase
- **Consumer<List<TaskWrapper>>** Consumes start/stop tasks generated by the ServiceManagerServer. A default implementation
 is provided, ServiceManagerServer, that executes tasks on its own executor thread.

#### Threading
ServiceManagerServer is threadsafe, all methods return immediately and the request is placed onto a queue for execution.
A single thread pulls the request from the queue and then reads/writes the underlying model all on a single thread. 
Thread name is serviceManagerThread-(n) where n is a global count

A task list may be produced by the ServiceManagerServer and pushed to a registered consumer. A ServiceTaskExecutor 
instance is registered with the ServiceManagerServer by default. The ServiceTaskExecutor executes all tasks on its own 
thread, the thread name is taskExecutor-(n) where n is a global count

### Programming overview
There are two phases for using the service starter. Phase 1 create and build a model. Phase 2 use the model in a running 
environment

#### Building a model

#### Executing a model

### Example

### Cli test client



### Service lifecycle
The service starter manages a set of services with the following behaviour
1. A service can be started if all its dependencies are in a STARTED state
2. A service can be started if it has no dependencies
3. A call to FluxtionSystemManager.start() will start any services that have no dependencies
4. Any service the FluxtionSystemManager starts will move to the STARTING state, and a [start command](https://github.com/gregv12/example-service-starter/blob/d15d4856af4f0315d08474de5fda74f849886757/src/main/java/com/fluxtion/example/servicestater/ServiceEvent.java#L57) will be published
5. Any dependencies of a STARTING service will move to WAITING_FOR_PARENTS_TO_START state
6. A service moves to STARTED state when a StatusUpdate with STARTED state is invoked by calling FluxtionSystemManager.processStatusUpdate() 
7. When all dependencies of a WAITING_FOR_PARENTS_TO_START service have STARTED state this service will be started, see (4) above
9. Continues down the dependency tree until all services are started

Stopping has the same behaviour for the reverse dependency order.

### States
    STATUS_UNKNOWN,
    WAITING_FOR_PARENTS_TO_START,
    STARTING,
    STARTED,
    WAITING_FOR_PARENTS_TO_STOP,
    STOPPING,
    STOPPED,
