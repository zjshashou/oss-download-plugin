package com.killer.oss;

import hudson.model.ParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

/**
 * @author killer
 * @create 2021/1/19 23:48
 * @since 1.0.0
 */
public class OSSParameterValue extends ParameterValue {

    private String remotePath;
    private String entries;
    private String packageVersion;

    @DataBoundConstructor
    public OSSParameterValue(String name, String remotePath, String entries, String packageVersion) {
        super(name);
        this.remotePath = remotePath;
        this.entries = entries;
        this.packageVersion = packageVersion;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getEntries() {
        return entries;
    }

    public void setEntries(String entries) {
        this.entries = entries;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    @Override
    public Object getValue() {
        return this.packageVersion;
    }
}
