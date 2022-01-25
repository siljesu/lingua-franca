/*
 * generated by Xtext 2.25.0
 */
package org.lflang.ui.wizard;

import java.io.BufferedReader;
import java.util.List;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.stream.Collectors;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.util.PluginProjectFactory;
import org.eclipse.xtext.ui.wizard.template.IProjectGenerator;
import org.eclipse.xtext.ui.wizard.template.IProjectTemplateProvider;
import org.eclipse.xtext.ui.wizard.template.ProjectTemplate;
import org.eclipse.xtext.ui.wizard.template.AbstractProjectTemplate;

/**
 * Create a list with all project templates to be shown in the template new project wizard.
 * 
 * Each template is able to generate one or more projects. Each project can be configured such that any number of files are included.
 */
class LFProjectTemplateProvider implements IProjectTemplateProvider {
	@Override
	   public AbstractProjectTemplate[] getProjectTemplates() {
	    // TODO: new HelloWorldProject(), new InteractiveProject(), new WebServerProject(), new ReflexGameProject(), new ParallelProject(),
        return new AbstractProjectTemplate[] {new FederatedProject(), new PipelineProject()};
    }
		
	static String getTemplateFromFile(AbstractProjectTemplate tpl, String target, String fileName) {
        var stream = tpl.getClass().getResourceAsStream("templates/" + target + "/" + fileName);
        var str = "";
        if (stream != null) {
            str = new BufferedReader(
                new InputStreamReader(stream)
            ).lines().collect(Collectors.joining("\n"));
        } else {
            throw new RuntimeException("Unable to open template for '" + fileName + "'");
        }
        return str;
    }
}

@ProjectTemplate(label="Parallel", icon="project_template.png", description="<p><b>Parallel</b></p><p>A simple" + 
        " fork-join pattern that exploits parallelism.</p>")
final class PipelineProject extends AbstractProjectTemplate {

   @Override
   public void generateProjects(IProjectGenerator generator) {
        var proj = new PluginProjectFactory();
        proj.setProjectName(this.getProjectInfo().getProjectName());
        proj.setLocation(this.getProjectInfo().getLocationPath());
        proj.addProjectNatures(XtextProjectHelper.NATURE_ID);
        proj.addBuilderIds(XtextProjectHelper.BUILDER_ID);
        proj.addFolders(List.of("src"));
        var fileName = "src/Pipeline.lf";
        this.addFile(proj, fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "c", fileName));
        generator.generate(proj);
    }
}

@ProjectTemplate(label="Federated", icon="project_template.png", description="<p><b>Federated</b></p>" +
        "<p>A federated \"Hello World\" program.</p>")
final class FederatedProject extends AbstractProjectTemplate {
    
    @Override
    public void generateProjects(IProjectGenerator generator) {
        var proj = new PluginProjectFactory();
        proj.setProjectName(this.getProjectInfo().getProjectName());
        proj.setLocation(this.getProjectInfo().getLocationPath());
        proj.addProjectNatures(XtextProjectHelper.NATURE_ID);
        proj.addBuilderIds(XtextProjectHelper.BUILDER_ID);
        proj.addFolders(List.of("src"));
        var fileName = "src/FederatedHelloWorld.lf";
        this.addFile(proj, fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "c", fileName));
        generator.generate(proj);            
    }
}


//@ProjectTemplate(label="Parallel", icon="project_template.png", description="<p><b>Parallel</b></p>
//    <p>A simple fork-join pattern that exploits parallelism.</p>")
//    final class ParallelProject {
//        // val advanced = check("Advanced:", false)
//        //val config = group("Configuration")
//        //val target = combo("Target:", #["C"], "The target language to compile down to", config)
//
//
//        override generateProjects(IProjectGenerator generator) {
//            generator.generate(new PluginProjectFactory => [
//                projectName = projectInfo.projectName
//                location = projectInfo.locationPath
//                projectNatures += #[XtextProjectHelper.NATURE_ID]
//                builderIds += #[XtextProjectHelper.BUILDER_ID]
//                folders += #["src"]
//                val fileName = "src/Parallel.lf"
//                addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "c", fileName))
//            ])
//        }
//    
//    }
//
//
//@ProjectTemplate(label="Hello World", icon="project_template.png", description="<p><b>Hello World</b></p>
//<p>Print \"Hello world!\" in a target language of choice.</p>")
//final class HelloWorldProject {
//	//val advanced = check("Advanced:", false)
//	val config = group("Configuration")
//	val targets = #["C", "C++", "Python", "TypeScript"] // FIXME: use Target enum
//	val target = combo("Target:", targets, "The target language to compile down to", config)
//	//val path = text("Package:", "mydsl", "The package path to place the files in", advancedGroup)
//    //target.enabled = true
//    
////	override protected updateVariables() {
////		name.enabled = advanced.value
////		path.enabled = advanced.value
////		if (!advanced.value) {
////			name.value = "Xtext"
////			path.value = "lf"
////		}
////	}
//
////	override protected validate() {
////		if (path.value.matches('[a-z][a-z0-9_]*(/[a-z][a-z0-9_]*)*'))
////			null
////		else
////			new Status(ERROR, "Wizard", "'" + path + "' is not a valid package name")
////	}
//
//	override generateProjects(IProjectGenerator generator) {
//        generator.generate(new PluginProjectFactory => [
//            projectName = projectInfo.projectName
//            location = projectInfo.locationPath
//            projectNatures += #[XtextProjectHelper.NATURE_ID] // JavaCore.NATURE_ID, "org.eclipse.pde.PluginNature", 
//            builderIds += #[XtextProjectHelper.BUILDER_ID] // JavaCore.BUILDER_ID, 
//            folders += "src"
//            val fileName = "src/HelloWorld.lf"
//
//            switch (target.value) {
//                case "C++":
//                    addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "cpp", fileName))
//                case "C":
//                    addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "c", fileName))
//                case "Python":
//                    addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "py", fileName))
//                case "TypeScript":
//                    addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "ts", fileName))
//            }
//        ])
//    }
//}
//
//@ProjectTemplate(label="Interactive", icon="project_template.png", description="<p><b>Interactive</b></p>
//<p>Simulate sensor input through key strokes.</p>")
//final class InteractiveProject {
//    //val advanced = check("Advanced:", false)
//    val config = group("Configuration")
//    val target = combo("Target:", #["C"], "The target language to compile down to", config)
//    
//    override generateProjects(IProjectGenerator generator) {
//        generator.generate(new PluginProjectFactory => [
//            projectName = projectInfo.projectName
//            location = projectInfo.locationPath
//            projectNatures += #[XtextProjectHelper.NATURE_ID]
//            builderIds += #[XtextProjectHelper.BUILDER_ID] 
//            folders += #["src", "src/include"]
//            val fileName = "src/Interactive.lf"
//            addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "c", fileName))
//            val cmakeFile = "src/include/ncurses-cmake-extension.txt"
//            addFile(cmakeFile, LFProjectTemplateProvider.getTemplateFromFile(this, "c", cmakeFile))
//        ])
//    }
//}
//
//@ProjectTemplate(label="WebServer", icon="project_template.png", description="<p><b>Web Server</b></p>
//<p>A simple web server implemented using TypeScript.</p>")
//final class WebServerProject {
//    //val advanced = check("Advanced:", false)
//    val config = group("Configuration")
//    val target = combo("Target:", #["TypeScript"], "The target language to compile down to", config)
//    
//    override generateProjects(IProjectGenerator generator) {
//        generator.generate(new PluginProjectFactory => [
//            projectName = projectInfo.projectName
//            location = projectInfo.locationPath
//            projectNatures += #[XtextProjectHelper.NATURE_ID]
//            builderIds += #[XtextProjectHelper.BUILDER_ID] 
//            folders += #["src"]
//            val fileName = "src/WebServer.lf"
//            addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "ts", fileName))
//        ])
//    }
//}
//
//@ProjectTemplate(label="ReflexGame", icon="project_template.png", description="<p><b>ReflexGame</b></p>
//<p>A simple reflex game.</p>")
//final class ReflexGameProject {
//    //val advanced = check("Advanced:", false)
//    val config = group("Configuration")
//    val target = combo("Target:", #["C", "Python"], "The target language to compile down to", config)
//    
//    override generateProjects(IProjectGenerator generator) {
//        generator.generate(new PluginProjectFactory => [
//            projectName = projectInfo.projectName
//            location = projectInfo.locationPath
//            projectNatures += #[XtextProjectHelper.NATURE_ID]
//            builderIds += #[XtextProjectHelper.BUILDER_ID] 
//            folders += #["src"]
//            val fileName = "src/ReflexGame.lf"
//            if (target.value.equals("C")) {
//                addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "c", fileName))
//            } else if (target.value.equals("Python")) {
//                addFile(fileName, LFProjectTemplateProvider.getTemplateFromFile(this, "py", fileName))
//                addFile("src/gui.py", LFProjectTemplateProvider.getTemplateFromFile(this, "py", "src/gui.py"))
//            }            
//        ])
//    
//    }
//        
//}
//
//
