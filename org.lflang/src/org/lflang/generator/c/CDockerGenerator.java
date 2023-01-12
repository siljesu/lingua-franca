package org.lflang.generator.c;

import java.util.stream.Collectors;

import org.eclipse.xtext.xbase.lib.IterableExtensions;

import org.lflang.Target;
import org.lflang.generator.DockerGeneratorBase;
import org.lflang.generator.LFGeneratorContext;
import org.lflang.util.StringUtil;

/**
 * Generate the docker file related code for the C and CCpp target.
 *
 * @author Hou Seng Wong
 */
public class CDockerGenerator extends DockerGeneratorBase {
    private static final String DEFAULT_BASE_IMAGE = "alpine:latest";

    /**
     * The constructor for the base docker file generation class.
     *
     * @param context The context of the code generator.
     */
    public CDockerGenerator(LFGeneratorContext context) {
        super(context);
    }

    /**
     * Translate data from the code generator to docker data as
     * specified in the DockerData class.
     *
     * @return docker data as specified in the DockerData class
     */
    @Override
    protected DockerData generateDockerData() {

        var fileConfig = context.getFileConfig();
        var lfModuleName = fileConfig.name;
        var dockerFilePath = fileConfig.getSrcGenPath().resolve(lfModuleName + ".Dockerfile");
        var dockerFileContent = generateDockerFileContent();
        var dockerBuildContext = "."; // FIXME: if federated, use federateName
        return new DockerData(dockerFilePath, dockerFileContent,dockerBuildContext);
    }

    /**
     * Generate the contents of the docker file.
     */
    protected String generateDockerFileContent() {
        var lfModuleName = context.getFileConfig().name;
        var config = context.getTargetConfig();
        var compileCommand = IterableExtensions.isNullOrEmpty(config.buildCommands) ?
                                 generateDefaultCompileCommand() :
                                 StringUtil.joinObjects(config.buildCommands, " ");
        var compiler = config.target == Target.CPP ? "g++" : "gcc";
        var baseImage = config.dockerOptions.from == null ? DEFAULT_BASE_IMAGE : config.dockerOptions.from;
        return String.join("\n",
            "# For instructions, see: https://www.lf-lang.org/docs/handbook/containerized-execution",
            "FROM "+baseImage+" AS builder",
            "WORKDIR /lingua-franca/"+lfModuleName,
            "RUN set -ex && apk add --no-cache "+compiler+" musl-dev cmake make",
            "COPY . src-gen",
            compileCommand,
            "",
            "FROM "+baseImage,
            "WORKDIR /lingua-franca",
            "RUN mkdir bin",
            "COPY --from=builder /lingua-franca/"+lfModuleName+"/bin/"+lfModuleName+" ./bin/"+lfModuleName,
            "",
            "# Use ENTRYPOINT not CMD so that command-line arguments go through",
            "ENTRYPOINT [\"./bin/"+lfModuleName+"\"]",
            ""
        );
    }

    /** Return the default compile command for the C docker container. */
    protected String generateDefaultCompileCommand() {
        return String.join("\n",
            "RUN set -ex && \\",
            "mkdir bin && \\",
            "cmake " + CCompiler.cmakeCompileDefinitions(context.getTargetConfig())
                .collect(Collectors.joining(" "))
                + " -S src-gen -B bin && \\",
            "cd bin && \\",
            "make all"
        );
    }
}
