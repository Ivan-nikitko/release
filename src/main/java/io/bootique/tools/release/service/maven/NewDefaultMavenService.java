package io.bootique.tools.release.service.maven;

import io.bootique.tools.release.model.maven.persistent.Module;
import io.bootique.tools.release.model.maven.persistent.Project;
import io.bootique.tools.release.model.maven.persistent.auto._Module;
import io.bootique.tools.release.model.persistent.Repository;
import io.bootique.tools.release.service.git.GitService;
import io.bootique.tools.release.service.preferences.PreferenceService;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
import java.util.List;

public class NewDefaultMavenService implements NewMavenService {

    private final PreferenceService preferences;

    @Inject
    GitService gitService;

    @Inject
    public NewDefaultMavenService(PreferenceService preferences) {
        this.preferences = preferences;
    }


    @Override
    public boolean isMavenProject(Repository repository) {
        Path path = preferences.get(GitService.BASE_PATH_PREFERENCE);
        return Files.exists(path.resolve(repository.getName()).resolve("pom.xml"));
    }

    @Override
    public Project createProject(Repository repository) {
        return null;
    }

    @Override
    public Project createOrUpdateProject( Repository repository) {

        ObjectContext context = repository.getObjectContext();
        Path basePath = preferences.get(GitService.BASE_PATH_PREFERENCE);
        Path projectPath = basePath.resolve(repository.getName());

        Project project = ObjectSelect.query(Project.class).where(Project.REPOSITORY.eq(repository)).selectFirst(context);
        if (project == null) {
            project = context.newObject(Project.class);
        }
        List<Module> modules = project.getModules(); //TODO del
        project.setRepository(repository);
        project.setPath(projectPath);
        Module rootModule = resolveModule(projectPath, context);
        rootModule.setProject(project);
        project.setRootModule(rootModule);
        project.setBranchName(gitService.getCurrentBranchName(repository));
        return project;
    }

    private MavenCoordinates getMavenCoordinates(Path path) {
        MavenCoordinates coordinates = new MavenCoordinates();
        try {
            Document document = readDocument(path.resolve("pom.xml").toUri().toURL());
            XPath xpath = XPathFactory.newInstance().newXPath();

            Node groupId = (Node) xpath.evaluate("/project/groupId", document, XPathConstants.NODE);
            Node artifactId = (Node) xpath.evaluate("/project/artifactId", document, XPathConstants.NODE);
            Node version = (Node) xpath.evaluate("/project/version", document, XPathConstants.NODE);
            if (groupId == null || version == null) {
                groupId = (Node) xpath.evaluate("/project/parent/groupId", document, XPathConstants.NODE);
                version = (Node) xpath.evaluate("/project/parent/version", document, XPathConstants.NODE);
            }
            coordinates.setGroupId(groupId);
            coordinates.setArtifactId(artifactId);
            coordinates.setVersion(version);
        } catch (XPathExpressionException | MalformedURLException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    /**
     * Check that path + ".git" directory exists
     *
     * @param path to pom.xml
     * @return root module description
     */
    Module resolveModule(Path path,ObjectContext context) {

            MavenCoordinates mavenCoordinates = getMavenCoordinates(path);

            Module module = ObjectSelect.query(Module.class)
                    .where(_Module.GROUP_STR.eq(mavenCoordinates.getGroupId().getTextContent()))
                    .and(_Module.GITHUB_ID.eq(mavenCoordinates.getArtifactId().getTextContent()))
                    .selectFirst(context);
            if (module==null){
                module = context.newObject(Module.class);
            }
            module.setGithubId(mavenCoordinates.getArtifactId().getTextContent());
            module.setGroupStr(mavenCoordinates.getGroupId().getTextContent());
            module.setVersion(mavenCoordinates.getVersion().getTextContent());
            return module;
    }

    @Override
    public Project updateProject(Project project, Repository repository) {
        return null;
    }

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
