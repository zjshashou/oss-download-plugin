package com.killer.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author killer
 * @create 2021/1/19 23:24
 * @since 1.0.0
 */
public class OSSParameterDefinition extends ParameterDefinition {

    private String remotePath;
    private String entries;

    @DataBoundConstructor
    public OSSParameterDefinition(String name, String description, String remotePath, String entries) {
        super(name, description);
        this.remotePath = remotePath;
        this.entries = entries;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        OSSParameterValue value = (OSSParameterValue)req.bindJSON(OSSParameterValue.class, jo);
        return value;
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

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] values = req.getParameterValues(this.getName());
        if (values != null && values.length == 3) {
            return new OSSParameterValue(this.getName(), values[0], values[1], values[2]);
        }
        return new OSSParameterValue(this.getName(), this.remotePath, this.entries, null);
    }

    @Symbol("ossParameter")
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return "OSS Parameters";
        }


        public FormValidation doCheckRemotePath(@QueryParameter("remotePath") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the remote path.");

            return FormValidation.ok();
        }
        public FormValidation doCheckEntries(@QueryParameter("entries") Integer value)
                throws IOException, ServletException {
            if (value == null)
                return FormValidation.error("Please set the max count.");
            if (value > 200)
                return FormValidation.error("Max value is 200.");

            return FormValidation.ok();
        }

        public ListBoxModel doFillPackageVersionItems(@AncestorInPath Job job,
                                                      @QueryParameter("name") String name,
                                                      @QueryParameter("remotePath") String remotePath,
                                                      @QueryParameter("entries") String entries
        ) {
            ListBoxModel items = new ListBoxModel();

            OSSConfiguration ossConfiguration = OSSConfiguration.get();

            OSSClient client = new OSSClient(ossConfiguration.getEndpoint(),
                    ossConfiguration.getAccessKeyId(),
                    ossConfiguration.getAccessKeySecret());

            ListObjectsRequest listRequest = new ListObjectsRequest();
            listRequest.setPrefix(remotePath);
            listRequest.setMaxKeys(Integer.valueOf(entries));
            listRequest.setBucketName(ossConfiguration.getBucketName());

            ObjectListing objectListing = client.listObjects(listRequest);
            if (objectListing != null) {
                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    items.add(objectSummary.getKey(), objectSummary.getKey());
                }
            } else {
                items.add("No Data", "No");
            }

            client.shutdown();

            return items;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            return super.configure(req, json);
        }
    }
}
