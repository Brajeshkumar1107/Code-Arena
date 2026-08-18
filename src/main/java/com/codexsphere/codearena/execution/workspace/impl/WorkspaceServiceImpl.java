package com.codexsphere.codearena.execution.workspace.impl;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.WorkspaceException;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.context.Workspace;
import com.codexsphere.codearena.execution.workspace.WorkspaceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
public class WorkspaceServiceImpl
        implements WorkspaceService {

    private final RunnerProperties runnerProperties;

    @Override
    public void createWorkspace(
            ExecutionContext context
    ) {

        try {

            Path root =
                    createRootDirectory();

            /*
             * Docker executes user code as a non-root user.
             *
             * The workspace is bind-mounted into the container
             * as /workspace, so the container user must be able
             * to read/write the workspace.
             */
            makeWorkspaceAccessible(root);

            Workspace workspace =
                    Workspace.builder()
                            .root(root)
                            .build();

            context.setWorkspace(workspace);

            log.info(
                    "Workspace created {}",
                    root
            );

        } catch (IOException e) {

            throw new WorkspaceException(
                    "Unable to create workspace",
                    e
            );
        }
    }

    /**
     * Creates the execution workspace root directory.
     *
     * Uses the configured root directory when present (required for
     * containerised docker-outside-of-docker deployments so sandbox
     * containers can bind-mount the workspace), otherwise falls back to
     * the system temporary directory.
     */
    private Path createRootDirectory()
            throws IOException {

        String rootDirectory =
                runnerProperties
                        .getWorkspace()
                        .getRootDirectory();

        if (rootDirectory != null
                && !rootDirectory.isBlank()) {

            Path root =
                    Path.of(
                            rootDirectory,
                            runnerProperties
                                    .getWorkspace()
                                    .getPrefix()
                                    + UUID.randomUUID()
                    );

            Files.createDirectories(root);

            return root;
        }

        return Files.createTempDirectory(
                runnerProperties
                        .getWorkspace()
                        .getPrefix()
        );
    }

    /**
     * Makes the workspace readable, writable and executable
     * for the owner/group/others.
     *
     * This is required because the workspace is bind-mounted
     * into a non-root Docker container.
     */
    private void makeWorkspaceAccessible(Path workspace) {

        try {

            Set<PosixFilePermission> permissions =
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE,

                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_WRITE,
                            PosixFilePermission.GROUP_EXECUTE,

                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE
                    );

            Files.setPosixFilePermissions(
                    workspace,
                    permissions
            );

        } catch (UnsupportedOperationException ex) {

            log.debug(
                    "POSIX permissions not supported for {}",
                    workspace
            );

        } catch (IOException ex) {

            throw new WorkspaceException(
                    "Unable to configure workspace permissions",
                    ex
            );
        }
    }

    @Override
    public void cleanupWorkspace(
            ExecutionContext context
    ) {

        if (context.getWorkspace() == null ||
                context.getWorkspace().getRoot() == null) {

            log.debug(
                    "No workspace available for cleanup."
            );

            return;
        }

        Path root =
                context.getWorkspace().getRoot();

        for (int attempt = 1; attempt <= 5; attempt++) {

            try (Stream<Path> paths =
                         Files.walk(root)) {

                paths.sorted(
                                Comparator.reverseOrder()
                        )
                        .forEach(path -> {

                            try {

                                Files.deleteIfExists(path);

                            } catch (IOException e) {

                                throw new WorkspaceException(
                                        "Exception in deleting existing file",
                                        e
                                );
                            }
                        });

                log.info(
                        "Workspace deleted: {}",
                        root
                );

                return;

            } catch (Exception ex) {

                log.warn(
                        "Cleanup attempt {} failed.",
                        attempt,
                        ex
                );

                try {

                    Thread.sleep(200);

                } catch (InterruptedException ignored) {

                    Thread.currentThread().interrupt();

                    return;
                }
            }
        }

        log.error(
                "Unable to cleanup workspace {}",
                root
        );
    }
}