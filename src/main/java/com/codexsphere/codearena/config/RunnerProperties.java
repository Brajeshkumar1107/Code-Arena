package com.codexsphere.codearena.config;

import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.execution.docker.config.DockerProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Slf4j
@ConfigurationProperties(prefix = "runner")
public class RunnerProperties {

    /**
     * Execution backend.
     * LOCAL -> ProcessBuilder
     * DOCKER -> Docker Engine
     */
    private ExecutionEnvironmentType environment =
            ExecutionEnvironmentType.LOCAL;

    /**
     * Workspace configuration.
     */
    private Workspace workspace =
            new Workspace();

    /**
     * Compilation configuration.
     */
    private Compile compile =
            new Compile();

    /**
     * Program execution configuration.
     */
    private Execution execution =
            new Execution();

    /**
     * Maximum concurrent code executions.
     */
    private Concurrency concurrency =
            new Concurrency();

    /**
     * Workspace cleanup configuration.
     */
    private Cleanup cleanup =
            new Cleanup();

    /**
     * Logging configuration.
     */
    private Logging logging =
            new Logging();

    /**
     * Docker sandbox configuration.
     */
    private DockerProperties docker =
            new DockerProperties();

    /**
     * Docker images used per programming language.
     */
    private Languages languages =
            new Languages();


    @Getter
    @Setter
    public static class Workspace {

        /**
         * Prefix used when creating temporary workspaces.
         *
         * Example:
         * exec-123456
         */
        private String prefix = "exec-";

        /**
         * Directory under which execution workspaces are created.
         *
         * When blank, the system temporary directory is used.
         *
         * For containerised deployments (docker-outside-of-docker) this
         * must be a path that is identical on the host and inside the
         * runner container, so sandbox containers can bind-mount it.
         */
        private String rootDirectory = "";
    }


    @Getter
    @Setter
    public static class Compile {

        /**
         * Maximum time allowed for compilation.
         * Unit: seconds.
         */
        private long timeout = 10;
    }


    @Getter
    @Setter
    public static class Execution {

        /**
         * Maximum time allowed for program execution.
         * Unit: seconds.
         */
        private long timeout = 2;
    }


    @Getter
    @Setter
    public static class Concurrency {

        /**
         * Maximum number of code executions
         * allowed to run simultaneously.
         */
        private int maxExecutions = 10;

        /**
         * Maximum time a request waits for an
         * execution slot.
         *
         * Unit: seconds.
         */
        private long acquireTimeoutSeconds = 1;
    }


    @Getter
    @Setter
    public static class Cleanup {

        /**
         * Whether temporary workspaces should
         * be cleaned after execution.
         */
        private boolean enabled = true;
    }


    @Getter
    @Setter
    public static class Logging {

        /**
         * Whether runner-specific logging is enabled.
         */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Languages {

        /**
         * Docker image used to compile/run Java submissions.
         */
        private String javaImage = "eclipse-temurin:21-jdk";

        /**
         * Docker image used to compile/run C++ submissions.
         */
        private String cppImage = "gcc:14";

        /**
         * Docker image used to run Python submissions.
         */
        private String pythonImage = "python:3.12";

        /**
         * Docker image used to run JavaScript submissions.
         */
        private String javascriptImage = "node:22";
    }

    private Limits limits = new Limits();

    @Getter
    @Setter
    public static class Limits {

        /**
         * Maximum source-code size in bytes.
         */
        private long maxSourceCodeBytes = 1024 * 1024; // 1 MB

        /**
         * Maximum number of test cases per request.
         */
        private int maxTestCases = 20;

        /**
         * Maximum input size per test case in bytes.
         */
        private long maxInputBytes = 1024 * 1024; // 1 MB

        /**
         * Maximum expected-output size per test case in bytes.
         */
        private long maxExpectedOutputBytes = 1024 * 1024; // 1 MB
    }

    private OutputLimits outputLimits = new OutputLimits();

    @Getter
    @Setter
    public static class OutputLimits {

        /**
         * Maximum stdout allowed per process.
         * Unit: bytes.
         */
        private long maxStdoutBytes = 1024 * 1024;

        /**
         * Maximum stderr allowed per process.
         * Unit: bytes.
         */
        private long maxStderrBytes = 1024 * 1024;
    }

    private RateLimit rateLimit =
            new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {

        /**
         * Whether API rate limiting is enabled.
         */
        private boolean enabled = true;

        /**
         * Maximum requests allowed within one window.
         */
        private int maxRequests = 30;

        /**
         * Rate-limit window in seconds.
         */
        private long windowSeconds = 60;

        /**
         * Interval at which inactive client entries
         * are removed from memory.
         */
        private long cleanupIntervalSeconds = 300;
    }
}