package org.jenkinsci.plugins.configfiles.folder;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigByNameComparator;
import org.jenkinsci.plugins.configfiles.ConfigComparator;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.ConfigProviderComparator;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;


public class FolderConfigFileProperty extends AbstractFolderProperty<AbstractFolder<?>> implements ConfigFileStore {

    private static Comparator<Config> COMPARATOR = new ConfigComparator();

    private static ConfigProviderComparator CONFIGPROVIDER_COMPARATOR = new ConfigProviderComparator();

    private Collection<Config> configs = new TreeSet<>(COMPARATOR);

    private transient AbstractFolder<?> owner;

    /*package*/ FolderConfigFileProperty(AbstractFolder<?> owner) {
        setOwner(owner);
    }

    @Override
    public Collection<Config> getConfigs() {
        return configs;
    }

    @Override
    public Collection<Config> getConfigs(Class<? extends Descriptor> descriptor) {
        List<Config> cs = new ArrayList<Config>();
        for (Config c : configs) {
            if (c.getDescriptor().getClass().equals(descriptor)) {
                cs.add(c);
            }
        }
        return cs;
    }

    @Override
    public Config getById(String id) {
        if (id != null) {
            for (Config c : configs) {
                if (id.equals(c.id)) {
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    public void save(Config config) {
        configs.remove(config);
        configs.add(config);
        try {
            getOwner().save();
        } catch (IOException e) {
            throw new RuntimeException("failed to save config to store", e);
        }
    }

    @Override
    public void remove(String id) {
        Config c = getById(id);
        if (c != null) {
            configs.remove(c);
            try {
                getOwner().save();
            } catch (IOException e) {
                throw new RuntimeException("failed to remove config from store", e);
            }
        }
    }

    @Override
    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs() {
        Map<ConfigProvider, Collection<Config>> grouped = new TreeMap<ConfigProvider, Collection<Config>>(CONFIGPROVIDER_COMPARATOR);
        for (Config c : configs) {
            Collection<Config> configs = grouped.get(c.getProvider());
            if (configs == null) {
                configs = new ArrayList<>();
                grouped.put(c.getProvider(), configs);
            }
            configs.add(c);
        }
        for (Map.Entry<ConfigProvider, Collection<Config>> entry :
                grouped.entrySet()) {
            List<Config> value = (List<Config>) entry.getValue();
            Collections.sort(value, ConfigByNameComparator.INSTANCE);
        }
        return grouped;
    }

    private Object readResolve() {
        if (!(configs instanceof TreeSet)) {
            Collection<Config> newConfigs = new TreeSet<>(COMPARATOR);
            newConfigs.addAll(configs);
            configs = newConfigs;
        }
        return this;
    }

    public FolderConfigFileProperty reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        return this;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @Override
        public String getDisplayName() {
            // nothing to be shown
            return "";
        }
    }

}
