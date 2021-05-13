package io.bootique.tools.release.model.maven.persistent.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;

import io.bootique.tools.release.model.maven.persistent.ModuleDependency;
import io.bootique.tools.release.model.maven.persistent.Project;

/**
 * Class _Module was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Module extends BaseDataObject {

    private static final long serialVersionUID = 1L;

    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> GITHUB_ID = PropertyFactory.createString("githubId", String.class);
    public static final StringProperty<String> GROUP_STR = PropertyFactory.createString("groupStr", String.class);
    public static final StringProperty<String> VERSION = PropertyFactory.createString("version", String.class);
    public static final ListProperty<ModuleDependency> DEPENDENCIES = PropertyFactory.createList("dependencies", ModuleDependency.class);
    public static final EntityProperty<Project> PROJECT = PropertyFactory.createEntity("project", Project.class);

    protected String githubId;
    protected String groupStr;
    protected String version;

    protected Object dependencies;
    protected Object project;

    public void setGithubId(String githubId) {
        beforePropertyWrite("githubId", this.githubId, githubId);
        this.githubId = githubId;
    }

    public String getGithubId() {
        beforePropertyRead("githubId");
        return this.githubId;
    }

    public void setGroupStr(String groupStr) {
        beforePropertyWrite("groupStr", this.groupStr, groupStr);
        this.groupStr = groupStr;
    }

    public String getGroupStr() {
        beforePropertyRead("groupStr");
        return this.groupStr;
    }

    public void setVersion(String version) {
        beforePropertyWrite("version", this.version, version);
        this.version = version;
    }

    public String getVersion() {
        beforePropertyRead("version");
        return this.version;
    }

    public void addToDependencies(ModuleDependency obj) {
        addToManyTarget("dependencies", obj, true);
    }

    public void removeFromDependencies(ModuleDependency obj) {
        removeToManyTarget("dependencies", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<ModuleDependency> getDependencies() {
        return (List<ModuleDependency>)readProperty("dependencies");
    }

    public void setProject(Project project) {
        setToOneTarget("project", project, true);
    }

    public Project getProject() {
        return (Project)readProperty("project");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "githubId":
                return this.githubId;
            case "groupStr":
                return this.groupStr;
            case "version":
                return this.version;
            case "dependencies":
                return this.dependencies;
            case "project":
                return this.project;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "githubId":
                this.githubId = (String)val;
                break;
            case "groupStr":
                this.groupStr = (String)val;
                break;
            case "version":
                this.version = (String)val;
                break;
            case "dependencies":
                this.dependencies = val;
                break;
            case "project":
                this.project = val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.githubId);
        out.writeObject(this.groupStr);
        out.writeObject(this.version);
        out.writeObject(this.dependencies);
        out.writeObject(this.project);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.githubId = (String)in.readObject();
        this.groupStr = (String)in.readObject();
        this.version = (String)in.readObject();
        this.dependencies = in.readObject();
        this.project = in.readObject();
    }

}
