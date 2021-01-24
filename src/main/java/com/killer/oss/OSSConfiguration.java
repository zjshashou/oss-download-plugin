package com.killer.oss;

import com.aliyun.oss.OSSClient;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author killer
 * @create 2021/1/22 10:24
 * @since 1.0.0
 */
@Extension
public class OSSConfiguration extends GlobalConfiguration {

    private String endpoint;

    private String accessKeyId;

    private Secret accessKeySecret;

    private String bucketName;

    public static @CheckForNull OSSConfiguration get() {
        return GlobalConfiguration.all().get(OSSConfiguration.class);
    }

    public OSSConfiguration() {
        load();
    }


    public @Nonnull String getEndpoint() {
        String endpoint = this.endpoint;
        if (endpoint == null) {
            return ".aliyuncs.com";
        }
        return endpoint;
    }

    public void setEndpoint(@CheckForNull String endpoint) {
        this.endpoint = endpoint;
        save();
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    @DataBoundSetter
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        save();
    }

    public String getAccessKeySecret() {
        if (accessKeySecret == null) return null;
        return accessKeySecret.getPlainText();
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = Secret.fromString(accessKeySecret);
        save();
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
        save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        return true;
    }

    public FormValidation doCheckEndpoint(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please enter Endpoint.");
        }
        return FormValidation.ok();
    }
    public FormValidation doCheckAccessKeyId(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please enter AccessKeyId.");
        }
        return FormValidation.ok();
    }
    public FormValidation doCheckAccessKeySecret(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please enter AccessKeySecret.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckBucketName(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please enter BucketName.");
        }
        return FormValidation.ok();
    }
    public FormValidation doValidateOssConnection(@QueryParameter("endpoint") final String endpoint,
                                                  @QueryParameter("accessKeyId") final String accessKeyId,
                                                  @QueryParameter("accessKeySecret") final String accessKeySecret,
                                                  @QueryParameter("bucketName") final String bucketName
                                                  ) {
        try {
            Secret secret = Secret.fromString(accessKeySecret);
            OSSClient client = new OSSClient(endpoint, accessKeyId, secret.getPlainText());
            if (client.doesBucketExist(bucketName)) {
                return FormValidation.ok("Success");
            } else {
                FormValidation.error("Validate Error: BucketName not exists");
            }
            client.shutdown();
            return FormValidation.ok("Success");
        } catch (Throwable e) {
            return FormValidation.error("Validate Error:" + e.getMessage());
        }
    }


}
