package io.bootique.tools.release.service.maven;

import io.bootique.tools.release.model.maven.persistent.Module;
import io.bootique.tools.release.model.maven.persistent.Project;
import io.bootique.tools.release.model.maven.persistent.auto._Module;
import io.bootique.tools.release.model.maven.persistent.auto._Project;
import io.bootique.tools.release.model.persistent.Repository;
import io.bootique.tools.release.service.git.GitService;
import io.bootique.tools.release.service.preferences.PreferenceService;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NewDefaultMavenService implements NewMavenService {

    private final PreferenceService preferences;
    private final Pattern dependencyPattern;
    private static final String POM_XML = "pom.xml";

    @Inject
    GitService gitService;

    @Inject
    public NewDefaultMavenService(PreferenceService preferences) {
        this.preferences = preferences;
        this.dependencyPattern = Pattern.compile(this.preferences.get(MavenService.GROUP_ID_PATTERN) + ".*$");
    }


    @Override
    public boolean isMavenProject(Repository repository) {
        Path path = preferences.get(GitService.BASE_PATH_PREFERENCE);
        return Files.exists(path.resolve(repository.getName()).resolve(POM_XML));
    }


    @Override
    public Project createOrUpdateProject(Repository repository) {

        ObjectContext context = repository.getObjectContext();
        Project project = getProject(repository, context);
        Path projectPath = getProjectPath(repository);
        Module rootModule = createOrUpdateModule(projectPath, context, project);

        project.setRepository(repository);
        project.setPath(projectPath);
        project.setRootModule(rootModule);
        addModulesToProject(context, project, projectPath, rootModule);
        project.setBranchName(gitService.getCurrentBranchName(repository));
        return project;
    }

    private void addModulesToProject(ObjectContext context, Project project, Path projectPath, Module rootModule) {
        List<Module> modules = getModules(rootModule, projectPath, context, project);
        for (Module module : modules) {
            project.addToModules(module);
        }
    }

    private Path getProjectPath(Repository repository) {
        Path basePath = preferences.get(GitService.BASE_PATH_PREFERENCE);
        return basePath.resolve(repository.getName());
    }

    private Project getProject(Repository repository, ObjectContext context) {
        Project project = ObjectSelect.query(Project.class).where(_Project.REPOSITORY.eq(repository)).selectFirst(context);
        if (project == null) {
            project = context.newObject(Project.class);
        }
        return project;
    }

    private MavenCoordinates getMavenCoordinates(Path path) {
        MavenCoordinates coordinates = new MavenCoordinates();
        try {
            Document document = readDocument(path.resolve(POM_XML).toUri().toURL());
            XPath xpath = XPathFactory.newInstance().newXPath();

            Node groupId = (Node) xpath.evaluate("/project/groupId", document, XPathConstants.NODE);
            Node artifactId = (Node) xpath.evaluate("/project/artifactId", document, XPathConstants.NODE);
            Node version = (Node) xpath.evaluate("/project/version", document, XPathConstants.NODE);
            if (groupId == null || version == null) {
                groupId = (Node) xpath.evaluate("/project/parent/groupId", document, XPathConstants.NODE);
                version = (Node) xpath.evaluate("/project/parent/version", document, XPathConstants.NODE);
            }
            coordinates.setGroupId(groupId.getTextContent());
            coordinates.setArtifactId(artifactId.getTextContent());
            coordinates.setVersion(version.getTextContent());
        } catch (XPathExpressionException | MalformedURLException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    List<Module> getModules(Module rootModule, Path path, ObjectContext context, Project project) {
        List<Module> moduleList = new ArrayList<>();
        try {
            Document document = readDocument(path.resolve(POM_XML).toUri().toURL());
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList modules = (NodeList) xpath.evaluate("/project/modules/module", document, XPathConstants.NODESET);
            for (int i = 0; i < modules.getLength(); i++) {
                Path currPath = path.resolve(modules.item(i).getTextContent());
                Module currModule = createOrUpdateModule(currPath, context, project);
                moduleList.addAll(getModules(currModule, currPath, context, project));
            }
        } catch (MalformedURLException | XPathExpressionException ex) {
            throw new RuntimeException("Invalid path " + path, ex);
        }
        moduleList.add(rootModule);
        return moduleList;
    }

    /**
     * @param path to pom.xml
     * @return root module description
     */
    private Module createOrUpdateModule(Path path, ObjectContext context, Project project) {
        MavenCoordinates mavenCoordinates = getMavenCoordinates(path);
        Module module = ObjectSelect.query(Module.class)
                .where(_Module.GROUP_STR.eq(mavenCoordinates.getGroupId()))
                .and(_Module.GITHUB_ID.eq(mavenCoordinates.getArtifactId()))
                .selectFirst(context);
        if (module == null) {
            module = context.newObject(Module.class);
        }
        module.setGithubId(mavenCoordinates.getArtifactId());
        module.setGroupStr(mavenCoordinates.getGroupId());
        module.setVersion(mavenCoordinates.getVersion());
        module.setProject(project);
        return module;
    }

    /*
    private void setDependencies(Path path, Project project, ObjectContext context) {
        try {
            Document document = readDocument(path.resolve(POM_XML).toUri().toURL());
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList dependencies = (NodeList) xpath.evaluate("/project/dependencies/dependency", document, XPathConstants.NODESET);
            List<Project> projects = ObjectSelect.query(Project.class).select(context);
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element element = (Element) dependencies.item(i);
                String groupId = element.getElementsByTagName("groupId").item(0).getTextContent();
                for (Project project1 : projects) {
                    Module rootModule = project1.getRootModule();
                    if (rootModule.getGroupStr().equals(groupId)){
                        project.addToDependencies(project1);
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Invalid path " + path, ex);
        }
    }
    */

    /*
    private ModuleDependency createOrUpdateDependency() {
        Module module = ObjectSelect.query(ModuleDependency.class)
                .where(ModuleDependency.MODULE.eq(mavenCoordinates.getGroupId().getTextContent()))
                .and(_Module.GITHUB_ID.eq(mavenCoordinates.getArtifactId().getTextContent()))
                .selectFirst(context);
        if (module == null) {
            module = context.newObject(Module.class);
        }
        module.setGithubId(mavenCoordinates.getArtifactId().getTextContent());
        module.setGroupStr(mavenCoordinates.getGroupId().getTextContent());
        module.setVersion(mavenCoordinates.getVersion().getTextContent());
        return module;
        return null;
    }
*/


    @Override
    public List<Project> sortProjects(List<Project> projects) {
        return null;
    }

    private static Document readDocument(URL url) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        try {
            DocumentBuilder domBuilder = documentBuilderFactory.newDocumentBuilder();
            try (InputStream inputStream = url.openStream()) {
                return domBuilder.parse(inputStream);
            } catch (IOException | SAXException e) {
                throw new RuntimeException("Error loading configuration from " + url, e);
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
