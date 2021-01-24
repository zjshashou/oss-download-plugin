package com.killer.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author killer
 * @create 2021/1/23 00:30
 * @since 1.0.0
 */
public class OSSDownloadPublisher extends Notifier implements SimpleBuildStep {

    private String parameterVar;

    private String localPath;

    private String maxRetries;

    private String remoteFile;

    @DataBoundConstructor
    public OSSDownloadPublisher(String parameterVar, String localPath, String maxRetries, String remoteFile) {
        this.parameterVar = parameterVar;
        this.localPath = localPath;
        this.maxRetries = maxRetries;
        this.remoteFile = remoteFile;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getParameterVar() {
        return parameterVar;
    }

    public void setParameterVar(String parameterVar) {
        this.parameterVar = parameterVar;
    }

    public int getMaxRetries() {
        return StringUtils.isEmpty(maxRetries) ? 3 : Integer.valueOf(maxRetries);
    }

    public void setMaxRetries(String maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getRemoteFile() {
        return remoteFile;
    }

    public void setRemoteFile(String remoteFile) {
        this.remoteFile = remoteFile;
    }

    @Override
    public void perform(Run<?, ?> run,
                        FilePath workspace,
                        Launcher launcher,
                        TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();

        String remotePath = getRemotePath(run.getActions(ParametersAction.class));

        if (StringUtils.isEmpty(remotePath) && !StringUtils.isEmpty(remoteFile)) {
            remotePath = remoteFile;
        }

        if (StringUtils.isEmpty(remotePath)) {
            throw new InterruptedException("Remote Path(Key) cannot empty.");
        }
        logger.println("Begin download remote path:" + remotePath);

        OSSConfiguration configuration = OSSConfiguration.get();

        OSSClient client = null;
        try {
            client = new OSSClient(configuration.getEndpoint(),
                    configuration.getAccessKeyId(),
                    configuration.getAccessKeySecret());

            GetObjectRequest getObjectRequest = new GetObjectRequest(configuration.getBucketName(),
                    remotePath);

            String tempLocalPath = localPath;
            EnvVars environment = run.getEnvironment(listener);
            tempLocalPath = environment.expand(tempLocalPath);

            File file = getTempFile(remotePath, tempLocalPath);

            logger.println("local path:" + file.getAbsolutePath());

            int maxRetries = getMaxRetries();
            int retries = 0;
            do {
                if (retries > 0) {
                    logger.println("upload retrying (" + retries + "/" + maxRetries + ")");
                }
                try {
                    client.getObject(getObjectRequest, file);

                    logger.println("local path download complete:" + file.getAbsolutePath());

                    return;
                } catch (Exception e) {
                    e.printStackTrace(logger);
                }
            } while ((++retries) <= maxRetries);

            throw new RuntimeException("Download fail, more than the max of retries");

        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    private File getTempFile(String remotePath, String tempLocalPath) throws IOException {
        File file = getFileName(tempLocalPath, remotePath);
        if (file.exists()) {
            file.delete();
        }
        IOUtils.mkdirs(file.getParentFile());
        return file;
    }

    private String getRemotePath(List<ParametersAction> actions) {
        if (CollectionUtils.isEmpty(actions)) return null;

        for (ParametersAction action : actions) {
            ParameterValue parameter = action.getParameter(parameterVar);
            if (parameter == null) continue;
            Object value = parameter.getValue();
            if (value == null) continue;
            return value.toString();
        }
        return null;
    }

    private File getFileName(String localPath, String remotePath) {

        int i = localPath.lastIndexOf("/");
        String temp = localPath;
        if (i > -1) {
            temp = localPath.substring(i);
        }
        if (temp.indexOf(".") > -1) return new File(localPath);

        i = remotePath.lastIndexOf(".");
        String ext = remotePath;
        if (i > -1) {
            ext = remotePath.substring(i);
        }
        localPath = localPath + ext;

        return new File(localPath);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Symbol("ossDownload")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            return super.configure(req, json);
        }

        @Override
        public String getDisplayName() {
            return "OSS Download";
        }

        public FormValidation doCheckLocalPath(@QueryParameter("localPath") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the local path");
            return FormValidation.ok();
        }
        public FormValidation doCheckParameterVar(@QueryParameter("parameterVar") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the parameter name");

            return FormValidation.ok();
        }

        public FormValidation doCheckEntries(@QueryParameter("entries") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the max retries");

            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                return FormValidation.error("Max retries is integer");
            }

            return FormValidation.ok();
        }

    }
}
