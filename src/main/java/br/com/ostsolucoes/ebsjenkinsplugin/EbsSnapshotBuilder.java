package br.com.ostsolucoes.ebsjenkinsplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.InstanceState;

import javax.servlet.ServletException;
import java.io.IOException;
/**
 *
 * @author Pedro
 */
public class EbsSnapshotBuilder extends Builder{
    
    private final String volumeId;
    private final String snapshotName;
    private final String snapshotDesc;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public EbsSnapshotBuilder(String volumeId, String snapshotName, String snapshotDesc) {
        this.volumeId = volumeId;
        this.snapshotName = snapshotName;
        this.snapshotDesc = snapshotDesc;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getVolumeId() {
        return volumeId;
    }

    public String getSnapshotName() {
        return snapshotName;
    }
        
    public String getSnapshotDesc() {
        return snapshotDesc;
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String awsSecretKey = getDescriptor().getAwsSecretKey();
        String awsAccessKey = getDescriptor().getAwsAccessKey();
        
        
        if (awsSecretKey == "") 
        {
            listener.getLogger().println("Configure um AWS Secret Key."); 
            return false;
        }
        
        if (awsAccessKey == "") 
        {
            listener.getLogger().println("Configure um AWS Access Key."); 
            return false;
        }
        
        // This also shows how you can consult the global configuration of the builder
        listener.getLogger().println("VolumeId = "+volumeId);
        listener.getLogger().println("Snapshot Name = "+snapshotName);
        listener.getLogger().println("Snapshot Description = "+snapshotDesc);
        
        AWSCredentials credentials = new BasicAWSCredentials(awsSecretKey, awsAccessKey);
        
        AmazonEC2 ec2 = new AmazonEC2Client(credentials);
	Region saEast1 = Region.getRegion(Regions.SA_EAST_1);
	ec2.setRegion(saEast1);
        
        DescribeInstancesResult result = ec2.describeInstances();
        
        for(Reservation reservation: result.getReservations())
        {
            listener.getLogger().println("Reservation ID: " + reservation.getReservationId());
            for(Instance instance: reservation.getInstances())
            {
                listener.getLogger().println("\tInstance ID: " + instance.getInstanceId());
                listener.getLogger().println("\tInstance Type: " + instance.getInstanceType());
                listener.getLogger().println("\tInstance State: " + instance.getState().getName());
                listener.getLogger().println("\tInstance Tags:");
                for(Tag tag : instance.getTags())
                {
                    listener.getLogger().println("\t\tKey: " + tag.getKey() + " - Value: " + tag.getValue());                    
                }
            }
        }
      
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String awsSecretKey;
        private String awsAccessKey;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckVolumeId(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Volume Id");
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckSnapshotName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Snapshot Name");
            
            return FormValidation.ok();
        }
                
        public FormValidation doCheckSnapshotDesc(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Snapshot Description");
            
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "EBS Snapshot Builder";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            awsSecretKey = formData.getString("awsSecretKey");
            awsAccessKey = formData.getString("awsAccessKey");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public String getAwsSecretKey() {
            return awsSecretKey;
        }
        public String getAwsAccessKey() {
            return awsAccessKey;
        }
    }
}
