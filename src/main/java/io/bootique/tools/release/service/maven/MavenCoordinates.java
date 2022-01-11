package io.bootique.tools.release.service.maven;

import org.w3c.dom.Node;


public class MavenCoordinates {

    private Node groupId;
    private Node artifactId;
    private Node version;

    public Node getGroupId() {
        return groupId;
    }

    public void setGroupId(Node groupId) {
        this.groupId = groupId;
    }

    public Node getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(Node artifactId) {
        this.artifactId = artifactId;
    }

    public Node getVersion() {
        return version;
    }

    public void setVersion(Node version) {
        this.version = version;
    }
}
