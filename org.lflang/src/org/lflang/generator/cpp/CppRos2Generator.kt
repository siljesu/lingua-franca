package org.lflang.generator.cpp

import org.lflang.generator.JavaGeneratorUtils
import org.lflang.generator.LFGeneratorContext
import java.nio.file.Path

class CppRos2Generator(generator: CppGenerator) : CppPlatformGenerator(generator) {

    override val srcGenPath: Path = generator.cppFileConfig.srcGenPath.resolve("src")
    private val packagePath: Path = generator.cppFileConfig.srcGenPath

    override fun generatePlatformFiles() {
        val packageXml = CppRos2PackageGenerator(generator).generatePackageXml()
        JavaGeneratorUtils.writeToFile(packageXml, packagePath.resolve("package.xml"))

        val cmake = CppRos2CmakeGenerator(generator).generateCode(generator.cppSources)
        JavaGeneratorUtils.writeToFile(cmake, packagePath.resolve("CMakeLists.txt"))
    }

    override fun doCompile(context: LFGeneratorContext, onlyGenerateBuildFiles: Boolean): Boolean {
        TODO("Not yet implemented")
    }
}