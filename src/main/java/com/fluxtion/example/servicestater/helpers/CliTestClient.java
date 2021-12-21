package com.fluxtion.example.servicestater.helpers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fluxtion.example.servicestater.Service;
import com.fluxtion.example.servicestater.ServiceManagerServer;
import com.fluxtion.example.servicestater.graph.FluxtionServiceManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * A command line client that tests a sample service graph loaded into the {@link FluxtionServiceManager}
 * <p>
 * Various cli commands are provided to exercise all the operations on the service manager. Run the program and a help
 * message is displayed detailing the usage.
 */
//@Log
@Slf4j
public class CliTestClient {

    private static ServiceManagerServer serviceManagerServer;

    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Welcome to FluxtionService interactive tester - building test service graph");
        System.out.println("===============================================================================");
        auditOn(false);
        buildGraph(false);
        serviceManagerServer.startService("aggAB");
        Scanner scanner = new Scanner(System.in);
        boolean run = true;
        Thread.sleep(100);//give the threads time to start and publish help message at end
        printHelp();
        while (run) {
            System.out.print(">");
            String command = scanner.next().toLowerCase(Locale.ROOT);
            switch (command) {
                case "build", "b" -> buildGraph(false);
                case "compile", "c" -> buildGraph(true);
                case "status", "ss" -> printStatus();
                case "startall", "sa" -> startAll();
                case "stopall", "ha" -> stopAll();
                case "start", "s" -> startByName(scanner);
                case "stop", "h" -> stopByName(scanner);
                case "ns" -> notifiedStartedByName(scanner);
                case "nh" -> notifiedStoppedByName(scanner);
                case "auditon", "aon" -> auditOn(true);
                case "auditoff", "aoff" -> auditOn(false);
                case "printtree", "pt" -> printTree();
                case "exit", "e" -> run = false;
                case "help", "?" -> printHelp();
                default -> System.out.println("unknown command:" + command + " ? for command list");
            }
            scanner.nextLine();
        }
        scanner.close();
        serviceManagerServer.shutdown();
    }

    static void printHelp() {
        String help = """
                                
                FluxtionService interactive tester commands:
                ===============================================
                help or ?                 - print this message
                build or b                - drops the graph and builds a new interpreted graph from scratch
                compile or c              - drops the graph and builds a new graph from scratch, generated and compiles java source code
                status or ss              - prints the current status of the graph to console
                startAll or sa            - start all services
                stopAll or ha             - stop all services
                start or s [service name] - start a single services by name
                stop or h [service name]  - stop a single service by name
                ns [service name]         - notify of started status for a single service by name
                nh [service name]         - notify of stopped status for a single service by name
                auditOn or aon            - turn audit recording on
                auditOff or aoff          - turn audit recording off
                printTree or pt           - print the DAG of the test model
                exit or e                 - exit the application
                """;
        System.out.println(help);
    }

    private static void printTree() {
        System.out.println(asciiArtDAG);
    }

    public static void startAll() {
        checkControllerIsBuilt();
        serviceManagerServer.startAllServices();
    }

    public static void stopAll() {
        checkControllerIsBuilt();
        serviceManagerServer.stopAllServices();
    }

    private static void printStatus() {
        checkControllerIsBuilt();
        serviceManagerServer.publishServiceStatus();
    }

    private static void checkControllerIsBuilt() {
        if (serviceManagerServer == null) {
            System.out.println("no service manager built, building one first");
            buildGraph(false);
        }
    }

    private static void startByName(Scanner scanner) {
        checkControllerIsBuilt();
        if (scanner.hasNext()) {
            serviceManagerServer.startService(scanner.next());
        } else {
            System.out.println("2nd argument required - service name");
        }
    }

    private static void stopByName(Scanner scanner) {
        checkControllerIsBuilt();
        if (scanner.hasNext()) {
            serviceManagerServer.stopService(scanner.next());
        } else {
            System.out.println("2nd argument required - service name");
        }
    }

    private static void notifiedStartedByName(Scanner scanner) {
        checkControllerIsBuilt();
        if (scanner.hasNext()) {
            serviceManagerServer.serviceStartedNotification(scanner.next());
        } else {
            System.out.println("2nd argument required - service name");
        }
    }

    private static void notifiedStoppedByName(Scanner scanner) {
        checkControllerIsBuilt();
        if (scanner.hasNext()) {
            serviceManagerServer.serviceStoppedNotification(scanner.next());
        } else {
            System.out.println("2nd argument required - service name");
        }
    }

    private static void auditOn(boolean flag) {
        Logger restClientLogger = (Logger) LoggerFactory.getLogger("fluxtion.eventLog");
        if (flag) {
            restClientLogger.setLevel(Level.INFO);
        } else {
            restClientLogger.setLevel(Level.OFF);
        }
    }

    private static final String HANDLER_A = "handlerA";
    private static final String HANDLER_B = "handlerB";
    private static final String HANDLER_C = "handlerC";
    private static final String AGG_AB = "aggAB";
    private static final String CALC_C = "calcC";
    private static final String PERSISTER = "persister";

    private static void buildGraph(boolean compile) {
        if (serviceManagerServer != null) {
            serviceManagerServer.shutdown();
        }
        Service handlerA = Service.builder(HANDLER_A).build();
        Service handlerB = Service.builder(HANDLER_B).build();
        Service handlerC = Service.builder(HANDLER_C).build();
        Service aggAB = Service.builder(AGG_AB)
                .servicesThatRequireMe(List.of(handlerA, handlerB))
                .startTask(CliTestClient::notifyStartedAggAB)
                .build();
        Service calcC = Service.builder(CALC_C)
                .servicesThatRequireMe(List.of(handlerC))
                .build();
        Service persister = Service.builder(PERSISTER)
                .servicesThatRequireMe(List.of(aggAB, calcC))
                .startTask(CliTestClient::notifyStartedPersister)
                .build();

        if (compile) {
            serviceManagerServer = ServiceManagerServer.compiledServer(persister, aggAB, calcC, handlerA, handlerB, handlerC);
        } else {
            serviceManagerServer = ServiceManagerServer.interpretedServer(persister, aggAB, calcC, handlerA, handlerB, handlerC);
        }
        serviceManagerServer.registerStatusListener(new PublishServiceStatusRecordToLog());
    }

    public static void notifyStartedPersister() {
        log.info("persister::startTask notify persister STARTED");
        serviceManagerServer.serviceStartedNotification(PERSISTER);
    }

    public static void notifyStartedAggAB() {
        log.info("aggAB::startTask notify aggAB STARTED");
        serviceManagerServer.serviceStartedNotification(AGG_AB);
    }

    private static final String asciiArtDAG = """
                Tree view of model
               
                +-------------+                      +------------+              +-----------+      |
                |             |                      |            |              |           |      |
                |  handler_c  |                      | handler_a  |              | handler_b |      |
                +---+---------+                      +----+-------+              +-----+-----+      |
                    |                                     |                            |            |
                    |    +---------+                      |        +-------+           |            |   DIRECTION OF
                    |    |         |                      |        |       |           |            |   EVENT FLOW
                    +----+ calc_c  |                      +--------+agg_AB +-----------+            |
                         +----+----+                               +---+---+                        |
                              |                                        |                            |
                              |                                        |                            |
                              |                +-----------+           |                            |
                              |                |           |           |                            |
                              +----------------+ persister +-----------+                            |
                                               |           |                                        |
                                               +-----------+                                        v
               
                        
            """;
}
