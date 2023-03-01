package com.jcohy.docs.build;

import java.io.File;

import com.jcohy.oss.OssUploadPlugin;
import com.jcohy.oss.OssUploadTask;
import com.jcohy.oss.dsl.AliOssExtension;
import org.asciidoctor.gradle.jvm.AbstractAsciidoctorTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.Zip;

import org.springframework.util.Assert;

/**
 * 描述: .
 *
 * <p>
 * Copyright © 2023 <a href="https://www.jcohy.com" target= "_blank">https://www.jcohy.com</a>
 *
 * @author jiac
 * @version 1.0.0 2023/2/22 11:52
 * @since 1.0.0
 */
public class ArchivesPlugins implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Assert.isTrue(project == project.getRootProject(),"此插件必须定义在根项目中");
        project.getPlugins().apply(OssUploadPlugin.class);
        createAggregatedAsciidoctorTask(project);
        createDocsZipTask(project);
        configureAliyunUpload(project);
    }

    private void configureAliyunUpload(Project project) {
        project.getTasks().withType(OssUploadTask.class, (ossUploadTask) -> {
            ossUploadTask.dependsOn("docsZip");
        });

        AliOssExtension extension = project.getExtensions().getByType(AliOssExtension.class);
        extension.setBucket("jcohy-test");
        String buildDir = project.getBuildDir().getName();
        extension.getUpload().setSource(buildDir + "/reference");
        extension.setAccessKey(System.getenv("OSS_ACCESS_KEY"));
        extension.setSecretKey(System.getenv("OSS_SECRET_KEY"));
        extension.getUpload().setPrefix("/docs");
        extension.getUpload().setIgnoreSourceDir(true);
    }

    private void createDocsZipTask(Project project) {
        project.getTasks().create("docsZip", Zip.class,(zip -> {
            project.afterEvaluate((p) -> {
                zip.dependsOn(p.getTasks().getByName("aggregatedAsciidoctor"));
                zip.setDuplicatesStrategy(DuplicatesStrategy.FAIL);
                zip.getDestinationDirectory().set(p.getLayout().getBuildDirectory().dir("archive"));
                zip.getArchiveFileName().set(p.getName() + "-" + p.getVersion() + ".zip");
                zip.from(p.getBuildDir() + "/reference");
            });
        }));

    }

    private void createAggregatedAsciidoctorTask(Project project) {
        project.getTasks().create("aggregatedAsciidoctor", Copy.class,(sync) -> {
            sync.setDestinationDir(new File(project.getBuildDir().getPath() + "/reference"));
            project.afterEvaluate((p) -> {
                project.subprojects((sub) -> {
                    sync.dependsOn(sub.getTasks().withType(AbstractAsciidoctorTask.class));
                    sync.from(sub.getBuildDir() + "/docs/asciidocMultipage",(spec) -> {
                        spec.into(sub.getName() + "/" + sub.getVersion() + "/html5");
                    });
                    sync.from(sub.getBuildDir() + "/docs/asciidoc",(spec) -> {
                        spec.into(sub.getName() + "/" + sub.getVersion() + "/htmlsingle");
                    });
                    sync.from(sub.getBuildDir() + "/docs/asciidocPdf",(spec) -> {
                        spec.into(sub.getName() + "/" + sub.getVersion() + "/pdf");
                    });
                });
            });
        });
    }
}
