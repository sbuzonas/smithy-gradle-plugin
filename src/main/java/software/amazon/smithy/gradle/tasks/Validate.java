/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.gradle.tasks;

import java.util.List;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import software.amazon.smithy.gradle.SmithyUtils;

/**
 * Validates the Smithy models found through model discovery.
 *
 * <p>The validation task will execute the Smithy CLI in a new process
 * to ensure that it uses an explicit classpath that ensures that the
 * generated JAR works correctly when used alongside its dependencies.
 *
 * <p>The CLI version used to validate the generated JAR is picked by
 * searching for smithy-model in the runtime dependencies. If found,
 * the same version of the CLI is used. If not found, a default version
 * is used.
 */
public class Validate extends SmithyTask {

    @TaskAction
    public void execute() {
        List<String> args = createCliArguments("validate", getModelDiscoveryClasspath());
        addModelArguments(args);
        getProject().getLogger().debug("Executing Smithy validation with args: " + String.join(" ", args));
        executeCliProcess(resolveValidateClasspath(), args);
    }

    private FileCollection resolveValidateClasspath() {
        FileCollection resolved = getClasspath() != null
                ? getClasspath()
                : SmithyUtils.getClasspath(getProject(), "runtimeClasspath");
        return resolved.plus(SmithyUtils.getSmithyCliClasspath(getProject()));
    }
}
